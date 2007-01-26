package qualm;

import java.util.Collection;

public class Cue implements Comparable {

  String song;
  String measure;
  Collection triggers;
  Collection events;
  Collection eventMaps;

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

  public Collection getTriggers() { return triggers; }
  public void setTriggers(Collection t) { triggers=t; }

  public Collection getEventMaps() { return eventMaps; }
  public void setEventMaps(Collection t) { eventMaps=t; }

  public Collection getEvents() { return events; }
  public void setEvents(Collection t) { events=t; }

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
    if (s1.equals(s2))
      return _compareStrings(m1,m2);
    return _compareStrings(s1,s2);
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

  public static void main(String args[]) {
    System.out.println("1A == 1a: " + _compareStrings("1A", "1a"));
    System.out.println("1 < 1a: " + _compareStrings("1", "1a"));
    System.out.println("1A < 1b: " + _compareStrings("1A", "1b"));
    System.out.println("1f < 2: " + _compareStrings("1f", "2"));
    System.out.println("A < 1: " + _compareStrings("A", "1"));
    System.out.println("9 < 10: " + _compareStrings("9", "10"));
  }

}
