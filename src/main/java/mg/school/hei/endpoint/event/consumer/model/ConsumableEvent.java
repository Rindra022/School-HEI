package mg.school.hei.endpoint.event.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mg.school.hei.PojaGenerated;

@PojaGenerated
@AllArgsConstructor
public class ConsumableEvent {
  @Getter private final TypedEvent event;
  private final Runnable acknowledger;
  private final Runnable randomVisibilityTimeoutSetter;

  public void ack() {
    acknowledger.run();
  }

  public void newRandomVisibilityTimeout() {
    randomVisibilityTimeoutSetter.run();
  }
}
