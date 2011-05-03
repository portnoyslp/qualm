package qualm;

import javax.sound.midi.ShortMessage;

/**
 * Qualm's internal representation of a MIDI message, similar to the MidiMessage available in javax.sound.midi.
 * 
 * @author speters
 */
public class MidiCommand {
  int channel;
  int type;
  int data1;
  int data2;
  
  /* Type codes */
  static int CONTROL_CHANGE = 0xB0;
  static int NOTE_OFF = 0x80;
  static int NOTE_ON = 0x90;
  static int PITCH_BEND = 0xE0;
  static int PROGRAM_CHANGE = 0xC0;
  
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
  
  public void setParams(int type, int data1, int data2) {
    this.type = type;
    this.data1 = data1;
    this.data2 = data2;
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

    return "[" + cStr + " chan:" + (channel + 1) + " d1:" + d1 + " d2:" + d2 + "]";
  }
}
