package qualm;

import java.util.Collection;

public class Cue implements Comparable {

  String song;
  String measure;
  Trigger trigger;
  Collection events;

  public Cue( String s, String m ) { song=s; measure=m; }
  public Cue( String ts ) { 
    String s1 = ts.substring(0,ts.indexOf("."));
    String m1 = ts.substring(ts.lastIndexOf(".")+1);
    song=s1;
    measure=m1;
  }

  public String getSong() { return song; }
  public String getMeasure() { return measure; }

  public Trigger getTrigger() { return trigger; }
  public void setTrigger(Trigger t) { trigger=t; }

  public Collection getEvents() { return events; }
  public void setEvents(Collection t) { events=t; }

  public String getCueNumber() { return song + "." + measure; }
  public String toString() { return "Q[" + getCueNumber() + "]"; }

  public int compareTimeStamps( String s1, String m1, 
				String s2, String m2 ) {
    // compare the song, then measure
    if (s1.equals(s2))
      return m1.compareTo(m2);
    return s1.compareTo(s2);
  }

  public int compareTimeStamps( String ts1, String ts2 ) {
    // parse the timestamps
    String s1 = ts1.substring(0,ts1.indexOf("."));
    String s2 = ts2.substring(0,ts2.indexOf("."));
    String m1 = ts1.substring(ts1.lastIndexOf(".")+1);
    String m2 = ts2.substring(ts2.lastIndexOf(".")+1);
    return compareTimeStamps(s1,m1,s2,m2);
  }

  public int compareTo(Object c) {
    Cue q = (Cue) c;
    return compareTimeStamps( getSong(), getMeasure(),
			      q.getSong(), q.getMeasure() );
  }

}
