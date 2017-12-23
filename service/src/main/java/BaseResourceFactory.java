import io.dropwizard.setup.Environment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class BaseResourceFactory {
  protected final MultipleChoiceConfiguration config;
  protected final Environment env;

  public static class ResourceFactory extends BaseResourceFactory {
    public ResourceFactory(MultipleChoiceConfiguration config, Environment env) {
      super(config, env);
    }
  }

  @Getter(AccessLevel.PUBLIC)
  private final RootResource rootResource = new RootResource();
}
