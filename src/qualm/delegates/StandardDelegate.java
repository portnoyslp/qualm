package qualm.delegates;

import qualm.*;
import qualm.Patch;
import javax.sound.midi.*;

/* StandardDelegate -- uses the standard MIDI commands for patch
 * changes, volume settings.  Doesn't do bank changes. */

public class StandardDelegate extends ChangeDelegate {
  public void patchChange( ProgramChangeEvent pce,
			   Receiver midiOut ) {
    try {
      int ch = pce.getChannel();
      Patch patch = pce.getPatch();
      int patchNum = patch.getNumber()-1;

      if (patch.getBank() != null) {
	System.out.println("Unable to switch to a new bank using the standard delegate.");
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
