package qualm;

import java.util.*;

/**
 * Holds all data for a stream of cues, including triggers.
 */

public class QStream {
  String title;
  SortedSet<Cue> cues;

  public QStream() {
    title = defaultTitle();
    cues = new TreeSet<Cue>();
  } 

  private String defaultTitle() { return "QStream"+hashCode(); }

  public String getTitle() { return title; }
  public void setTitle(String t) { if (t!=null) title=t; }

  public void addCue( Cue cue ) {
    cues.add(cue);
  }
  public SortedSet<Cue> getCues() { return cues; }

  public void dump() {
    System.out.println("Que Stream " + getTitle() + ":");
    System.out.println("  " + cues);
  }

  // equals and hashCode derived from title and cues
  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;

    QStream qs = (QStream)obj;
    return (title == null ? qs.getTitle() == null : title.equals(qs.getTitle()))
      && (cues == null ? qs.getCues() == null : cues.equals(qs.getCues()))
      ;
  }
  @Override
  public int hashCode() {
    final int prime = 61;
    int result = 1;
    result =  prime * result + (title==null ? 0 : title.hashCode());
    result += prime * result + (cues == null ? 0 : cues.hashCode());
    
    return result;
  }

} 
