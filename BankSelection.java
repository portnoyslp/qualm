package qualm;

import javax.sound.midi.*;

public class BankSelection {
  public BankSelection() { }

  public static ShortMessage[] RolandBankSelect(int ch, int bank)  
       throws InvalidMidiDataException {
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
