import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import opennlp.tools.tokenize.SimpleTokenizer;

@Slf4j
@RequiredArgsConstructor
public class QuestionGuesser {
  private final ThreadLocalTagger TAGGER;
  private final Set<String> STOP_WORDS;
  private final Set<String> FILTERED_POS_TAGS;
  private final Map<String, double[]> VECTOR_SPACE;

  /**
   * Replaces common contractions with their full versions
   *
   * This approach is currently using .replace() which compiles a pattern for
   * every call which could be a performance hit.
   *
   * Based on:
   * https://stackoverflow.com/questions/14062030/removing-contractions
   *
   * @param inputString The full string that might have contractions in it
   * @return Returns a new String with all of its contractions expanded
   */
  private static String expandContractions(String inputString) {
    return Strings.nullToEmpty(inputString)
      .replace('’', '\'')
      .replace("'s", "")
      .replace("can't", "cannot")
      .replace("won't", "will not")
      .replace("n't", " not")
      .replace("'re", " are")
      .replace("'m", " am")
      .replace("'ll", " will")
      .replace("'ve", " have");
  }

  private boolean allowedWord(String word, String tag) {
    return word.length() > 2
      && !FILTERED_POS_TAGS.contains(tag)
      && !STOP_WORDS.contains(word)
      && VECTOR_SPACE.containsKey(word);
  }

  /**
   * Takes a raw string, and will normalize the text.
   *
   * Normalization in this context means that we will
   * {@link QuestionGuesser#expandContractions} then we will remove any punctuation,
   * then we will remove any stop words, and lastly, we'll also remove any
   * words with a part of speech tag that was not specified to be acceptable.
   *
   * @param corpus The body of text to normalize
   * @return Returns the tokens as a List
   */
  private List<String> normalizedTokensOf(String corpus) {
    String expandedCorpus = expandContractions(corpus);
    String removedPunctuation = expandedCorpus.replaceAll("[^a-zA-Z. ]", "");
    List<String> normalized = new ArrayList<>();
    String[] allWords = SimpleTokenizer.INSTANCE.tokenize(removedPunctuation);
    String[] tags = TAGGER.get().tag(allWords);

    for (int i = 0; i < tags.length; ++i) {
      String word = allWords[i].toLowerCase();
      String tag = tags[i];
      if (allowedWord(word, tag)) {
        normalized.add(word);
      }
    }
    return normalized;
  }

  /**
   * Given a question, uses word2vec to guess an answer out of the list of
   * answers.
   *
   * This is the core feature of this entire service. The guess works by taking
   * a sentence and filtering out words of little value. After that, we sum the
   * word2vec vectors for the words in both the questions and in the answers.
   * Then we take the cosine similarity of the question vector, and the answer
   * vectors and we return the string of the most similar answer. The intuition
   * here is that since answers to questions should appear around the words
   * that would be used to ask about it, the resulting summation vector of the
   * question should have a high cosine similarity to the summation of the
   * answer vector.
   *
   * @param question The question text
   * @param answers Each of the possible answers
   * @return Returns the text of the answer we've deemed to be most likely
   */
  public Optional<String> guess(String question, List<String> answers) {
    List<String> tokensOfQuestion = normalizedTokensOf(question);
    StringBuilder sb = new StringBuilder();
    for (String s : tokensOfQuestion) {
      sb.append(s);
      sb.append(" ");
    }
    log.warn(sb.toString());
    // Defaults to -2 since cosine similarity ranges from -1 to 1
    final double NO_ANSWER = -2.0;

    double topCosineSimilarity = NO_ANSWER;
    String topAnswer = "";
    for (String answer : answers) {
      List<String> tokensOfAnswer = normalizedTokensOf(answer);
      Optional<Double> cosSim =
        cosineSimilarityOf(tokensOfQuestion, tokensOfAnswer);

      StringBuilder dbg = new StringBuilder();
      if (!cosSim.isPresent()) {
        dbg.append("No cosSim -- ");
        dbg.append(answer);
      } else {
        dbg.append(cosSim.get());
        dbg.append(" -- ");
        for (String s : tokensOfAnswer) {
          dbg.append(s);
          dbg.append(" ");
        }
      }
      log.warn(dbg.toString());
      log.warn("");

      if (cosSim.isPresent() && cosSim.get() > topCosineSimilarity) {
        topCosineSimilarity = cosSim.get();
        topAnswer = answer;
      }
    }

    if (topCosineSimilarity == NO_ANSWER) {
      return Optional.empty();
    }
    return Optional.of(topAnswer);
  }

  private Optional<Double> cosineSimilarityOf(List<String> question,
      List<String> answer) {
    if (question.isEmpty() || answer.isEmpty()) {
      return Optional.empty();
    }

    Optional<double[]> optVec1 = vectorOf(question);
    Optional<double[]> optVec2 = vectorOf(answer);

    if (!optVec1.isPresent() || !optVec2.isPresent()) {
      if (!optVec1.isPresent()) {
        log.warn("There was no vector for: " + question);
      }
      if (!optVec2.isPresent()) {
        log.warn("There was no vector for: " + answer);
      }
      return Optional.empty();
    }

    return cosineSimilarity(optVec1.get(), optVec2.get());
  }

  private Optional<Double> cosineSimilarity(double[] v1, double[] v2) {
    double dotProduct = 0.0f;
    double norm1 = 0.0f;
    double norm2 = 0.0f;
    for (int i = 0; i < v1.length; ++i) {
      dotProduct += v1[i] * v2[i];
      norm1 += v1[i] * v1[i];
      norm2 += v2[i] * v2[i];
    }
    return Optional.of(dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
  }

  private Optional<double[]> vectorOf(List<String> words) {
    int vectorSize =
      VECTOR_SPACE.entrySet().iterator().next().getValue().length;
    double[] vector = new double[vectorSize];
    for (String word : words) {
      if (!VECTOR_SPACE.containsKey(word)) {
        return Optional.empty();
      }
      final double[] individualWord = VECTOR_SPACE.get(word);
      for (int i = 0; i < vectorSize; ++i) {
        vector[i] += individualWord[i];
      }
    }
    return Optional.of(vector);
  }
}
