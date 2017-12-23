import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/")
public class RootResource {

  @POST
  public String root(MultipleChoiceRequest req) {
    if (req == null) {
      return "JSON request required";
    }
    if (req.getQ() != null) {
      System.out.println(req.getQ());
    } else {
      System.out.println("No question was given");
    }
    return "ok";
  }

}
