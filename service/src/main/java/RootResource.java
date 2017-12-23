import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import lombok.SneakyThrows;

import java.util.*;

@Path("/")
public class RootResource {

  @GET
  @SneakyThrows
  public String root() {
    return "ok";
  }

}
