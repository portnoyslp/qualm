package qualm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import static qualm.Qualm.LOG;

import qualm.notification.QualmNotifier;

/**
 * The main controller for Qualm.  It manages and dispatches incoming
 * events to the individual QControllers for the streams, and serves
 * as the controller for the application.
 */

public class MasterController implements QReceiver {
  private PreferencesManager preferencesManager = new PreferencesManager();

  QReceiver midiOut;
  SortedMap<String, QController> controllers;
  QualmREPL REPL = null;
  boolean silentErrorHandling = true;
  private NotificationManager notificationManager;
  private QData qdata;
  private PatchChanger patchChanger = new PatchChanger();
  
  public MasterController( QReceiver out ) {
    midiOut = new VerboseReceiver(out);
    controllers = new TreeMap<>();
    setNotificationManager(new NotificationManager());
    preferencesManager.setController(this);
  }

  public QReceiver getMidiOut() { return midiOut; }

  public void setREPL(QualmREPL newREPL) { REPL = newREPL; }
  public QualmREPL getREPL() { return REPL; }

  public void setNotificationManager(NotificationManager pm) {
    notificationManager = pm;
  }
  public NotificationManager getNotificationManager() { return notificationManager; }
  
  public PreferencesManager getPreferencesManager() { return preferencesManager; }
  
  public void addController( QController qc ) {
    controllers.put( qc.getTitle(), qc );
    qc.setMaster( this );
  }

  public void removeControllers() {
    controllers.clear();
  }

  public QData getQData() { return qdata; }
  public void setQData(QData qdata) {
    this.qdata = qdata;
    patchChanger = PatchChanger.fromQData(qdata);
  }

  public QController mainQC() { 
    return (QController) controllers.get(controllers.firstKey()); 
  }

  public void advanceMainPatch() {
    mainQC().advancePatch();
  }
  
  public void gotoCue(String cueName) {
    // send all controllers to the cue number named in the line
    Collection<QEvent> sentPCs = new ArrayList<>();

    Collection<QEvent> changes = changesForCue( cueName );

    // we now have a set of cued ProgramChangeEvents.  We need to
    // determine which is the best (most recent) program change for
    // each channel, and send it.
    boolean[] sent_channel = new boolean[16];
    for(int i=0;i<16;i++) sent_channel[i] = false;

    boolean[] sent_nw_on_channel = new boolean[16];
    for(int i=0;i<16;i++) sent_nw_on_channel[i] = false;

    for (QEvent obj : changes) {
      switch (obj) {
        case ProgramChangeEvent pce -> {
          int channel = pce.getChannel();
          if (!sent_channel[ channel ]) {
            patchChanger.patchChange(pce, mainQC().getTarget() );
            sentPCs.add(pce);
            sent_channel[channel] = true;
          }
        }
        case NoteWindowChangeEvent nwce -> {
          int channel = nwce.getChannel();
          if (!sent_nw_on_channel[ channel ]) {
            patchChanger.noteWindowChange(nwce, mainQC().getTarget() );
            sentPCs.add(nwce);
            sent_nw_on_channel[channel] = true;
          }
        }
        case MidiEvent me  -> sendMidiEvent(me);
        case StreamAdvance sa -> {}
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
    patchChanger.patchChange(pce, midiOut);
  }

  private void sendNoteWindowChange(NoteWindowChangeEvent nwce) {
    patchChanger.noteWindowChange(nwce, midiOut);
  }
  
  protected void sendEvents( Collection<QEvent> c ) {
    for (QEvent obj : c) {
      switch (obj) {
        case ProgramChangeEvent pce     -> sendPatchChange(pce);
        case NoteWindowChangeEvent nwce -> sendNoteWindowChange(nwce);
        case StreamAdvance sa           -> advanceStream(sa);
        case MidiEvent me               -> sendMidiEvent(me);
      }
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
	  int ch1 = (ca instanceof ChannelEvent ce) ? ce.getChannel() : -1;
	  int ch2 = (cb instanceof ChannelEvent ce) ? ce.getChannel() : -1;
	  return ch1 - ch2;
	}
      });
   
    for (QController qc : controllers.values()) {
      changes.addAll(qc.changesForCue( cueName ));
    }
    return changes;
  }

  public Collection<QController> getControllers() {
    return Collections.unmodifiableCollection(controllers.values());
  }

  private void updateCue(Collection<QEvent> c) {
    // identify and notify all patch changes
    getNotificationManager().startNotifications();
    
    for (QEvent obj : c) {
      switch (obj) {
        case ProgramChangeEvent pce -> {
          int ch = pce.getChannel();
          Patch patch = pce.getPatch();
          getNotificationManager().handlePatchChanges( ch, qdata.getMidiChannels()[ch], patch);
        }
        case NoteWindowChangeEvent nwce -> {
          // TODO: new notification for NWCEs?
        }
        case MidiEvent me   -> {}
        case StreamAdvance sa -> {}
      }
    }
    
    // tell notifiers about any new mapping info
    getNotificationManager().handleMapActivations(this);
    // finally, tell notifiers about the new cue
    getNotificationManager().handleCueChanges(this);
    getNotificationManager().endNotifications();
  }

  public void setSilentErrorHandling(boolean flag) { silentErrorHandling=flag; }

  /* Forwards the given midi command to all the controllers.
   * @see qualm.QReceiver#handleMidiCommand(qualm.MidiCommand)
   */
  public void handleMidiCommand(MidiCommand midi) {
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

  Set<QualmNotifier> removePlugin(String name) {
    return notificationManager.removeNotification(name);
  }
  
  private static QData loadQData(String filename) throws IOException {
    String lower = filename.toLowerCase();
    boolean looksLikeYaml = lower.endsWith(".yaml") || lower.endsWith(".yml");
    if (looksLikeYaml) {
      try {
        return new YAMLDataLoader().load(filename);
      } catch (Exception e) {
        LOG.warning("YAML load failed, trying XML: " + e.getMessage());
        return new QDataLoader().load(filename);
      }
    } else {
      try {
        return new QDataLoader().load(filename);
      } catch (Exception e) {
        LOG.warning("XML load failed, trying YAML: " + e.getMessage());
        return new YAMLDataLoader().load(filename);
      }
    }
  }

  public void loadFilename( String filename ) throws IOException {
    // remove existing controllers
    removeControllers();

    QData qdata = loadQData(filename);
    setQData(qdata);

    // For each cue stream, start a controller
    for(QStream qs : qdata.getCueStreams()) {
      QController qc = new QController( getMidiOut(), qs, this );
      addController(qc);
    }
  }

}
