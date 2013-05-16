package qualm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import qualm.notification.QualmNotification;

/**
 * The main controller for Qualm.  It manages and dispatches incoming
 * events to the individual QControllers for the streams, and serves
 * as the controller for the application.
 */

public class MasterController implements QReceiver {
  private static PreferencesManager preferencesManager = new PreferencesManager();

  QReceiver midiOut;
  SortedMap<String, QController> controllers;
  QualmREPL REPL = null;
  boolean debugMIDI = false;
  boolean silentErrorHandling = true;
  private NotificationManager pluginManager;
  private QData qdata;
  
  public MasterController( QReceiver out ) {
    midiOut = new VerboseReceiver(out);
    controllers = new TreeMap<String, QController>();
    setPluginManager(new NotificationManager());
    preferencesManager.setController(this);
  }

  public QReceiver getMidiOut() { return midiOut; }

  public void setREPL(QualmREPL newREPL) { REPL = newREPL; }
  public QualmREPL getREPL() { return REPL; }

  public void setPluginManager(NotificationManager pm) {
    pluginManager = pm;
  }
  public NotificationManager getPluginManager() { return pluginManager; }
  
  public PreferencesManager getPreferencesManager() { return preferencesManager; }
  
  public void addController( QController qc ) {
    controllers.put( qc.getTitle(), qc );
    qc.setMaster( this );
  }

  public void removeControllers() {
    controllers.clear();
  }

  public QData getQData() { return qdata; }
  public void setQData(QData qdata) { this.qdata = qdata; }

  public QController mainQC() { 
    return (QController) controllers.get(controllers.firstKey()); 
  }

  public void advanceMainPatch() {
    mainQC().advancePatch();
  }
  
  public void gotoCue(String cueName) {
    // send all controllers to the cue number named in the line
    Collection<QEvent> sentPCs = new ArrayList<QEvent>();

    Collection<QEvent> changes = changesForCue( cueName );

    // we now have a set of cued ProgramChangeEvents.  We need to
    // determine which is the best (most recent) program change for
    // each channel, and send it.
    boolean[] sent_channel = new boolean[16];
    for(int i=0;i<16;i++) sent_channel[i] = false;

    boolean[] sent_nw_on_channel = new boolean[16];
    for(int i=0;i<16;i++) sent_nw_on_channel[i] = false;

    for (QEvent obj : changes) {
      if (obj instanceof ProgramChangeEvent) {
	ProgramChangeEvent pce = (ProgramChangeEvent)obj;
	int channel = pce.getChannel();
	if (!sent_channel[ channel ]) {
	  PatchChanger.patchChange(pce, mainQC().getTarget() );
	  sentPCs.add(pce);
	  sent_channel[channel] = true;
	}
      } else if (obj instanceof NoteWindowChangeEvent) {
	NoteWindowChangeEvent nwce = (NoteWindowChangeEvent)obj;
	int channel = nwce.getChannel();
	if (!sent_nw_on_channel[ channel ]) {
	  PatchChanger.noteWindowChange(nwce, mainQC().getTarget() );
	  sentPCs.add(nwce);
	  sent_nw_on_channel[channel] = true;
	}
      } else if (obj instanceof MidiEvent) {
        // Send the MIDI event.
        sendMidiEvent((MidiEvent)obj);
      }
    }

    updateCue(sentPCs);
  }

  public void advanceStream(String stream_id) {
    QController qc = (QController) controllers.get(stream_id);
    if (qc != null) 
      qc.advancePatch();
  }

  public void reverseStream(String stream_id) {
    QController qc = (QController) controllers.get(stream_id);
    if (qc != null) 
      qc.reversePatch();
  }

  private void sendMidiEvent(MidiEvent me) {
    midiOut.handleMidiCommand(me.getMidiCommand());
  }
  
  private void sendPatchChange(ProgramChangeEvent pce) {
    PatchChanger.patchChange(pce, midiOut);
  }
  
  private void sendNoteWindowChange(NoteWindowChangeEvent nwce) {
    PatchChanger.noteWindowChange(nwce, midiOut);
  }
  
  protected void sendEvents( Collection<QEvent> c ) {
    Iterator<QEvent> i = c.iterator();
    while(i.hasNext()) {
      QEvent obj = i.next();
      if (obj instanceof ProgramChangeEvent)
	sendPatchChange((ProgramChangeEvent)obj);
      else if (obj instanceof NoteWindowChangeEvent) 
	sendNoteWindowChange((NoteWindowChangeEvent)obj);
      else if (obj instanceof StreamAdvance) 
	advanceStream( (StreamAdvance) obj );
      else if (obj instanceof MidiEvent)
        sendMidiEvent( (MidiEvent) obj );
    }

    // update print loop
    updateCue( c );
  }

  public void advanceStream(StreamAdvance sa) {
    QController qc = controllers.get(sa.getStreamID());
    if (qc == null) 
      return;
    
    if (sa.getSong() == null) 
      qc.advancePatch();
    else {
      String cueName = sa.getCueNumber();
      Collection<QEvent> changes = qc.changesForCue( cueName );
      sendEvents(changes);
    }
  }

  private Collection<QEvent> changesForCue( String cueName ) {
    Collection<QEvent> changes = new TreeSet<QEvent>( new Comparator<QEvent>() {
	// compare cues in reverse order
	public int compare( QEvent ca, QEvent cb) {
	  // first, compare cue numbers
	  int val = cb.getCue().compareTo(ca.getCue());
	  if (val != 0)
	    return val;
	  // if same cue number, compare by name of runtime class (to
	  // distinguish between different types of events)
	  val = ca.getClass().getName().compareTo(cb.getClass().getName());
	  if (val != 0)
	    return val;
	  // if same cue number and runtime class, use channel to disambiguate more.	  
	  return ca.getChannel()-cb.getChannel();
	}
      });
   
    for (QController qc : controllers.values()) {
      changes.addAll(qc.changesForCue( cueName ));
    }
    return changes;
  }

  public Collection<QController> getControllers() {
    return controllers.values();
  }

  private void updateCue(Collection<QEvent> c) {
    // update the cue plugins
    getPluginManager().handleCueChanges(this);
    
    for (QEvent obj : c) {
      if (obj instanceof ProgramChangeEvent) {
        ProgramChangeEvent pce = (ProgramChangeEvent)obj;
        int ch = pce.getChannel();
        Patch patch = pce.getPatch();
        // update the PatchChange plugins
        getPluginManager().handlePatchChanges( ch, qdata.getMidiChannels()[ch], patch);
      }
      
      else if (obj instanceof NoteWindowChangeEvent) {
        // TODO: new notification for NWCEs?
        NoteWindowChangeEvent nwce = (NoteWindowChangeEvent)obj;
        //int ch = nwce.getChannel();
        //output.println( qd.getMidiChannels()[ch] + " " + nwce );
      }
    }
  }

  public void setDebugMIDI(boolean flag) { 
    debugMIDI=flag; 
    if (midiOut instanceof VerboseReceiver) 
      ((VerboseReceiver)midiOut).setDebugMIDI(flag);
  }
  public void setSilentErrorHandling(boolean flag) { silentErrorHandling=flag; }

  /* Forwards the given midi command to all the controllers.
   * @see qualm.QReceiver#handleMidiCommand(qualm.MidiCommand)
   */
  public void handleMidiCommand(MidiCommand midi) {
    if (debugMIDI) 
      Qualm.LOG.fine( "Rec'd" + midi );

    try {
      for (QController qc : controllers.values() ) {
        qc.handleMidiCommand(midi);
      }
    } catch (RuntimeException re) {
      // if we get any errors from the data send, we can either print
      // them and continue, or throw a monkey wrench
      re.printStackTrace();
      if (!silentErrorHandling)
        throw re;
    }
  }

  Set<QualmNotification> removePlugin(String name) {
    return pluginManager.removeNotification(name);
  }
  
  public void loadFilename( String filename ) {
    // remove existing controllers
    removeControllers();

    QData qdata = Qualm.loadQDataFromFilename(filename);
    setQData(qdata);

    // For each cue stream, start a controller
    for(QStream qs : qdata.getCueStreams()) {
      QController qc = new QController( getMidiOut(), qs, this );
      addController(qc);
    }
  }

}
