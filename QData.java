package qualm;

import java.util.*;

/**
 * Holds all data for the cue handling...
 */

public class QData {
  String[] channels;
  String[] patches;
  Collection reverseTriggers;
  Collection setupEvents;
  Collection mapEvents;
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

  public Collection getSetupEvents() { return setupEvents; }
  public void setSetupEvents(Collection s) { setupEvents=s; }

  public Collection getReverseTriggers() { return reverseTriggers; }
  public void setReverseTriggers(Collection s) { reverseTriggers=s; }

  public void addMidiChannel( int num, String desc ) {
    channels[num] = desc;
  }
  public String[] getMidiChannels() { return channels; }
  public String[] getPatches() { return patches; }

  public void addPatch( int num, String desc ) {
    patches[num] = desc;
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
    out = new ArrayList();
    for (int i=0; i<patches.length;i++) 
      if (patches[i]!=null) out.add("(" + i + ")" + patches[i]);
    System.out.println("  pl:" + out);

    System.out.println("  se:" + setupEvents);
    System.out.println("  rt:" + reverseTriggers);
   
    System.out.println("  qs:" + cues);
  }
  
} 
