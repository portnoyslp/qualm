package qualm;

public class ProgramChangeEvent {
  int channel;
  Patch patch;
  public ProgramChangeEvent( int ch, Patch p ) { channel = ch; patch = p; }
  public int getChannel() { return channel; }
  public Patch getPatch() { return patch; }
  public String toString() { return "PC[" + channel + "," + patch + "]"; }
}

