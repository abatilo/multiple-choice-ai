import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import lombok.RequiredArgsConstructor;

@Path("/")
@RequiredArgsConstructor
public class RootResource {

  // https://httpstatusdogs.com/422-unprocessable-entity
  private static final int UNPROCESSABLE_ENTRY = 422;

  private final ObjectMapper mapper;
  private final QuestionGuesser guesser;

  @POST
  public Response root(MultipleChoiceRequest req) {
    if (req == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    if (req.getQ() != null) {
      Optional<String> guess = guesser.guess(req.getQ(), answersOf(req));
      if (guess.isPresent()) {
        return Response.ok(guess.get()).build();
      }
    }
    return Response.status(UNPROCESSABLE_ENTRY).build();
  }

  private static List<String> answersOf(MultipleChoiceRequest req) {
    ImmutableList.Builder<String> answers = ImmutableList.builder();
    if (req.getA() != null) {
      answers.add(req.getA());
    }
    if (req.getB() != null) {
      answers.add(req.getB());
    }
    if (req.getC() != null) {
      answers.add(req.getC());
    }
    if (req.getD() != null) {
      answers.add(req.getD());
    }
    return answers.build();
  }
}
