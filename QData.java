package qualm;

import java.util.*;

/**
 * Holds all data for the cue handling...
 */

public class QData {
  String[] channels;
  Map patches;
  Collection reverseTriggers;
  Collection setupEvents;
  Collection mapEvents;
  SortedSet cues;
  String title;

  public QData( ) {
    title = null;
    channels = new String[16];
    patches = new HashMap();
    cues = new TreeSet();
  } 

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public Collection getSetupEvents() { return setupEvents; }
  public void setSetupEvents(Collection s) { setupEvents=s; }

  public Collection getReverseTriggers() { return reverseTriggers; }
  public void setReverseTriggers(Collection s) { reverseTriggers=s; }

  public void addMidiChannel( int num, String desc ) {
    channels[num] = desc;
  }
  public String[] getMidiChannels() { return channels; }
  public Collection getPatches() { return patches.values(); }

  public void addPatch( Patch p ) {
    patches.put( p.getID(), p );
  }
  public Patch lookupPatch( String id ) { 
    return (Patch)patches.get(id); 
  }

  public void addCue( Cue cue ) {
    cues.add(cue);
  }
  public SortedSet getCues() { return cues; }

  public void dump() {
    System.out.println("Data dump for " + getTitle());
    // Create lists of patches, channels.
    List out = new ArrayList();
    for (int i=0; i<channels.length;i++) 
      if (channels[i]!=null) out.add("(" + i + ")" + channels[i]);
    System.out.println("  ch:" + out);
    System.out.println("  pl:" + patches.values());

    System.out.println("  se:" + setupEvents);
    System.out.println("  rt:" + reverseTriggers);
   
    System.out.println("  qs:" + cues);
  }
  
} 
