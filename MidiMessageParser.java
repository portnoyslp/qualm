package qualm;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

/**
 * Converts MidiMessages into Strings.
 */

public class MidiMessageParser {
  
  public static String messageToString(MidiMessage mm) {
    if (mm instanceof ShortMessage) {
      ShortMessage sm = (ShortMessage) mm;
      int channel = sm.getChannel();
      int command = sm.getCommand();
      int d1 = sm.getData1();
      int d2 = sm.getData2();
      String cStr = "UNKNOWN";
      switch (command) {
      case ShortMessage.CHANNEL_PRESSURE:
	cStr = "ChannelPressure"; break;
      case ShortMessage.CONTROL_CHANGE:
	cStr = "ControlChange"; break;
      case ShortMessage.NOTE_OFF:
	cStr = "NoteOff"; break;
      case ShortMessage.NOTE_ON:
	cStr = "NoteOn"; break;
      case ShortMessage.PITCH_BEND:
	cStr = "PitchBend"; break;
      case ShortMessage.PROGRAM_CHANGE:
	cStr = "ProgramChange"; break;
      }
      return "[" + cStr + " chan:" + (channel+1) +
	" d1:" + d1 + " d2:" + d2 + "]";
    }
    else return mm.toString();
  }
  
}
