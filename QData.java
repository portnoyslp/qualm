package qualm;

import java.util.*;

/**
 * Holds all data for the cue handling...
 */

public class QData {
  String[] channels;
  Map patches;
  Collection cueStreams;
  String title;

  public QData( ) {
    title = null;
    channels = new String[16];
    patches = new HashMap();
    cueStreams = new ArrayList();
  } 

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public void addMidiChannel( int num, String desc ) {
    channels[num] = desc;
    PatchChanger.addPatchChanger( num, null );
  }
  public String[] getMidiChannels() { return channels; }
  public Collection getPatches() { return patches.values(); }

  public void addPatch( Patch p ) {
    patches.put( p.getID(), p );
  }
  public Patch lookupPatch( String id ) { 
    return (Patch)patches.get(id); 
  }

  public void addCueStream(QStream qs) {
    cueStreams.add(qs);
  }
  public Collection getCueStreams() { return cueStreams; }

  public void dump() {
    System.out.println("Data dump for " + getTitle());
    // Create lists of patches, channels.
    List out = new ArrayList();
    for (int i=0; i<channels.length;i++) 
      if (channels[i]!=null) out.add("(" + i + ")" + channels[i]);
    System.out.println("  ch:" + out);
    System.out.println("  pl:" + patches.values());

    Iterator iter = cueStreams.iterator();
    while(iter.hasNext()) {
      ((QStream)iter.next()).dump();
    }
  }
  

  public void prepareCueStreams() {
    // go through the different cue streams, and populate the cues'
    // program changes with info on how to reverse the patch changes
    // properly.

    // we do this first by creating a "master plot" of cue changes

    // problem with a master plot is that we can't use the normal
    // comparator for Cues, we have to use our own which will allow
    // two cues with the same measure number to exist in the set.
    Comparator cueCompare = new Comparator() {
	public int compare(Object a, Object b) {
	  if (a instanceof Cue &&
	      b instanceof Cue) {
	    int out = ((Cue)a).compareTo(b);
	    if (out == 0)
	      return a.hashCode()-b.hashCode();
	    else return out;
	  }
	  return ((Comparable)a).compareTo(b);
	}
      };
    TreeSet masterCues = new TreeSet(cueCompare);
    Iterator iter = cueStreams.iterator();
    while(iter.hasNext()) {
      masterCues.addAll(  ((QStream)iter.next()).getCues() );
    }
    
    // next, we go through the cues' patch changes, and populate them
    // with back-patch info
    Patch[] patches = new Patch[16];
    
    iter = masterCues.iterator();
    while (iter.hasNext()) {
      Cue q = (Cue)(iter.next());

      Collection events = q.getEvents();
      Iterator j = events.iterator();
      while(j.hasNext()) {
	ProgramChangeEvent pce = (ProgramChangeEvent)j.next();
	int ch = pce.getChannel();
	if (patches[ch] != null) {
	  pce.setPreviousPatch(patches[ch]);
	}

	patches[ch] = pce.getPatch();
      }
    }
    
  }

} 
