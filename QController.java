package qualm;

import javax.sound.midi.*;
import java.util.*;

public class QController implements Receiver {

  Receiver midiOut;
  MasterController master;
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

  public Receiver getMidiOut() { return midiOut; }

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public QData getQData() { return qdata; }
  public Cue getCurrentCue() { return advancer.getCurrentCue(); }
  public Cue getPendingCue() { return advancer.getPendingCue(); }

  public void setMaster( MasterController mc ) { master = mc; }
  public MasterController getMaster() { return master; }


  private void setupTriggers() {
    // set up triggers
    triggers = new ArrayList();
    addCurrentTriggers();
    buildTriggerCache();
  }

  public Collection changesForCue( String cuenum ) {
    Collection events = advancer.switchToMeasure(cuenum);
    setupTriggers();
    return events;
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
    master.sendEvents( advancer.advancePatch() );
    setupTriggers();
  }
  public void reversePatch() {
    master.sendEvents( advancer.reversePatch() );
    setupTriggers();
  }

  public void send(MidiMessage midiMessage, long l) {
    // Do any of the currently in-effect maps match this event?
    Cue cue = getCurrentCue();
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
    Cue cue = getCurrentCue();
    if (cue != null) {
      Iterator iter = cue.getTriggers().iterator();
      while(iter.hasNext()) {
	Trigger t = (Trigger)iter.next();
	addTrigger( t );
      }
    }
  }
 
} // QController
