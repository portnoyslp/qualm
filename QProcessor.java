package qualm;

import javax.sound.midi.Receiver;
import javax.sound.midi.MidiMessage;
import java.util.Iterator;
import java.util.SortedSet;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.InvalidMidiDataException;
import java.lang.reflect.Method;



/**
 * Main processing thread.  This keeps track of the QData and the
 * current position in it, and coordinates with the event receiver and
 * the read-eval-print loop.
 */

public class QProcessor {
  QualmLoop repl;
  QReceiver receiver;
  QData qdata;
  Receiver midiOut;

  Cue currentCue;
  Cue pendingCue;
  
  public QProcessor( Receiver out, QData qd ) {
    midiOut = out;
    qdata = qd;

    currentCue = null;
  }

  public void setRepl(QualmLoop r) { repl = r; }

  protected void sendPresets() {
    Iterator i = qdata.getSetupEvents().iterator();
    while(i.hasNext()) {
      ProgramChangeEvent pce = (ProgramChangeEvent)i.next();
      sendPatchChange(pce);
    }
  }

  protected void sendPresetOnChannel(int channel) {
    Iterator i = qdata.getSetupEvents().iterator();
    while(i.hasNext()) {
      ProgramChangeEvent pce = (ProgramChangeEvent)i.next();
      if (pce.getChannel() == channel)
	sendPatchChange(pce);
    }
  }

  public void switchToMeasure(String cueName) { 
    Cue newQ = new Cue( cueName );
    SortedSet cueset = qdata.getCues();
    SortedSet head = cueset.headSet(newQ);
    
    // set the new current cue
    if (head.size() == 0) {
      currentCue = null;
    } else {
      currentCue = (Cue)head.last();
    }
    pendingCue = findNextCue( currentCue );

    revertPatchChanges();
  }

  /**
   * Send the next patch change.  This is a callback from the QReceiver.
   *
   * @param trig a <code>Trigger</code> value
   * @param msg a <code>MidiMessage</code> value
   * @return the next pending cue
   */
  public void advancePatch( Trigger trig, MidiMessage msg ) { 
    // send the next patch change

    Cue nextCue = findNextCue(currentCue);
    
    // trigger the patch change
    sendPatchChange(nextCue);

    currentCue = nextCue;

    // find the next cue...
    pendingCue = findNextCue( currentCue );
  } 


  public void reversePatch( Trigger trig, MidiMessage msg ) { 
    // find previous patch
    SortedSet cueset = qdata.getCues();
    SortedSet head = cueset.headSet ( currentCue );
    Cue lastCue = (Cue)head.last();
    switchToMeasure(lastCue.getCueNumber());
  } 



  /**** Private Messages ****/

  private void scheduleNextCue() {
    try {
      Method m = QProcessor.class.getMethod( "advancePatch",
					     new Class[] { Trigger.class,
							   MidiMessage.class });
      
      receiver.addTrigger( pendingCue.getTrigger(), this, m );
    } catch (Exception e) { e.printStackTrace(); }
  }

  private Cue findNextCue( Cue current ) {
    SortedSet cueset = qdata.getCues();

    if (current == null)
      return (Cue)cueset.first();

    // 'increment' measure number.
    SortedSet tail = cueset.tailSet( new Cue( current.getSong(),
					      current.getMeasure() + "_" ));
    if (tail.size() == 0) return null;
    return (Cue)tail.first();
  }


  private void revertPatchChanges() {
    // send the patch changes for the current cue.  To do this right,
    // we need to keep track of which channels have been marked, and
    // go back through the cues to find the earliest previous program
    // change for each channel, and then apply those.

    String[] midiChans = qdata.getMidiChannels();
    int channelCount = 0;
    for(int i=0; i<midiChans.length; i++) 
      if (midiChans[i] != null) channelCount++;
      
    ProgramChangeEvent[] events = new ProgramChangeEvent[ midiChans.length ];
    for(int i=0; i<events.length; i++) events[i] = null;

    int programCount = 0;

    // go backward through the events
    SortedSet headset = qdata.getCues();
    Cue loopQ = currentCue;
    while ( loopQ != null && programCount < channelCount ) {
      Iterator iter = currentCue.getEvents().iterator();
      while (iter.hasNext()) {
	ProgramChangeEvent ev = (ProgramChangeEvent)iter.next();
	int ch = ev.getChannel();
	if (events[ch] == null && midiChans[ch] != null) {
	  events[ch] = ev;
	  programCount++;
	}
      }

      //Update headset
      headset = headset.headSet( loopQ );
      if (headset.size() == 0)
	loopQ=null;
      else
	loopQ = (Cue)headset.last();
    }

    // send all the patch changes
    for(int i=0; i<events.length; i++) {
      if (events[i] != null)
	sendPatchChange(events[i]);
    }

  }

  private void sendPatchChange(Cue c) {
    Iterator i = c.getEvents().iterator();
    while (i.hasNext()) {
      ProgramChangeEvent ev = (ProgramChangeEvent)i.next();
      sendPatchChange(ev);
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


}
