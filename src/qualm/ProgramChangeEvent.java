package qualm;

public class ProgramChangeEvent {
  int channel;
  Patch patch;
  Patch previousPatch = null;
  public ProgramChangeEvent( int ch, Patch p ) { channel = ch; patch = p; }
  public int getChannel() { return channel; }
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

