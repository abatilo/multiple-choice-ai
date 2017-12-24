import com.fasterxml.jackson.databind.DeserializationFeature;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import io.dropwizard.java8.Java8Bundle;

import lombok.SneakyThrows;

public class MultipleChoiceApplication
  extends Application<MultipleChoiceConfiguration> {

  @Override
  public void run(MultipleChoiceConfiguration config, Environment env)
    throws Exception {
    BaseResourceFactory.ResourceFactory resources =
      new BaseResourceFactory.ResourceFactory(config, env);
    env.jersey().register(resources.getRootResource());
  }

  @Override
  public void initialize(Bootstrap<MultipleChoiceConfiguration> bootstrap) {
    bootstrap.addBundle(new Java8Bundle());
    bootstrap.getObjectMapper().disable(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @SneakyThrows
  public static void main(String[] args) {
    new MultipleChoiceApplication().run(args);
  }
}
