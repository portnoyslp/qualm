package qualm;

// A simple class for attaching a cue number to a ProgramChangeEvent.

public class CuedProgramChangeEvent extends ProgramChangeEvent
  implements CuedEvent
{
  public CuedProgramChangeEvent( Cue q, ProgramChangeEvent pce ) {
    super(pce.getChannel(), pce.getPatch());
    setPreviousPatch( pce.getPreviousPatch() );
    this.cue = q;
  }
  public Cue getCue() { return cue; }
  Cue cue;
}
