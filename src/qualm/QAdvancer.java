package qualm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;


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

  // returns Collection of events
  public Collection<QEvent> switchToMeasure(String cueName) { 
    Cue newQ = new Cue( cueName );
    SortedSet<Cue> cueset = qstream.getCues();
    SortedSet<Cue> head = cueset.headSet(newQ);

    // set the new current cue
    if (head.size() == 0) {
      // the first cue is always the first in the stream
      currentCue = cueset.first();
    } else {
      currentCue = head.last();
    }

    // if our target cue equals the next one, then use it instead.
    Cue nextQ = findNextCue( currentCue );
    if (nextQ != null && newQ.equals( nextQ )) {
      currentCue = nextQ;
    }

    pendingCue = findNextCue( currentCue );

    return revertPatchChanges();
  }
  
  public Collection<QEvent> advancePatch() {
    // send the next patch change
    Cue nextCue = findNextCue(currentCue);

    if (nextCue != null) {

      // find patch changes
      Collection<QEvent> events = getPatchChanges(nextCue);
      
      currentCue = nextCue;

      // find the next cue...
      pendingCue = findNextCue( currentCue );

      return events;

    } else {
      return new ArrayList<QEvent>(); 
    }

  } 

  public Collection<QEvent> reversePatch() {
    SortedSet<Cue> cueset = qstream.getCues();
    SortedSet<Cue> head;
    try {
      head = cueset.headSet( currentCue );
    }
    catch (NullPointerException npe) {
      return switchToMeasure( "0.0" );
    }

    if (head.size() == 0)
      return switchToMeasure( "0.0" );

    Cue previousCue = head.last();

    List<QEvent> out = new ArrayList<QEvent>();
    for (QEvent obj : currentCue.getEvents()) {
      if (obj instanceof ProgramChangeEvent pce) {
        Patch prior = findEffectivePatch(previousCue, pce.getChannel());
        if (prior != null)
          out.add(new ProgramChangeEvent(pce.getChannel(), pce.getCue(), prior));
      } else if (obj instanceof NoteWindowChangeEvent nwce) {
        out.add(findEffectiveNoteWindow(previousCue, nwce.getChannel(),
            nwce.getTopNote() != null, nwce.getBottomNote() != null, nwce.getCue()));
      }
    }

    pendingCue = currentCue;
    currentCue = previousCue;
    return out;
  }

  /** Scans backward from startCue (inclusive) to find the most recent patch on the given channel. */
  private Patch findEffectivePatch(Cue startCue, int channel) {
    SortedSet<Cue> headset = qstream.getCues();
    Cue loopQ = startCue;
    while (loopQ != null) {
      for (QEvent obj : loopQ.getEvents()) {
        if (obj instanceof ProgramChangeEvent pce) {
          if (pce.getChannel() == channel)
            return pce.getPatch();
        }
      }
      headset = headset.headSet(loopQ);
      loopQ = headset.isEmpty() ? null : headset.last();
    }
    return null;
  }

  /**
   * Scans backward from startCue (inclusive) to reconstruct the note-window state for a channel.
   * Only fills in the top/bottom components that the original event controlled (needTop/needBottom).
   */
  private NoteWindowChangeEvent findEffectiveNoteWindow(Cue startCue, int channel,
      boolean needTop, boolean needBottom, Cue cueRef) {
    Integer top = null, bottom = null;
    SortedSet<Cue> headset = qstream.getCues();
    Cue loopQ = startCue;
    while (loopQ != null && ((needTop && top == null) || (needBottom && bottom == null))) {
      for (QEvent obj : loopQ.getEvents()) {
        if (obj instanceof NoteWindowChangeEvent nwce) {
          if (nwce.getChannel() == channel) {
            if (needTop && top == null && nwce.getTopNote() != null)
              top = nwce.getTopNote();
            if (needBottom && bottom == null && nwce.getBottomNote() != null)
              bottom = nwce.getBottomNote();
          }
        }
      }
      headset = headset.headSet(loopQ);
      loopQ = headset.isEmpty() ? null : headset.last();
    }
    return new NoteWindowChangeEvent(channel, cueRef, bottom, top);
  }

  private Collection<QEvent> getPatchChanges(Cue c) {
    return c.getEvents();
  }

  private Cue findNextCue( Cue current ) {
    SortedSet<Cue> cueset = qstream.getCues();

    if (current == null)
      return (Cue)cueset.first();

    // 'increment' measure number.
    SortedSet<Cue> tail = cueset.tailSet( new Cue( current.getSong(),
					      current.getMeasure() + "_" ));
    if (tail.size() == 0) return null;
    return tail.first();
  }


  /**
   * Send all necessary patch changes to get to the current cue.  To
   * do this right, we need to keep track of which channels have been
   * marked, and go back through the cues to find the earliest
   * previous program change for each channel, and then apply those.
   *
   * @return a <code>Collection</code> of
   * <code>CuedEvent</code> objects. */

  private Collection<QEvent> revertPatchChanges() {
    String[] midiChans = qdata.getMidiChannels();

    ProgramChangeEvent[] pceEvents = new ProgramChangeEvent[midiChans.length];
    Integer[] nwcTop = new Integer[midiChans.length];
    Integer[] nwcBottom = new Integer[midiChans.length];
    boolean[] nwcSeen = new boolean[midiChans.length];

    SortedSet<Cue> headset = qstream.getCues();
    Cue loopQ = currentCue;
    while (loopQ != null) {
      for (QEvent obj : loopQ.getEvents()) {
        if (obj instanceof ProgramChangeEvent pce) {
          int ch = pce.getChannel();
          if (pceEvents[ch] == null && midiChans[ch] != null)
            pceEvents[ch] = pce;
        } else if (obj instanceof NoteWindowChangeEvent nwce) {
          int ch = nwce.getChannel();
          if (midiChans[ch] != null) {
            nwcSeen[ch] = true;
            if (nwcTop[ch] == null && nwce.getTopNote() != null)
              nwcTop[ch] = nwce.getTopNote();
            if (nwcBottom[ch] == null && nwce.getBottomNote() != null)
              nwcBottom[ch] = nwce.getBottomNote();
          }
        }
      }
      headset = headset.headSet(loopQ);
      loopQ = headset.isEmpty() ? null : headset.last();
    }

    List<QEvent> out = new ArrayList<QEvent>();
    for (int i = 0; i < pceEvents.length; i++) {
      if (pceEvents[i] != null) out.add(pceEvents[i]);
    }
    for (int i = 0; i < midiChans.length; i++) {
      if (nwcSeen[i])
        out.add(new NoteWindowChangeEvent(i, currentCue, nwcBottom[i], nwcTop[i]));
    }
    return out;
  }

}
