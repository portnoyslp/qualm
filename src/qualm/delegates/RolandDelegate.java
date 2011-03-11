package qualm.delegates;

import qualm.*;
import qualm.Patch;
import javax.sound.midi.*;

public class RolandDelegate extends ChangeDelegate {
  protected int[] _bankSelector ( String bankName, int patchNum ) {
    // Takes in the bank name, and returns two integers which are the
    // values for the BankSelect command.

    // This is based on the JV-1080, which is common across most JV
    // synths and should be compatible with many Roland keyboards.
    // The XV series has a subclass with more robust bank selection
    // (see RolandXVDelegate.java).
    
    // we need to select the proper bank
    int bank = -1;
    if (bankName.equals("User")) bank = 0;
    else if (bankName.equals("PrA")) bank=8;
    else if (bankName.equals("PrB")) bank=9;
    else if (bankName.equals("PrC")) bank=10;
    else if (bankName.equals("PrD")) bank=11;
    else if (bankName.equals("Data")) bank=16;
    else if (bankName.equals("PCM")) bank=24;
    else if (bankName.equals("XpA")) bank=32;
    else if (bankName.equals("XpB")) bank=40;
    else if (bankName.equals("XpC")) bank=48;
    else if (bankName.equals("XpD")) bank=56;
    
    else if (bankName.indexOf('/')>-1) {
      // bank is in format "[bankMSB]/[LSB]"
      String msb = bankName.substring(0,bankName.indexOf('/'));
      String lsb = bankName.substring(bankName.indexOf('/')+1);
      bank = -1;
      try { 
	bank = (Integer.parseInt(msb)-0x50) * 8 +
	  Integer.parseInt(lsb);
      } catch (NumberFormatException nfe) {
	System.out.println("Couldn't parse bank specfication: " + 
			   bankName);
      }
    }
    
    // for Roland, if the patchNum is greater than 128, then we're
    // increasing the bank LSB by one.
    if (patchNum >= 128 &&
	bankName.indexOf('/')==-1) {
      bank++;
    }

    // The two returned values are the MSB with an offset, and the LSB.
    int[] retvals = new int[2];
    retvals[0] = bank/8 + 0x50;
    retvals[1] = bank%8;

    return retvals;
  }

  public void patchChange( ProgramChangeEvent pce,
			   Receiver midiOut ) {
    try {
      int ch = pce.getChannel();
      Patch patch = pce.getPatch();
      int patchNum = patch.getNumber()-1;

      if (patch.getBank() != null) {

	String bankName = patch.getBank();

	int[] bankSelectValues = _bankSelector(bankName,patchNum);
	
	ShortMessage msg = new ShortMessage();
	msg.setMessage( ShortMessage.CONTROL_CHANGE,
			ch,
			0, bankSelectValues[0]);
	
	if (midiOut != null) 
	  midiOut.send(msg,-1);

	msg = new ShortMessage();
	msg.setMessage( ShortMessage.CONTROL_CHANGE,
			ch,
			0x20, bankSelectValues[1]);
	if (midiOut != null)
	  midiOut.send(msg,-1);
      }

      // we do this for all the messages
      ShortMessage msg = new ShortMessage();
      msg.setMessage( ShortMessage.PROGRAM_CHANGE, 
		      pce.getChannel(), 
		      patchNum%128, 0 );

      if (midiOut != null) 
	midiOut.send(msg, -1);

      Integer volume = patch.getVolume();
      if (volume != null)
      {
	// send Control Change 7 to set channel volume
	msg = new ShortMessage();
	msg.setMessage( ShortMessage.CONTROL_CHANGE,
			pce.getChannel(), 7, volume.intValue() );

	if (midiOut != null)
	  midiOut.send(msg,-1);
      }

    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Program Change: " + pce);
      System.out.println(e);
    } 
  }
  
}
