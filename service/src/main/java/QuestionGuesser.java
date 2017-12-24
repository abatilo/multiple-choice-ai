import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

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

  private static final Pattern WEIRD_QUOTE = Pattern.compile("’");
  private static final Pattern APOSTROPHE_S = Pattern.compile("'s");
  private static final Pattern CANT = Pattern.compile("can't");
  private static final Pattern WONT = Pattern.compile("won't");
  private static final Pattern NOT = Pattern.compile("n't");
  private static final Pattern ARE = Pattern.compile("'re");
  private static final Pattern AM = Pattern.compile("'m");
  private static final Pattern WILL = Pattern.compile("'ll");
  private static final Pattern HAVE = Pattern.compile("'ve");

  /**
   * Replaces common contractions with their full versions.
   *
   * Based on:
   * https://stackoverflow.com/questions/14062030/removing-contractions
   *
   * @param inputString The full string that might have contractions in it
   * @return Returns a new String with all of its contractions expanded
   */
  private static String expandContractions(String inputString) {
    return
      WEIRD_QUOTE.matcher(
          HAVE.matcher(
            WILL.matcher(
              AM.matcher(
                ARE.matcher(
                  NOT.matcher(
                    WONT.matcher(
                      CANT.matcher(
                        APOSTROPHE_S.matcher(
                          Strings.nullToEmpty(inputString)
                          ).replaceAll("'")
                        ).replaceAll("")
                      ).replaceAll("can not")
                    ).replaceAll("will not")
                  ).replaceAll("not")
                ).replaceAll("are")
              ).replaceAll("am")
            ).replaceAll("will")
          ).replaceAll("have");
  }

  /**
   * Cheap and easy filter for removing low impact words.
   *
   * @param word The word to evaluate
   * @param tag The part of speech tag for the word
   * @return Returns whether or not we should keep this word
   */
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
    // Defaults to -2 since cosine similarity ranges from -1 to 1
    final double NO_ANSWER = -2.0;

    double topCosineSimilarity = NO_ANSWER;
    String topAnswer = "";
    for (String answer : answers) {
      List<String> tokensOfAnswer = normalizedTokensOf(answer);
      Optional<Double> cosSim =
        cosineSimilarityOf(tokensOfQuestion, tokensOfAnswer);
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

  /**
   * Sums the vectors of all the words in the passed in list to create a single
   * thought vector.
   *
   * @param words The words that make up the thought
   * @return Returns an optional holding the vector for a given thought
   */
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
