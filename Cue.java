package qualm;

import java.util.Collection;

public class Cue implements Comparable {

  String song;
  String measure;
  Trigger trigger;
  Collection events;

  public Cue( String s, String m ) { song=s; measure=m; }

  public String getSong() { return song; }
  public String getMeasure() { return measure; }

  public Trigger getTrigger() { return trigger; }
  public void setTrigger(Trigger t) { trigger=t; }

  public Collection getEvents() { return events; }
  public void setEvents(Collection t) { events=t; }

  public String getCueNumber() { return song + "." + measure; }
  public String toString() { return "Q[" + getCueNumber() + "]"; }

  public int compareTo(Object c) {
    Cue q = (Cue) c;
    // compare the song, then measure
    if (q.getSong() == getSong()) 
      return getMeasure().compareTo(q.getMeasure());
    return getSong().compareTo(q.getSong());
  }

}
