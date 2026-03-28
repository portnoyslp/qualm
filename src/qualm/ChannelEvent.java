package qualm;

/** A QEvent that is addressed to a specific MIDI channel. */
public sealed abstract class ChannelEvent extends QEvent
    permits ProgramChangeEvent, NoteWindowChangeEvent, MidiEvent {
  private int channel;

  public void setChannel(int ch) { this.channel = ch; }
  public int getChannel() { return channel; }
}
