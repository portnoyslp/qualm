package qualm;

/**
 * Qualm's internal representation of a MIDI message, similar to the MidiMessage
 * available in javax.sound.midi.
 */
public class MidiCommand {
  int channel;
  int type;
  byte[] data;
  
  /* Type codes */
  public static final int CONTROL_CHANGE = 0xB0;
  public static final int NOTE_OFF = 0x80;
  public static final int NOTE_ON = 0x90;
  public static final int PITCH_BEND = 0xE0;
  public static final int PROGRAM_CHANGE = 0xC0;
  
  /* Special signifier for SYSEX messages */
  public static final int SYSEX = -2;

  
  public MidiCommand(int ch, int type, int data) {
    setParams(ch, type, (byte)data, (byte)0);
  }

  public MidiCommand(int ch, int type, int data1, int data2) {
    setParams(ch, type, (byte)data1, (byte)data2);
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
    if (data.length>0)
      return data[0];
    else return 0;
  }

  public int getData2() {
    if (data.length>1)
      return data[1];
    else return 0;
  }

  public void setParams(int channel, int type, byte data1) {
    setChannel(channel);
    this.type = type;
    data = new byte[1];
    data[0] = (byte) data1;
  }

  public void setParams(int channel, int type, byte data1, byte data2) {
    setChannel(channel);
    this.type = type;
    data = new byte[2];
    data[0] = data1;
    data[1] = data2;
  }

  public void setSysex(byte[] data) {
    this.type = SYSEX;
    this.data = data;
  }

  public byte[] getData() {
    return data;
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
    
    if (cStr.equals("UNKNOWN")) {
      // print out the message, in hex
      String ret = "[DATA: "; 
      for(int i = 0; i<data.length; i++) {
        ret += Integer.toHexString((int) data[i]);
        if (i<data.length-1) ret += " ";
      }
      ret += "]";
      return ret;
    }
    
    return "[" + cStr + " chan:" + (channel + 1) + " d1:" + getData1() + " d2:"
        + getData2() + "]";
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
    setParams(ch,cmd,(byte)d1,(byte)d2);
  }
}
