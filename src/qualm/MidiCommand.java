package qualm;

/**
 * Qualm's internal representation of a MIDI message, similar to the MidiMessage
 * available in javax.sound.midi.
 */
public class MidiCommand {
  int channel;
  int type;
  int data1;
  int data2;

  /* Type codes */
  public static final int CONTROL_CHANGE = 0xB0;
  public static final int NOTE_OFF = 0x80;
  public static final int NOTE_ON = 0x90;
  public static final int PITCH_BEND = 0xE0;
  public static final int PROGRAM_CHANGE = 0xC0;

  public MidiCommand(int ch, int type, int data) {
    setParams(ch, type, data, 0);
  }

  public MidiCommand(int ch, int type, int data1, int data2) {
    setParams(ch, type, data1, data2);
  }

  public MidiCommand() {
     // totally blank, in case you want to use setParams()
  }

  public void setChannel(int ch) {
    channel = ch;
  }

  public int getChannel() {
    return channel;
  }

  public int getType() {
    return type;
  }

  public int getData1() {
    return data1;
  }

  public int getData2() {
    return data2;
  }

  public void setParams(int channel, int type, int data1) {
    setChannel(channel);
    this.type = type;
    this.data1 = data1;
  }

  public void setParams(int channel, int type, int data1, int data2) {
    setChannel(channel);
    this.type = type;
    this.data1 = data1;
    this.data2 = data2;
  }

  public String toString() {
    String cStr = "UNKNOWN";
    switch (type) {
    case CONTROL_CHANGE:
      cStr = "ControlChange";
      break;
    case NOTE_OFF:
      cStr = "NoteOff";
      break;
    case NOTE_ON:
      cStr = "NoteOn";
      break;
    case PITCH_BEND:
      cStr = "PitchBend";
      break;
    case PROGRAM_CHANGE:
      cStr = "ProgramChange";
      break;
    }

    return "[" + cStr + " chan:" + (channel + 1) + " d1:" + data1 + " d2:"
        + data2 + "]";
  }
  
  /**
   * A helper function for compatibility with ShortMessage code
   * @param cmd
   * @param ch
   * @param d1
   * @param d2
   * @deprecated
   */
  public void setMessage(int cmd, int ch, int d1, int d2) {
    setParams(ch,cmd,d1,d2);
  }
}
