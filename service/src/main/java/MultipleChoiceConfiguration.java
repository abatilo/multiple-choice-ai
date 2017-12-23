import io.dropwizard.Configuration;
import javax.validation.Valid;
import lombok.Getter;

@Getter
public class MultipleChoiceConfiguration extends Configuration {
  @Valid private String version;
}
