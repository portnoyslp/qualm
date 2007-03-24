package qualm;

// A simple class for attaching a cue number to a NoteWindowChangeEvent.

public class CuedNoteWindowChangeEvent extends NoteWindowChangeEvent
  implements CuedEvent
{
  Cue cue;
  public CuedNoteWindowChangeEvent( Cue q, NoteWindowChangeEvent nwce ) {
    super(nwce.getChannel(), nwce.getBottomNote(), nwce.getTopNote());
    setPrevious( nwce.getPrevious() );
    this.cue = q;
  }
  public Cue getCue() { return cue; }
}
