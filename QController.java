package qualm;

import javax.sound.midi.*;
import java.util.*;

public class QController implements Receiver {

  Receiver midiOut;
  QAdvancer advancer;
  QData qdata;
  String title;

  public QController( Receiver out, QStream qstream, QData data ) {
    midiOut = out;
    qdata = data;
    title = qstream.getTitle();
    advancer = new QAdvancer( qstream, data );

    setupTriggers();
  }

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public QData getQData() { return qdata; }
  public Cue getCurrentCue() { return advancer.getCurrentCue(); }
  public Cue getPendingCue() { return advancer.getPendingCue(); }

  QualmREPL REPL = null;
  public QualmREPL getREPL() {
    return REPL;
  }

  public void setREPL(QualmREPL newREPL) {
    this.REPL = newREPL;
  }

  private void sendEvents( Collection c ) {
    Iterator i = c.iterator();
    while(i.hasNext()) {
      ProgramChangeEvent pce = (ProgramChangeEvent)i.next();
      sendPatchChange(pce);
    }

    // update print loop
    if (REPL != null) {
      REPL.updateCue( c );
    }
  }

  private void sendPatchChange(ProgramChangeEvent pce) {
    PatchChanger.patchChange(pce, midiOut);
  }
  
  private void setupTriggers() {
    // set up triggers
    triggers = new ArrayList();
    addCurrentTriggers();
    buildTriggerCache();
  }

  public void switchToCue( String cuenum ) {
    sendEvents( advancer.switchToMeasure( cuenum ) );
    setupTriggers();
  }

  // Implementation of javax.sound.midi.Receiver

  /**
   * Describe <code>close</code> method here.
   */
  public void close() { }

  /**
   * Describe <code>send</code> method here.
   *
   * @param midiMessage a <code>MidiMessage</code> value
   * @param l a <code>long</code> value
   */
  long waitForTime = -1;
  private boolean ignoreEvents() {
    if (waitForTime == -1) 
      return false;
    if (System.currentTimeMillis() < waitForTime) 
      return true;
    
    waitForTime=-1;
    return false;
  }
  private void setTimeOut() {
    waitForTime = System.currentTimeMillis() + 1000; 
  }

  public void advancePatch() {
    sendEvents( advancer.advancePatch() );
    setupTriggers();
  }
  public void reversePatch() {
    sendEvents( advancer.reversePatch() );
    setupTriggers();
  }

  public void send(MidiMessage midiMessage, long l) {
    // Do any of the currently in-effect maps match this event?
    Cue cue = advancer.getPendingCue();
    if (cue != null) {
      Iterator iter = cue.getEventMaps().iterator();
      while(iter.hasNext()) {
	EventMapper em = (EventMapper)iter.next();
	MidiMessage out = em.mapEvent(midiMessage);
	if (out != null && midiOut != null)
	  midiOut.send(out, -1);
      }
    }

    if (ignoreEvents()) 
      return;
      
    for (int i=0;i<cachedTriggers.length;i++) {
      boolean triggered = false;

      Trigger trig = cachedTriggers[i];
      if (trig.match(midiMessage)) {
	triggered = true;
	setTimeOut();

	// call the appropriate action
	if (trig.getReverse()) 
	  reversePatch();
	else
	  advancePatch();
	
      }
      if (triggered) break;
    }
    // no match, just ignore the message.
  }


  Trigger cachedTriggers[] = {};
  List triggers;

  private void buildTriggerCache() {
    List l = new ArrayList();
    l.addAll(triggers);
    cachedTriggers = (Trigger[]) l.toArray(new Trigger[]{});
  }

  private void addTrigger( Trigger t ) {
    triggers.add(t);
  }

  private void addCurrentTriggers() {
    Cue cue = advancer.getPendingCue();
    if (cue != null) {
      Iterator iter = cue.getTriggers().iterator();
      while(iter.hasNext()) {
	Trigger t = (Trigger)iter.next();
	addTrigger( t );
      }
    }
  }
 
} // QController
