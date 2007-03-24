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

  // returns Collection of CuedEvent
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
    Cue nextQ = findNextCue( currentCue );
    if (nextQ != null && newQ.equals( nextQ )) {
      currentCue = nextQ;
    }

    pendingCue = findNextCue( currentCue );

    return revertPatchChanges();
  }
  
  public Collection advancePatch() {
    // send the next patch change
    Cue nextCue = findNextCue(currentCue);

    if (nextCue != null) {

      // find patch changes
      Collection events = getPatchChanges(nextCue);
      
      currentCue = nextCue;

      // find the next cue...
      pendingCue = findNextCue( currentCue );

      return events;

    } else {
      return new ArrayList(); 
    }

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
	if (pce.getPreviousPatch() != null)
	  out.add( new ProgramChangeEvent(pce.getChannel(),
					  pce.getPreviousPatch()) );
      }
      else if (obj instanceof NoteWindowChangeEvent) {
	out.add( ((NoteWindowChangeEvent)obj).getUndoEvent() );
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
   * <code>CuedEvent</code> objects. */

  private Collection revertPatchChanges() {
    String[] midiChans = qdata.getMidiChannels();
    int channelCount = 0;
    for(int i=0; i<midiChans.length; i++) 
      if (midiChans[i] != null) channelCount++;
      
    CuedProgramChangeEvent[] events = new CuedProgramChangeEvent[ midiChans.length ];
    for(int i=0; i<events.length; i++) events[i] = null;

    CuedNoteWindowChangeEvent[] nwcEvents = new CuedNoteWindowChangeEvent[ midiChans.length ];
    for(int i=0; i<nwcEvents.length; i++) nwcEvents[i] = null;

    int programCount = 0, noteWindowCount = 0;

    // go backward through the events
    SortedSet headset = qstream.getCues();
    Cue loopQ = currentCue;
    while ( loopQ != null && (programCount < channelCount ||
			      noteWindowCount < channelCount) ) {
      Iterator iter = loopQ.getEvents().iterator();
      while (iter.hasNext()) {
	Object obj = iter.next();
	if (programCount < channelCount &&
	    obj instanceof ProgramChangeEvent) {
	  ProgramChangeEvent pce = (ProgramChangeEvent)obj;
	  CuedProgramChangeEvent ev = 
	    new CuedProgramChangeEvent(loopQ, pce);
	  int ch = ev.getChannel();
	  if (events[ch] == null && midiChans[ch] != null) {
	    events[ch] = ev;
	    programCount++;
	  }
	}
	else if (noteWindowCount < channelCount &&
		 obj instanceof NoteWindowChangeEvent) {
	  NoteWindowChangeEvent nwce = (NoteWindowChangeEvent)obj;
	  int ch = nwce.getChannel();
	  if (nwcEvents[ch] == null && midiChans[ch] != null) {
	    nwcEvents[ch] = new CuedNoteWindowChangeEvent
	      (loopQ, nwce.getPostStateEvent());
	    noteWindowCount++;
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
    for(int i=0; i<nwcEvents.length; i++) {
      if (nwcEvents[i] != null)
	out.add(nwcEvents[i]);
    }
    return out;
  }

}
