package qualm;

/** A QEvent that is addressed to a specific MIDI channel. */
public abstract class ChannelEvent extends QEvent {
  private int channel;

  public void setChannel(int ch) { this.channel = ch; }
  public int getChannel() { return channel; }
}
