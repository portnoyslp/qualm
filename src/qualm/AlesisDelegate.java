package qualm;

import javax.sound.midi.*;

/**
 * ChangeDelegate for Alesis (written for Alesis QS8.1)
 */
public class AlesisDelegate extends ChangeDelegate {
  public void patchChange( ProgramChangeEvent pce,
			   Receiver midiOut ) {
    try {
      ShortMessage msg;

      int ch = pce.getChannel();
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
	// else if (bankName.equals("CardA")) bank = ??;
	// else if (bankName.equals("CardB")) bank = ??;
	else
	  throw new Exception("invalid bank name: " + bankName);

	// send Control Change 0 to select Bank MSB (note: for Alesis,
	// MSB is the entire bank number; LSB is not used at all)
	msg = new ShortMessage();
	msg.setMessage( ShortMessage.CONTROL_CHANGE,
			ch, 0, bank );

	if (midiOut != null)
	  midiOut.send(msg,-1);
      }

      // send Program Change to select Program
      msg = new ShortMessage();
      msg.setMessage( ShortMessage.PROGRAM_CHANGE, 
		      ch, progNum, 0 );

      if (midiOut != null) 
	midiOut.send(msg, -1);
      
    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Program Change: " + pce);
      System.out.println(e);
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }
  
}

