package qualm;

public final class ProgramChangeEvent extends ChannelEvent {
  Patch patch;

  public ProgramChangeEvent( int ch, Cue q, Patch p ) {
    setChannel(ch); setCue(q); patch=p;
  }

  public Patch getPatch() { return patch; }

  public String toString() {
    return "PrCh[" + getChannel() + "," + getPatch() + "]";
  }
}

