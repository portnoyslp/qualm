package qualm;

import java.util.Collection;
import java.util.SortedSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;


/**
 * QAdvancer
 */
public class QAdvancer {

  QStream qstream;
  QData qdata;

  Cue currentCue;
  Cue pendingCue;

  public QAdvancer( QStream s, QData d ) {
    qstream = s;
    qdata = d;
    currentCue = null;
    pendingCue = findNextCue(null);
  }

  public QStream getQStream() { return qstream; }
  public Cue getCurrentCue() { return currentCue; }
  public Cue getPendingCue() { return pendingCue; }

  public Collection switchToMeasure(String cueName) { 
    Cue newQ = new Cue( cueName );
    SortedSet cueset = qstream.getCues();
    SortedSet head = cueset.headSet(newQ);

    // set the new current cue
    if (head.size() == 0) {
      // the first cue is always the first in the stream
      currentCue = (Cue)cueset.first();
    } else {
      currentCue = (Cue)head.last();
    }

    // if our target cue equals the next one, then use it instead.
    try {
      Cue nextQ = findNextCue( currentCue );
      if (nextQ != null && newQ.equals( nextQ )) {
	currentCue = nextQ;
      }
    } catch (Exception e) {
      e.printStackTrace();
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
    SortedSet cueset = qstream.getCues();
    SortedSet head;
    try { 
      head = cueset.headSet ( currentCue ) ;
    } 
    catch (NullPointerException npe) {
      return switchToMeasure( "0.0" );
    }

    if (head.size() == 0)
      return switchToMeasure( "0.0" );

    Cue previousCue = (Cue)head.last();

    // OK, now we have to back out the patch.  We're going to create
    // new PCE's which will back out the patch whenever possible.
    Collection out = new ArrayList();
    Iterator iter = currentCue.getEvents().iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof ProgramChangeEvent) {
	ProgramChangeEvent pce = (ProgramChangeEvent)obj;
	out.add( new ProgramChangeEvent(pce.getChannel(), pce.getPreviousPatch()) );
      }
    }

    pendingCue = currentCue;
    currentCue = previousCue;
    return out;
  }

  private Collection getPatchChanges(Cue c) {
    return c.getEvents();
  }

  private Cue findNextCue( Cue current ) {
    SortedSet cueset = qstream.getCues();

    if (current == null)
      return (Cue)cueset.first();

    // 'increment' measure number.
    SortedSet tail = cueset.tailSet( new Cue( current.getSong(),
					      current.getMeasure() + "_" ));
    if (tail.size() == 0) return null;
    return (Cue)tail.first();
  }


  /**
   * Send all necessary patch changes to get to the current cue.  To
   * do this right, we need to keep track of which channels have been
   * marked, and go back through the cues to find the earliest
   * previous program change for each channel, and then apply those.
   *
   * @return a <code>Collection</code> of
   * <code>PatchChangeEvent</code> objects. */

  private Collection revertPatchChanges() {
    String[] midiChans = qdata.getMidiChannels();
    int channelCount = 0;
    for(int i=0; i<midiChans.length; i++) 
      if (midiChans[i] != null) channelCount++;
      
    CuedProgramChangeEvent[] events = new CuedProgramChangeEvent[ midiChans.length ];
    for(int i=0; i<events.length; i++) events[i] = null;

    int programCount = 0;

    // go backward through the events
    SortedSet headset = qstream.getCues();
    Cue loopQ = currentCue;
    while ( loopQ != null && programCount < channelCount ) {
      Iterator iter = loopQ.getEvents().iterator();
      while (iter.hasNext()) {
	Object obj = iter.next();
	if (obj instanceof ProgramChangeEvent) {
	  ProgramChangeEvent pce = (ProgramChangeEvent)obj;
	  CuedProgramChangeEvent ev = 
	    new CuedProgramChangeEvent(loopQ, pce);
	  int ch = ev.getChannel();
	  if (events[ch] == null && midiChans[ch] != null) {
	    events[ch] = ev;
	    programCount++;
	  }
	}
      }

      //Update headset
      headset = headset.headSet( loopQ );
      if (headset.size() == 0) {
	loopQ = null;
      } else
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
