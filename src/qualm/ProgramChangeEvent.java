package qualm;

public class ProgramChangeEvent extends QEvent {
  int channel;
  Patch patch;
  Patch previousPatch = null;
  
  public ProgramChangeEvent( int ch, Cue q, Patch p ) { 
    setChannel(ch); setCue(q); patch=p;
  }
  
  public Patch getPatch() { return patch; }

  public String toString() { 
    String out = "PC[" + getChannel() + "," + getPatch();
    if (getPreviousPatch() != null) 
      out += "(" + getPreviousPatch() + ")";
    return out + "]";
  }
  
  public void setPreviousPatch(Patch p) { previousPatch = p; }
  public Patch getPreviousPatch() { return previousPatch; }
}

