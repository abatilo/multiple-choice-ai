import io.dropwizard.Configuration;

import java.util.Set;

import javax.validation.Valid;

import lombok.Getter;

@Getter
public class MultipleChoiceConfiguration extends Configuration {
  @Valid private String posModel;
  @Valid private String w2vModel;
  @Valid private String stopWords;
  @Valid private Set<String> allowedTags;
}
