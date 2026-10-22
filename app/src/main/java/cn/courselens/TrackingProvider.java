package cn.courselens;

import java.util.*;

public interface TrackingProvider {
 enum Error { NONE,EMPTY,RATE_LIMIT,NETWORK,UNAVAILABLE,INVALID_RESPONSE,NOT_CONFIGURED }
 final class Event {
  public final String time,description;
  public Event(String time,String description){this.time=time;this.description=description;}
 }
 final class Result {
  public final Error error;
  public final String state;
  public final List<Event> events;
  public final long retryAfterMillis;
  public Result(Error error,String state,List<Event> events,long retryAfterMillis){this.error=error;this.state=state;this.events=Collections.unmodifiableList(new ArrayList<>(events));this.retryAfterMillis=retryAfterMillis;}
  public static Result failure(Error error,long retryAfterMillis){return new Result(error,"",Collections.emptyList(),retryAfterMillis);}
 }
 Result query(String trackingNumber,String carrier);
}
