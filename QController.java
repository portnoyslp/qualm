package qualm;

import javax.sound.midi.*;
import java.util.*;

/**
 * 
 *
 */
public class QController implements Receiver {

  Receiver midiOut;
  QAdvancer advancer;

  public QController( Receiver out, QData data ) {
    midiOut = out;
    triggers = new HashMap();
    advancer = new QAdvancer( data );
  }

  private void sendEvents( Collection c ) {
    Iterator i = c.iterator();
    while(i.hasNext()) {
      ProgramChangeEvent pce = (ProgramChangeEvent)i.next();
      sendPatchChange(pce);
    }
  }

  private void sendPatchChange(ProgramChangeEvent pce) {
    MidiMessage patchChange = new ShortMessage();

    try {
      ((ShortMessage)patchChange)
	.setMessage( ShortMessage.PROGRAM_CHANGE, 
		     pce.getChannel(), 
		     pce.getPatch(), 0 );
      midiOut.send(patchChange, -1);
    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Program Change: " + pce);
      System.out.println(e);
    }
  }
  


  // Implementation of javax.sound.midi.Receiver

  /**
   * Describe <code>close</code> method here.
   *
   */
  public void close() { }

  /**
   * Describe <code>send</code> method here.
   *
   * @param midiMessage a <code>MidiMessage</code> value
   * @param l a <code>long</code> value
   */
  public void send(MidiMessage midiMessage, long l) {
    // OK, we've received a message.  Check the triggers.
    for (int i=0;i<cachedTriggers.length;i++) {
      Trigger trig = cachedTriggers[i];
      if (trig.match(midiMessage)) {
	// call the method
	String action = (String)triggers.get(trig);
	if (trig.equals("advance")) {
	  sendEvents( advancer.advancePatch() );
	  removeTrigger( trig );
	  addCurrentTrigger();
	}
	else if (trig.equals( "reverse" )) {
	  sendEvents( advancer.reversePatch() );
	  triggers = new HashMap();
	  addReverseTrigger();
	}
	else 
	  throw new RuntimeException("Unknown action " + action);

	removeTrigger(trig);
      }
    }
    // no match, just ignore the message.
  }


  Trigger cachedTriggers[] = {};
  Map triggers;

  private void buildTriggerCache() {
    List l = new ArrayList();
    l.addAll(triggers.keySet());
    cachedTriggers = (Trigger[]) l.toArray(cachedTriggers);
  }

  private void addTrigger( Trigger t, String action ) {
    triggers.put(t, action);
    buildTriggerCache();
  }

  private void removeTrigger( Trigger t ) {
    triggers.remove(t);
    buildTriggerCache();
  }

  private void addReverseTrigger() {
    addTrigger( advancer.getQData().getReverseTrigger(), "reverse" );
  }

  private void addCurrentTrigger() {
    addTrigger( advancer.getCurrentCue().getTrigger(), "advance" );
  }
 
  
} // QController
