package qualm;

public class ProgramChangeEvent {
  int channel;
  int patch;
  public ProgramChangeEvent( int ch, int p ) { channel = ch; patch = p; }
  public int getChannel() { return channel; }
  public int getPatch() { return patch; }
  public String toString() { return "PC[" + channel + "," + patch + "]"; }
}

