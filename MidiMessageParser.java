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
      case ShortMessage.POLY_PRESSURE:
	cStr = "PolyPressure"; break;
      case ShortMessage.PITCH_BEND:
	cStr = "PitchBend"; break;
      case ShortMessage.MIDI_TIME_CODE:
	cStr = "MidiTimeCode"; break;
      case ShortMessage.SONG_POSITION_POINTER:
	cStr = "SongPositionPointer"; break;
      case ShortMessage.SONG_SELECT:
	cStr = "SongSelect"; break;
      case ShortMessage.TUNE_REQUEST:
	cStr = "TuneRequest"; break;
      case ShortMessage.TIMING_CLOCK:
	cStr = "TimingClock"; break;
      case ShortMessage.START:
	cStr = "Start"; break;
      case ShortMessage.CONTINUE:
	cStr = "Continue"; break;
      case ShortMessage.STOP:
	cStr = "Stop"; break;
      case ShortMessage.ACTIVE_SENSING:
	cStr = "ActiveSensing"; break;
      case ShortMessage.SYSTEM_RESET:
	cStr = "SystemReset"; break;
      case ShortMessage.END_OF_EXCLUSIVE:
	cStr = "EndOfExclusive"; break;

      }
      return "[" + cStr + " chan:" + (channel+1) +
	" d1:" + d1 + " d2:" + d2 + "]";
    }
    else return mm.toString();
  }
  
}
