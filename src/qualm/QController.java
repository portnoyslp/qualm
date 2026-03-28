package qualm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class QController extends AbstractQReceiver {

  MasterController master;
  QAdvancer advancer;
  String title;

  private static final int ignoreEventsTimeMsec = 1000; // 1 second
  private static final int minWaitTimeMsec = 200;
  private static final int minimalSleepMsec = 100;

  public QController( QReceiver out, QStream qstream, MasterController mc) {
    setTarget(out);
    title = qstream.getTitle();
    setMaster( mc );

    advancer = new QAdvancer( qstream, mc.getQData() );
    setupTriggers();
  }

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public Cue getCurrentCue() { return advancer.getCurrentCue(); }
  public Cue getPendingCue() { return advancer.getPendingCue(); }

  public void setMaster( MasterController mc ) { master = mc; }
  public MasterController getMaster() { return master; }

  private void setupTriggers() {
    // interrupt and clear any pending delay threads
    for (TriggerDelayThread th : triggerThreads)
      th.interrupt();
    triggerThreads.clear();

    triggers = new ArrayList<>();
    addCurrentTriggers();
    buildTriggerCache();
  }

  public Collection<QEvent> changesForCue( String cuenum ) {
    Collection<QEvent> events = advancer.switchToMeasure(cuenum);
    setupTriggers();
    return events;
  }

  volatile long waitForTime = -1;
  private boolean ignoreEvents() {
    if (waitForTime == -1) 
      return false;
    if (Clock.asMillis() < waitForTime) 
      return true;
    
    waitForTime=-1;
    return false;
  }
  
  private void setTimeOut() {
    waitForTime = Clock.asMillis() + ignoreEventsTimeMsec;
  }

  public void advancePatch() {
    master.sendEvents( advancer.advancePatch() );
    setupTriggers();
  }
  public void reversePatch() {
    master.sendEvents( advancer.reversePatch() );
    setupTriggers();
  }

  public void handleMidiCommand(MidiCommand cmd) {
    // Do any of the currently in-effect maps match this event?
    Cue cue = getCurrentCue();
    if (cue != null && cue.getEventMaps() != null) {
      for (EventMapper em : cue.getEventMaps() ) {
	for ( MidiCommand mc : em.mapEvent(cmd) ) {
	  if (mc != null && getTarget() != null)
	    getTarget().handleMidiCommand(mc);
        }
      }
    }

    if (ignoreEvents()) {
      return;
    }

    for (Trigger trig : cachedTriggers) {
      if (trig.match(cmd)) {
        executeTrigger(trig);
	break; // once we execute a trigger, we can stop.
      }
    }
  }
  
  private void executeTriggerWithoutDelay(Trigger trig) {
    // call the appropriate action
    if (trig.getReverse()) 
      reversePatch();
    else
      advancePatch();
  }

  private void executeTrigger(Trigger trig) {
    setTimeOut();

    // anything that's very short isn't worth creating a thread for.
    if (trig.getDelay() < minWaitTimeMsec) {
      executeTriggerWithoutDelay(trig);
      // reset timeout *after* trigger completes, in case it blocked
      // for a significant amount of time
      setTimeOut();
    }
    else {
      // check to see if this trigger is already represented in the
      // list of pending trigger threads.
      boolean found = false;
      for (TriggerDelayThread th : triggerThreads) {
        if (th.hasTrigger(trig)) {
          found = true;
          break;
        }
      }
      if (!found) {
        // spawn a thread which will execute this trigger after the appropriate number of ms.
	TriggerDelayThread tdt = new TriggerDelayThread(trig,this);
	triggerThreads.add(tdt);
	tdt.start();
      }
    }
    
  }

  Trigger cachedTriggers[] = {};
  List<Trigger> triggers;
  final List<TriggerDelayThread> triggerThreads = new CopyOnWriteArrayList<>();

  private void buildTriggerCache() {
    List<Trigger> l = new ArrayList<>();
    l.addAll(triggers);
    cachedTriggers = (Trigger[]) l.toArray(new Trigger[]{});
  }

  private void addTrigger( Trigger t ) {
    triggers.add(t);
  }

  private void addCurrentTriggers() {
    Cue cue = getCurrentCue();
    if (cue != null && cue.getTriggers() != null) {
      for ( Trigger t : cue.getTriggers() )
	addTrigger( t );
    }
  }

  class TriggerDelayThread extends Thread {
    public void run() {
      try {
        while (Clock.asMillis() < wakeupTime) {
          sleep(minimalSleepMsec);
        }
        qc.executeTriggerWithoutDelay(trig);
      } catch (InterruptedException ie) {
        // cancelled by setupTriggers(); do not fire
      } finally {
        triggerThreads.remove(this);
      }
    }
    TriggerDelayThread(Trigger trig, QController qc) {
      this.trig = trig;
      this.qc = qc;
      this.wakeupTime = Clock.asMillis() + trig.getDelay();
    }

    public boolean hasTrigger(Trigger t) {
      return trig.equals(t);
    }

    public String toString() {
      return "TDT:" + trig;
    }
      
    private Trigger trig;
    private QController qc;
    private long wakeupTime;
  }

} // QController
