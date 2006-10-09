package qualm;

import javax.sound.midi.*;

public class RolandDelegate extends ChangeDelegate {
  public void patchChange( ProgramChangeEvent pce,
			   Receiver midiOut ) {
    try {
      int ch = pce.getChannel();
      Patch patch = pce.getPatch();
      int patchNum = patch.getNumber();

      if (patch.getBank() != null) {

	String bankName = patch.getBank();
	
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
	    System.out.println("Couldn't parse bank specficiation: " + 
			       bankName);
	  }
	}
	
	// for Roland, if the patchNum is greater than 128, than we're
	// increasing the bank LSB by one.
	if (patchNum > 128 &&
	    bankName.indexOf('/')==-1) {
	  bank++;
	}
	
	ShortMessage msg = new ShortMessage();
	msg.setMessage( ShortMessage.CONTROL_CHANGE,
			ch,
			0, bank/8 + 0x50);
	
	if (midiOut != null) 
	  midiOut.send(msg,-1);

	msg = new ShortMessage();
	msg.setMessage( ShortMessage.CONTROL_CHANGE,
			ch,
			0x20, bank%8);
	if (midiOut != null)
	  midiOut.send(msg,-1);
      }

      // we do this for all the messages
      ShortMessage msg = new ShortMessage();
      msg.setMessage( ShortMessage.PROGRAM_CHANGE, 
		      pce.getChannel(), 
		      patch.getNumber()%128, 0 );

      if (midiOut != null) 
	midiOut.send(msg, -1);
      
    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Program Change: " + pce);
      System.out.println(e);
    }
  }
  
}