package qualm;

import java.util.*;

/**
 * Holds all data for the cue handling...
 */

public class QData {
  String[] channels;
  Map<String, Patch> patches;
  Collection<QStream> cueStreams;
  String title;

  public QData( ) {
    title = null;
    channels = new String[16];
    patches = new HashMap<String, Patch>();
    cueStreams = new ArrayList<QStream>();
  } 

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public void addMidiChannel( int num, String deviceType, String desc ) {
    channels[num] = desc;
    PatchChanger.addPatchChanger( num, deviceType );
  }
  public String[] getMidiChannels() { return channels; }
  public Collection<Patch> getPatches() { return patches.values(); }

  public void addPatch( Patch p ) {
    patches.put( p.getID(), p );
  }
  public Patch lookupPatch( String id ) { 
    return patches.get(id); 
  }

  public void addCueStream(QStream qs) {
    cueStreams.add(qs);
  }
  public Collection<QStream> getCueStreams() { return cueStreams; }

  public void dump() {
    System.out.println("Data dump for " + getTitle());
    // Create lists of patches, channels.
    List<String> out = new ArrayList<String>();
    for (int i=0; i<channels.length;i++) 
      if (channels[i]!=null) out.add("(" + i + ")" + channels[i]);
    System.out.println("  ch:" + out);
    System.out.println("  pl:" + patches.values());

    Iterator<QStream> iter = cueStreams.iterator();
    while(iter.hasNext()) {
      iter.next().dump();
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
    Comparator cueCompare = new Comparator<Cue>() {
	public int compare(Cue a, Cue b) {
	  int out = a.compareTo(b);
	  if (out == 0)
	    return a.hashCode()-b.hashCode();
	  else return out;
	}
      };
    TreeSet<Cue> masterCues = new TreeSet<Cue>(cueCompare);
    Iterator<QStream> iter = cueStreams.iterator();
    while(iter.hasNext()) {
      masterCues.addAll( iter.next().getCues() );
    }
    
    // next, we go through the cues' patch changes, and populate them
    // with back-patch info
    Patch[] patches = new Patch[16];
    NoteWindowChangeEvent[] noteWindowChangeEvents =
      new NoteWindowChangeEvent[16];

    Iterator<Cue> cueIter = masterCues.iterator();
    while (cueIter.hasNext()) {
      Cue q = cueIter.next();

      Collection<QEvent> events = q.getEvents();
      Iterator<QEvent> j = events.iterator();
      while(j.hasNext()) {
	QEvent obj = j.next();
	if (obj instanceof ProgramChangeEvent) {
	  ProgramChangeEvent pce = (ProgramChangeEvent)obj;
	  int ch = pce.getChannel();
	  if (patches[ch] != null) {
	    pce.setPreviousPatch(patches[ch]);
	  }

	  patches[ch] = pce.getPatch();
	}
	else if (obj instanceof NoteWindowChangeEvent) {
	  NoteWindowChangeEvent nwce = (NoteWindowChangeEvent)obj;
	  int ch = nwce.getChannel();
	  nwce.setPrevious(noteWindowChangeEvents[ch]);
	  noteWindowChangeEvents[ch] = nwce;
	}
      }
    }
    
  }

} 
