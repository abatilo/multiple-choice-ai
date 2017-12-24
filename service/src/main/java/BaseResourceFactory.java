import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.dropwizard.setup.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import opennlp.tools.postag.POSModel;

@AllArgsConstructor
public class BaseResourceFactory {
  protected final MultipleChoiceConfiguration config;
  protected final Environment env;

  public static class ResourceFactory extends BaseResourceFactory {
    public ResourceFactory(MultipleChoiceConfiguration config,
        Environment env) throws IOException {
      super(config, env);
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private final InputStream posStream =
      Resources.getResource(config.getPosModel()).openStream();
    private final POSModel posModel = new POSModel(posStream);
    private final ThreadLocalTagger tagger =
      new ThreadLocalTagger(posModel);

    private final Set<String> STOP_WORDS =
      Sets.newHashSet(
          Resources.readLines(Resources.getResource(config.getStopWords()),
            Charset.defaultCharset()));

    private final Set<String> TAGS = config.getFilteredTags();

    private final Map<String, double[]> vec =
      Word2Vec.fromBin(new File(config.getW2vModel()));

    private final QuestionGuesser guesser = new QuestionGuesser(
        tagger, STOP_WORDS, TAGS, vec);

    @Getter(AccessLevel.PUBLIC)
    private final RootResource rootResource = new RootResource(mapper, guesser);

  }
}
