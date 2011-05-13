package qualm.delegates;

import qualm.*;
import qualm.Patch;

/* StandardDelegate -- uses the standard MIDI commands for patch
 * changes, volume settings.  Doesn't do bank changes. */

public class StandardDelegate extends ChangeDelegate {
  public void patchChange( ProgramChangeEvent pce,
			   QReceiver midiOut ) {
    Patch patch = pce.getPatch();
    int patchNum = patch.getNumber()-1;

    if (patch.getBank() != null) {
      System.out.println("Unable to switch to a new bank using the standard delegate.");
    }

    // we do this for all the messages
    MidiCommand msg = new MidiCommand();
    msg.setParams( pce.getChannel(),
                   MidiCommand.PROGRAM_CHANGE, 
                   (byte)(patchNum%128), (byte)0 );
    
    if (midiOut != null) 
      midiOut.handleMidiCommand(msg);

    Integer volume = patch.getVolume();
    if (volume != null) {
      // send Control Change 7 to set channel volume
      msg = new MidiCommand();
      msg.setParams( pce.getChannel(),
                     MidiCommand.CONTROL_CHANGE,
		     (byte)7, volume.byteValue() );

      if (midiOut != null)
        midiOut.handleMidiCommand(msg);
    } 
  }
  
}
