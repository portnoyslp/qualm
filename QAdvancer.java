package qualm;

import java.util.Collection;
import java.util.SortedSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;



/**
 * QAdvancer.java
 *
 *
 * Created: Thu Jan 15 09:49:05 2004
 *
   */
public class QAdvancer {

  QData qdata;

  Cue currentCue;
  Cue pendingCue;
 
  public QAdvancer( QData d ) {
    qdata = d;
    currentCue = null;
    pendingCue = findNextCue(null);
  }

  public QData getQData() { return qdata; }
  public Cue getCurrentCue() { return currentCue; }
  public Cue getPendingCue() { return pendingCue; }

  public Collection getPresets() {
    return qdata.getSetupEvents();
  }

  public Collection switchToMeasure(String cueName) { 
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

    return revertPatchChanges();
  }
  
  public Collection advancePatch() {
    // send the next patch change
    
    Cue nextCue = findNextCue(currentCue);
    
    // find patch changes
    Collection events = getPatchChanges(nextCue);

    currentCue = nextCue;

    // find the next cue...
    pendingCue = findNextCue( currentCue );

    return events;
  } 

  public Collection reversePatch() {
    // find previous patch
    SortedSet cueset = qdata.getCues();
    SortedSet head = cueset.headSet ( currentCue );
    if (head.size() == 0) {
      return switchToMeasure( "0.0" );
    } else {
      Cue lastCue = (Cue)head.last();
      return switchToMeasure(lastCue.getCueNumber());
    }
  }

  private Collection getPatchChanges(Cue c) {
    return c.getEvents();
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


  private Collection revertPatchChanges() {
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

    List out = new ArrayList();
    for(int i=0; i<events.length; i++) {
      if (events[i] != null)
	out.add(events[i]);
    }
    return out;

  }


}



