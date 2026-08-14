package mg.school.hei.endpoint.event.model;

import static java.lang.Math.random;
import static mg.school.hei.endpoint.event.EventStack.EVENT_STACK_1;

import java.io.Serializable;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import mg.school.hei.PojaGenerated;
import mg.school.hei.endpoint.event.EventStack;

@PojaGenerated
public abstract class PojaEvent implements Serializable {

  @Getter @Setter protected int attemptNb;

  public abstract Duration maxConsumerDuration();

  public Duration eventHandlerInitMaxDuration() {
    return Duration.ofSeconds(90); // note(init-visibility)
  }

  private Duration randomConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds((int) (random() * maxConsumerBackoffBetweenRetries().toSeconds()));
  }

  public abstract Duration maxConsumerBackoffBetweenRetries();

  public final Duration randomVisibilityTimeout() {
    return Duration.ofSeconds(
        eventHandlerInitMaxDuration().toSeconds()
            + maxConsumerDuration().toSeconds()
            + randomConsumerBackoffBetweenRetries().toSeconds());
  }

  public EventStack getEventStack() {
    return EVENT_STACK_1;
  }

  public String getEventSource() {
    return "mg.school.hei.event1";
  }
}
