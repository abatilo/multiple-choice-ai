import lombok.RequiredArgsConstructor;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

@RequiredArgsConstructor
public class ThreadLocalTagger extends ThreadLocal<POSTaggerME> {
  private final POSModel model;
  @Override public POSTaggerME get() {
    return new POSTaggerME(model);
  }
}
