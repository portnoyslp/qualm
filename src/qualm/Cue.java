package qualm;

import java.util.ArrayList;
import java.util.Collection;

public class Cue implements Comparable<Cue> {

  String song;
  String measure;
  Collection<Trigger> triggers;
  Collection<QEvent> events;
  Collection<EventMapper> eventMaps;

  public Cue( String s, String m ) { song=s; measure=m; }
  public Cue( String ts ) { 
    if (ts.indexOf(".") == -1) 
      ts += ".1";
    String s1 = ts.substring(0,ts.indexOf("."));
    String m1 = ts.substring(ts.lastIndexOf(".")+1);
    song=s1;
    measure=m1;
  }

  public String getSong() { return song; }
  public String getMeasure() { return measure; }

  public Collection<Trigger> getTriggers() { return triggers; }
  public void setTriggers(Collection<Trigger> t) { triggers=t; }

  public Collection<EventMapper> getEventMaps() { return eventMaps; }
  public void setEventMaps(Collection<EventMapper> t) { eventMaps=t; }

  public Collection<QEvent> getEvents() { return events; }
  public void setEvents(Collection<QEvent> t) { events=t; }

  public String getCueNumber() { return song + "." + measure; }
  
  public String toString() {
    return "Q[" + getCueNumber() + getTriggers() + 
      " => " + getEvents() + "]";
  }
  
  public boolean equals( Object x ) {
    return getCueNumber().equals( ((Cue)x).getCueNumber() );
  }

  /**
   * Compares the two strings by performing a numeric comparison if
   * they are numbers.  Letters are treated so that they are handled
   * as follows:
   * 
   * <pre>
   *   1A == 1a
   *    1 < 1a
   *   1a < 1b
   *   1f < 2
   *    a < 1
   * </pre>
   * 
   */
  private static int _compareStrings( String s1, String s2) {
    int s1int = 0;
    int s2int = 0;
    String s1char = null;
    String s2char = null;
    s1 = s1.toLowerCase();
    s2 = s2.toLowerCase();
    
    for(int i=0; i< s1.length(); i++) {
      char c = s1.charAt(i);
      if (c>='0' && c<='9') { s1int = s1int*10 + (int)(c-'0'); }
      else { s1char = s1.substring(i); break; }
    }
    for(int i=0; i< s2.length(); i++) {
      char c = s2.charAt(i);
      if (c>='0' && c<='9') { s2int = s2int*10 + (int)(c-'0'); }
      else { s2char = s2.substring(i); break; }
    }

    if (s1int == s2int) {
      if (s1char == null && s2char == null) return 0;
      if (s1char == null) return -1;
      if (s2char == null) return 1;
      return s1char.compareTo(s2char);
    } else {
      return s1int - s2int;
    }
  }

  public int compareTimeStamps( String s1, String m1, 
				String s2, String m2 ) {
    // compare the song, then measure

    int sCmp = _compareStrings(s1,s2);
    if (sCmp == 0)
      return _compareStrings(m1,m2);
    return sCmp;
  }

  public int compareTo(Cue q) {
    return compareTimeStamps( getSong(), getMeasure(),
			      q.getSong(), q.getMeasure() );
  }

  
  public static class Builder {
    String cueNum = null;
    Collection<Trigger> triggers = new ArrayList<Trigger>();
    Collection<QEvent> events = new ArrayList<QEvent>();
    Collection<EventMapper> eventMaps = new ArrayList<EventMapper>();

    public Builder setCueNumber(String cNum) {
      this.cueNum = cNum;
      return this;
    }

    public Builder addTriggers(Collection<Trigger> triggers) {
      this.triggers.addAll(triggers);
      return this;
    }

    public Builder addTrigger(Trigger trigger) {
      this.triggers.add(trigger);
      return this;
    }

    public Builder addEvents(Collection<QEvent> events) {
      this.events.addAll(events);
      return this;
    }

    public Builder addEvent(QEvent event) {
      this.events.add(event);
      return this;
    }

    public Builder addProgramChangeEvent( int ch, Patch p ) {
      this.events.add( new ProgramChangeEvent( ch, null, p ));
      return this;
    }                     

    public Builder addEventMaps(Collection<EventMapper> eventMaps) {
      this.eventMaps.addAll(eventMaps);
      return this;
    }

    public Builder addEventMap(EventMapper eventMap) {
      this.eventMaps.add(eventMap);
      return this;
    }

    public Cue build() {
      if (cueNum == null) {
        throw new IllegalStateException("No cue number was provided");
      }
      Cue c = new Cue( cueNum );
      c.setTriggers(triggers);
      for (QEvent ev : events) {
        if ( ev.getCue() == null ) 
          ev.setCue( c );
      }
      c.setEvents(events);
      c.setEventMaps(eventMaps);
      return c;
    }
  }
}
