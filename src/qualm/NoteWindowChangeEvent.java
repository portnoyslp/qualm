package qualm;

public class NoteWindowChangeEvent {
  int channel;
  Integer bottomNote, topNote; // either or both may be null
  NoteWindowChangeEvent previous = null;

  public NoteWindowChangeEvent( int ch, Integer bottomNote, Integer topNote )
  {
    channel = ch; this.bottomNote = bottomNote; this.topNote = topNote;
  }

  public int getChannel() { return channel; }

  public Integer getBottomNote() { return bottomNote; }

  public Integer getTopNote() { return topNote; }

  public String toString() { 
    return "NoteWindowChange[" + getChannel() + "," +
      _midiToNoteName(bottomNote) + "," + _midiToNoteName(topNote) + "]";
  }

  /**
   * Keep track of the NoteWindowChangeEvent that happens just before
   * this one on the same channel (for undo purposes).
   */
  public void setPrevious( NoteWindowChangeEvent e ) { previous = e; }

  public NoteWindowChangeEvent getPrevious() { return previous; }

  /**
   * Creates a new NWCE that will recreate the note-window state of
   * the channel prior to this NWCE by setting both the top and bottom
   * notes to the values they should have had before this NWCE (based
   * on the chain of previous-event links created with the setPrevious
   * method).
   */
  public NoteWindowChangeEvent getPriorStateEvent()
  {
    Integer top = null, bottom = null;

    // traverse the link chain until we have found both the most
    // recently set top note and the most recently set bottom note, or
    // until we run out of links.
    for ( NoteWindowChangeEvent n = this.previous;
	  top == null || bottom == null;
	  n = n.previous )
    {
      if (n == null)
	break;
      if (top == null && n.getTopNote() != null)
	top = n.getTopNote();
      if (bottom == null && n.getBottomNote() != null)
	bottom = n.getBottomNote();
    }

    // create an event to set this top and bottom note
    return new NoteWindowChangeEvent(this.channel, bottom, top);
  }

  /**
   * Creates a new NWCE that will undo the effects of this NWCE by
   * setting _only those parameters this NWCE actually changes_ to the
   * values they should have had before this NWCE (based on the chain
   * of previous-event links created with the setPrevious method).
   */
  public NoteWindowChangeEvent getUndoEvent()
  {
    NoteWindowChangeEvent n = getPriorStateEvent();

    // don't need to undo parameters this event doesn't change
    if (this.topNote == null)
      n.topNote = null;
    if (this.bottomNote == null)
      n.bottomNote = null;

    return n;
  }

  /**
   * Returns a single NWCE that will create the proper note-window
   * state that this channel should have after executing all previous
   * events (based on the chain of previous-event links created with
   * the setPrevious method) and then this one.
   */
  public NoteWindowChangeEvent getPostStateEvent()
  {
    // first check if this event is sufficient by itself
    if (this.topNote != null && this.bottomNote != null)
      return this;

    // otherwise, reproduce the prior state and then apply changes
    // from this event
    NoteWindowChangeEvent n = getPriorStateEvent();

    if (this.topNote != null)
      n.topNote = this.topNote;
    if (this.bottomNote != null)
      n.bottomNote = this.bottomNote;

    return n;
  }

  private static final String[] keys =
  {"c","c#","d","d#","e","f","f#","g","g#","a","a#","b"};

  private static String _midiToNoteName(Integer note)
  {
    if (note == null)
      return "null";
    int n = note.intValue();
    int octave = (n / 12) - 1; // 60 -> 4, 59 -> 3
    String key = keys[n % 12];
    return key + octave;
  }

}
