package qualm;

import java.util.*;

/**
 * Holds all data for the cue handling...
 */

public class QData {
  String[] channels;
  String[] patches;
  SortedSet cues;
  String title;

  public QData( ) {  
    title = null;
    channels = new String[16];
    patches = new String[128];
    cues = new TreeSet();
  } 

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public void addMidiChannel( int num, String desc ) {
    channels[num] = desc;
  }

  public void addPatch( int num, String desc ) {
    patches[num] = desc;
  }

  public void addCue( QData.Cue cue ) {
    cues.add(cue);
  }

  public void dump() {
    System.out.println("Data dump for " + getTitle());
    System.out.println("  ch:" + Arrays.asList(channels));
    System.out.println("  pl:" + Arrays.asList(patches));
    System.out.println("  qs:" + cues);
  }
  		      
  public class Cue {
    public Cue( String song, String measure ) { }
		
  }

} 
