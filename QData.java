package qualm;

import java.util.*;

/**
 * Holds all data for the cue handling...
 */

public class QData {
  public String[] channels;
  public String[] patches;
  public SortedSet cues;
  public QData( ) {  
    channels = new String[16];
    patches = new String[128];
    cues = new TreeSet();
  } 

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
    System.out.println("Data dump");
    System.out.println("  ch:" + Arrays.asList(channels));
    System.out.println("  pl:" + Arrays.asList(patches));
    System.out.println("  qs:" + cues);
  }
  		      
  public class Cue {
    public Cue( String song, String measure ) { }
		
  }

} 
