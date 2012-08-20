package qualm;

/**
 * A simple interface to the clock.
 */

public class Clock { 
  public static TimeSource source = null;

  public static final TimeSource SYSTEMSRC = 
    new TimeSource() {
      public long millis() {
	return System.currentTimeMillis();
      }
    };

  public static TimeSource getTimeSource() {
    if (source == null)
      return SYSTEMSRC;
    else return source;
  }

  public static void setTimeSource(TimeSource ts) {
    source = ts;
  }

  public static void reset() { setTimeSource(null); }
  
  public static long asMillis() {
    return getTimeSource().millis();
  }

}
 