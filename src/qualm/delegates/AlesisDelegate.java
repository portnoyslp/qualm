package qualm.delegates;

import java.util.HashMap;
import java.util.Map;

import qualm.*;

/**
 * ChangeDelegate for Alesis (written for Alesis QS8.1)
 *
 * Note: parameters of note-window-change events for Alesis devices
 * (e.g. top="b6") should be specified in the control file according
 * to the Alesis convention of referring to middle C as C3 rather than
 * C4.  AlesisDelegate automatically adjusts the MIDI note number by
 * an octave to correct for this when it processes the event.
 */
public class AlesisDelegate extends ChangeDelegate {
  
  private static Map<String,Integer> bankNameMap = createBankNameMap();
  private static final Map<String,Integer> createBankNameMap() {
    Map<String, Integer> m = new HashMap<String,Integer>();
    m.put("User", new Integer(0));
    for(int i=1;i<=4;i++)
      m.put("Pr" + i, new Integer(i));
    for(int i=1;i<=8;i++)       
      m.put("Card" + i, new Integer(i+4));
    return m;
  }
  
  public void patchChange(ProgramChangeEvent pce, QReceiver midiOut) {
    MidiCommand msg;

    int channel = pce.getChannel();
    Patch patch = pce.getPatch();

    // Alesis patches are numbered 0-127 (don't need to subtract 1)
    int progNum = patch.getNumber();

    String bankName = patch.getBank();
    if (bankName != null) {
      // translate bank name into bank number
      int bank;
      if (bankNameMap.containsKey(bankName))
        bank = bankNameMap.get(bankName).intValue();
      else
        throw new RuntimeException("invalid bank name: " + bankName);

      // send Control Change 0 to select Bank MSB (note: for Alesis,
      // MSB is the entire bank number; LSB is not used at all)
      msg = new MidiCommand(channel, MidiCommand.CONTROL_CHANGE, 0, bank);

      if (midiOut != null)
        midiOut.handleMidiCommand(msg);
    }

    // send Program Change to select Program
    msg = new MidiCommand(channel, MidiCommand.PROGRAM_CHANGE, progNum, 0);

    if (midiOut != null)
      midiOut.handleMidiCommand(msg);

    Integer volume = patch.getVolume();
    if (volume != null) {
      // send Control Change 7 to set channel volume
      msg = new MidiCommand(channel, MidiCommand.CONTROL_CHANGE, 7, volume.intValue());

      if (midiOut != null)
        midiOut.handleMidiCommand(msg);
    }
  }


  
  public void noteWindowChange( NoteWindowChangeEvent nwce,
				QReceiver midiOut )
  {
    try {
      MidiCommand sysex;
      byte[] data;

      int channel = nwce.getChannel();

      // Alesis SysEx format for MIDI parameter editing:
      //
      // F0 00 00 0E 0E 10
      //
      // followed by four data bytes made up of the following bits:
      // <0mmfffff> <0ssppppp> <0ccccddv> <0vvvvvv>
      //   mm = 1 (Mix)
      //   fffff = 4 (function for Channel low/high note)
      //   ss = 0 (not used when editing Mix parameters)
      //   ppppp = 0 (page for Channel low/high note)
      //   cccc = channel (0-15), 4 bit unsigned
      //   dd = 0 or 1 (data pot for Channel low/high note)
      //   vvvvvvvv = parameter value, 8 bit 2's complement
      //
      // followed by F7

      if (nwce.getBottomNote() != null)
      {
	// add an octave here to make "c3" in the control file
	// correspond to middle C.
	int bottomNote = nwce.getBottomNote().intValue() + 12;

	// send SysEx to set note window bottom for Part
	data = new byte[]
	  { (byte) 0xF0, 0, 0, 0x0E, 0x0E, 0x10, 0x24, 0,
	    (byte) (channel << 3), (byte) bottomNote, (byte) 0xF7 };

	sysex = new MidiCommand();
	sysex.setSysex( data );

	if (midiOut != null)
	  midiOut.handleMidiCommand(sysex);
      }

      if (nwce.getTopNote() != null)
      {
	// add an octave here to make "c3" in the control file
	// correspond to middle C.
	int topNote = nwce.getTopNote().intValue() + 12;

	// send SysEx to set note window top for Part
	data = new byte[]
	  { (byte) 0xF0, 0, 0, 0x0E, 0x0E, 0x10, 0x24, 0,
	    (byte) (channel << 3 | 2), (byte) topNote, (byte) 0xF7 };

	sysex = new MidiCommand();
	sysex.setSysex( data );

	if (midiOut != null)
	  midiOut.handleMidiCommand(sysex);
      }
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }
}
