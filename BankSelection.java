package qualm;

import javax.sound.midi.*;

public class BankSelection {
  public BankSelection() { }

  public static ShortMessage[] RolandBankSelect(int ch, String bankName)  
       throws InvalidMidiDataException {
    int bank = -1;
    // Selecting the right bank
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
	System.out.println("Couldn't parse bank specficiation: " + bankName);
      }
    }

    ShortMessage[] msgs = new ShortMessage[ 2 ];
    msgs[0] = new ShortMessage();
    msgs[1] = new ShortMessage();

    msgs[0].setMessage( ShortMessage.CONTROL_CHANGE,
			ch,
			0, bank/8 + 0x50);
    msgs[1].setMessage( ShortMessage.CONTROL_CHANGE,
			ch,
			0x20, bank%8);
    return msgs;
  }
  
}
