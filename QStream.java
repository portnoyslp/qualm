package qualm;

import java.util.*;

/**
 * Holds all data for a stream of cues, including triggers.
 */

public class QStream {
  String title;
  SortedSet cues;

  public QStream() {
    title = null;
    cues = new TreeSet();
  } 

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public void addCue( Cue cue ) {
    cues.add(cue);
  }
  public SortedSet getCues() { return cues; }

  public void dump() {
    System.out.println("Que Stream " + getTitle() + ":");
    System.out.println("  " + cues);
  }
  
} 
