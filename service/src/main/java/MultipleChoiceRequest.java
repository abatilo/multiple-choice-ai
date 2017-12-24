import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MultipleChoiceRequest {
  @JsonProperty("#Q") private String q;
  @JsonProperty("A") private String a;
  @JsonProperty("B") private String b;
  @JsonProperty("C") private String c;
  @JsonProperty("D") private String d;
}
