package qualm;

import java.util.*;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

public class MasterController implements Receiver {

  Receiver midiOut;
  SortedMap controllers;
  QualmREPL REPL = null;
  boolean debugMIDI = false;
  boolean silentErrorHandling = true;

  public MasterController( Receiver out ) {
    midiOut = out;
    controllers = new TreeMap();
  }

  public Receiver getMidiOut() { return midiOut; }

  public void setREPL(QualmREPL newREPL) { REPL = newREPL; }
  public QualmREPL getREPL() { return REPL; }

  public void addController( QController qc ) {
    controllers.put( qc.getTitle(), qc );
    qc.setMaster( this );
  }

  public QController mainQC() { 
    return (QController) controllers.get(controllers.firstKey()); 
  }

  public void gotoCue(String cueName) {
    // send all controllers to the cue number named in the line
    Collection sentPCs = new ArrayList();

    Collection changes = changesForCue( cueName );

    // we now have a set of cued ProgramChangeEvents.  We need to
    // determine which is the best (most recent) program change for
    // each channel, and send it.
    boolean[] sent_channel = new boolean[16];
    for(int i=0;i<16;i++) sent_channel[i] = false;

    boolean[] sent_nw_on_channel = new boolean[16];
    for(int i=0;i<16;i++) sent_nw_on_channel[i] = false;

    Iterator iter = changes.iterator();
    while(iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof ProgramChangeEvent) {
	ProgramChangeEvent pce = (ProgramChangeEvent)obj;
	int channel = pce.getChannel();
	if (!sent_channel[ channel ]) {
	  PatchChanger.patchChange(pce, mainQC().getMidiOut() );
	  sentPCs.add(pce);
	  sent_channel[channel] = true;
	}
      }
      if (obj instanceof NoteWindowChangeEvent) {
	NoteWindowChangeEvent nwce = (NoteWindowChangeEvent)obj;
	int channel = nwce.getChannel();
	if (!sent_nw_on_channel[ channel ]) {
	  PatchChanger.noteWindowChange(nwce, mainQC().getMidiOut() );
	  sentPCs.add(nwce);
	  sent_nw_on_channel[channel] = true;
	}
      }
    }

    updateCue(sentPCs);
  }

  public void advanceStream(String stream_id) {
    QController qc = (QController) controllers.get(stream_id);
    if (qc != null) 
      qc.advancePatch();
  }

  private void sendPatchChange(ProgramChangeEvent pce) {
    PatchChanger.patchChange(pce, midiOut);
  }
  
  private void sendNoteWindowChange(NoteWindowChangeEvent nwce) {
    PatchChanger.noteWindowChange(nwce, midiOut);
  }
  
  protected void sendEvents( Collection c ) {
    Iterator i = c.iterator();
    while(i.hasNext()) {
      Object obj = i.next();
      if (obj instanceof ProgramChangeEvent)
	sendPatchChange((ProgramChangeEvent)obj);
      else if (obj instanceof NoteWindowChangeEvent) 
	sendNoteWindowChange((NoteWindowChangeEvent)obj);
      else if (obj instanceof StreamAdvance) 
	advanceStream( (StreamAdvance) obj );
    }

    // update print loop
    updateCue( c );
  }

  public void advanceStream(StreamAdvance sa) {
    QController qc = (QController) controllers.get(sa.getStreamID());
    if (qc == null) 
      return;
    
    if (sa.getSong() == null) 
      qc.advancePatch();
    else {
      String cueName = sa.getCueNumber();
      Collection changes = qc.changesForCue( cueName );
      sendEvents(changes);
    }
  }

  private Collection changesForCue( String cueName ) {
    Collection changes = new TreeSet( new Comparator() {
	// compare cues in reverse order
	public int compare( Object a, Object b) {
	  CuedEvent ca = (CuedEvent)a;
	  CuedEvent cb = (CuedEvent)b;
	  // first, compare cue numbers
	  int val = cb.getCue().compareTo(ca.getCue());
	  if (val != 0)
	    return val;
	  // if same cue number, compare by name of runtime class (to
	  // distinguish between different types of events)
	  val = ca.getClass().getName().compareTo(cb.getClass().getName());
	  if (val != 0)
	    return val;
	  // if same cue number and runtime class, compare by channel
	  return ca.getChannel()-cb.getChannel();
	}
      });
    
    Iterator iter = controllers.values().iterator();
    while (iter.hasNext()) {
      QController qc = (QController)iter.next();
      changes.addAll(qc.changesForCue( cueName ));
    }
    return changes;
  }

  public Collection getControllers() {
    return controllers.values();
  }

  public void updateCue(Collection c) {
    if (REPL != null)
      REPL.updateCue( c );
  }


  // Implementation of javax.sound.midi.Receiver

  public void setDebugMIDI(boolean flag) { debugMIDI=flag; }
  public void setSilentErrorHandling(boolean flag) { silentErrorHandling=flag; }

  public void close() {
    Iterator i = controllers.values().iterator();
    while (i.hasNext()) 
      ((Receiver)i.next()). close();
  }

  public void send(MidiMessage midiMessage, long ts) {
    if (debugMIDI) 
      System.out.println( MidiMessageParser.messageToString(midiMessage) );

    try {
      Iterator i = controllers.values().iterator();
      while (i.hasNext()) 
	((Receiver)i.next()). send(midiMessage, ts);
    } catch (RuntimeException re) {
      // if we get any errors from the data send, we can either print
      // them and continue, or throw a monkey wrench
      re.printStackTrace();
      if (!silentErrorHandling)
	throw re;
    }
  }


} // QController
