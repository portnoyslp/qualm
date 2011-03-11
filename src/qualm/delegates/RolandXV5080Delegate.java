package qualm.delegates;

import qualm.*;

public class RolandXV5080Delegate extends RolandDelegate {
  protected int[] _bankSelector ( String bankName, int patchNum ) {
    int retvals[] = new int[2];
    retvals[0] = retvals[1] = -1;

    // select the proper bank
    if (bankName.indexOf('/')>-1) {
      // bank is in format "[bankMSB]/[bankLSB]"
      String msb = bankName.substring(0,bankName.indexOf('/'));
      String lsb = bankName.substring(bankName.indexOf('/')+1);
      try { 
	retvals[0] = Integer.parseInt(msb);
	retvals[1] = Integer.parseInt(lsb);
	return retvals;
      } catch (NumberFormatException nfe) {
	System.out.println("Couldn't parse bank specfication: " + 
			   bankName);
      }
    }

    // often the rhythm banks are separate.
    boolean rhythmBank = bankName.contains("Rhythm");

    // otherwise, the bank is a named value
    if (bankName.startsWith("User") ||
	bankName.startsWith("Card") || 
	bankName.startsWith("Pr")) {
      retvals[0] = (rhythmBank ? 86 : 87);

      if (bankName.startsWith("User"))
	retvals[1] = 0;
      else { // Presets and data Cards
	// convert last character to get LSB of bank
	char ch = bankName.toUpperCase().charAt(bankName.length()-1);
	int bankOffset = ch - 'A';
	// Card offsets are 32 and up, Presets are 64 and up.
	retvals[1] = bankOffset + (bankName.startsWith("Card") ? 32 : 64);
      }

    // Handle the SR-JV80-xx boards.
    } else if (bankName.startsWith("SR-JV80-")) {
      // rhythm banks are at MSB=88, patches at MSB=89
      retvals[0] = (rhythmBank ? 88 : 89);
      // parse the xx  after SR-JV80-
      int expansionNum = -1;
      try { 
	expansionNum = Integer.parseInt(bankName.substring(8,10));
      } catch (RuntimeException re) {
	System.out.println("Couldn't parse expansion board specification: " + bankName);
      }
      // expansion boards increase the bank LSB by one for high-valued patches.
      retvals[1] = (expansionNum - 1) * 2 + (patchNum >= 128 ? 1 : 0);

    // OK, I give up.
    } else {
      System.out.println("Couldn't parse bank specification: " + bankName);
    }

    return retvals;
  }
}
