import javax.ws.rs.GET;
import javax.ws.rs.Path;

import lombok.SneakyThrows;

@Path("/")
public class RootResource {

  @GET
  @SneakyThrows
  public String root() {
    return "ok";
  }

}
