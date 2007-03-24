package qualm;

import javax.sound.midi.*;

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
  public void patchChange( ProgramChangeEvent pce,
			   Receiver midiOut ) {
    try {
      ShortMessage msg;

      int channel = pce.getChannel();
      Patch patch = pce.getPatch();

      // Alesis patches are numbered 0-127 (don't need to subtract 1)
      int progNum = patch.getNumber();

      String bankName = patch.getBank();
      if (bankName != null)
      {
	// translate bank name into bank number
	int bank;
	if (bankName.equals("User")) bank = 0;
	else if (bankName.equals("Pr1")) bank = 1;
	else if (bankName.equals("Pr2")) bank = 2;
	else if (bankName.equals("Pr3")) bank = 3;
	else if (bankName.equals("Pr4")) bank = 4;
	else if (bankName.equals("Card1")) bank = 5;
	else if (bankName.equals("Card2")) bank = 6;
	else if (bankName.equals("Card3")) bank = 7;
	else if (bankName.equals("Card4")) bank = 8;
	else if (bankName.equals("Card5")) bank = 9;
	else if (bankName.equals("Card6")) bank = 10;
	else if (bankName.equals("Card7")) bank = 11;
	else if (bankName.equals("Card8")) bank = 12;
	else
	  throw new Exception("invalid bank name: " + bankName);

	// send Control Change 0 to select Bank MSB (note: for Alesis,
	// MSB is the entire bank number; LSB is not used at all)
	msg = new ShortMessage();
	msg.setMessage( ShortMessage.CONTROL_CHANGE,
			channel, 0, bank );

	if (midiOut != null)
	  midiOut.send(msg,-1);
      }

      // send Program Change to select Program
      msg = new ShortMessage();
      msg.setMessage( ShortMessage.PROGRAM_CHANGE, 
		      channel, progNum, 0 );

      if (midiOut != null) 
	midiOut.send(msg, -1);

      Integer volume = patch.getVolume();
      if (volume != null)
      {
	// send Control Change 7 to set channel volume
	msg = new ShortMessage();
	msg.setMessage( ShortMessage.CONTROL_CHANGE,
			channel, 7, volume.intValue() );

	if (midiOut != null)
	  midiOut.send(msg,-1);
      }
    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Program Change: " + pce);
      System.out.println(e);
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }


  
  public void noteWindowChange( NoteWindowChangeEvent nwce,
				Receiver midiOut )
  {
    try {
      SysexMessage sysex;
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

	sysex = new SysexMessage();
	sysex.setMessage( data, data.length );

	if (midiOut != null)
	  midiOut.send(sysex, -1);
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

	sysex = new SysexMessage();
	sysex.setMessage( data, data.length );

	if (midiOut != null)
	  midiOut.send(sysex, -1);
      }

    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Note Window Change: " + nwce);
      System.out.println(e);
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }
}
