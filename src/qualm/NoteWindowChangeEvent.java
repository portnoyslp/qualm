package qualm;

public class NoteWindowChangeEvent extends QEvent {
  Integer bottomNote, topNote; // either or both may be null

  public NoteWindowChangeEvent( int ch, Cue q, Integer bottomNote, Integer topNote )
  {
    this.bottomNote = bottomNote; this.topNote = topNote;
    setCue(q);
    setChannel(ch);
  }

  public Integer getBottomNote() { return bottomNote; }

  public Integer getTopNote() { return topNote; }

  public String toString() {
    return "NoteWindowChange[" + getChannel() + "," +
      _midiToNoteName(bottomNote) + "-" + _midiToNoteName(topNote) + "]";
  }

  private static String _midiToNoteName(Integer note)
  {
    if (note == null) return "";
    return Utilities.midiNumberToNoteName( note.intValue() );
  }

}
