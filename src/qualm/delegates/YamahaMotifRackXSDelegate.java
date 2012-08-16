package qualm.delegates;

import qualm.*;
import qualm.Patch;
import javax.sound.midi.*;

/**
 * ChangeDelegate for Yamaha MOTIF-RACK XS.  Sends all change commands
 * as SysEx Parameter Changes so that they are targeted to affect a
 * particular Part (as opposed to any Part that's listening on a
 * particular MIDI channel).
 *
 * Note: make sure the module is set to receive sysex messages on
 * "exclusive channel 1".
 */
public class YamahaMotifRackXSDelegate extends ChangeDelegate
{
  public void patchChange( ProgramChangeEvent pce,
                           QReceiver midiOut )
  {
    try {
      MidiCommand sysex;
      byte[] data;

      int channel = pce.getChannel();
      Patch patch = pce.getPatch();

      // patches are numbered 1-128, so subtract 1
      int progNum = patch.getNumber() - 1;

      String bankName = patch.getBank();
      if (bankName != null)
      {
        // translate bank name into bank number
        int msb, lsb = -1;

        if (bankName.equals("GM")) { msb = 0; lsb = 0; }
        else if (bankName.equals("Pre1")) { msb = 63; lsb = 0; }
        else if (bankName.equals("Pre2")) { msb = 63; lsb = 1; }
        else if (bankName.equals("Pre3")) { msb = 63; lsb = 2; }
        else if (bankName.equals("Pre4")) { msb = 63; lsb = 3; }
        else if (bankName.equals("Pre5")) { msb = 63; lsb = 4; }
        else if (bankName.equals("Pre6")) { msb = 63; lsb = 5; }
        else if (bankName.equals("Pre7")) { msb = 63; lsb = 6; }
        else if (bankName.equals("Pre8")) { msb = 63; lsb = 7; }
        else if (bankName.equals("Usr1")) { msb = 63; lsb = 8; }
        else if (bankName.equals("Usr2")) { msb = 63; lsb = 9; }
        else if (bankName.equals("Usr3")) { msb = 63; lsb = 10; }
        else if (bankName.equals("GMDR")) { msb = 127; lsb = 0; }
        else if (bankName.equals("PDR")) { msb = 63; lsb = 32; }
        else if (bankName.equals("UDR")) { msb = 63; lsb = 40; }
        else
          throw new Exception("invalid bank name: " + bankName);

        // send SysEx to select Bank MSB for Part
        data = new byte[]
          { (byte) 0xF0, 0x43, 0x10, 0x7F, 0x03, 0x37, (byte) channel, 1,
            (byte) msb, (byte) 0xF7 };

        sysex = new MidiCommand();
        sysex.setSysex( data );

        if (midiOut != null)
          midiOut.handleMidiCommand(sysex);

        // send SysEx to select Bank LSB for Part, if needed
        if (lsb != -1)
        {
          data[7] = 2;
          data[8] = (byte) lsb;

          sysex = new MidiCommand();
          sysex.setSysex( data );

          if (midiOut != null)
            midiOut.handleMidiCommand(sysex);
        }
      }

      // send SysEx to select Program for Part
      data = new byte[]
        { (byte) 0xF0, 0x43, 0x10, 0x7F, 0x03, 0x37, (byte) channel, 3,
          (byte) progNum, (byte) 0xF7 };

      sysex = new MidiCommand();
      sysex.setSysex( data );

      if (midiOut != null)
        midiOut.handleMidiCommand(sysex);


      if (patch.getVolume() != null)
      {
        // send SysEx to set volume for Part
        data = new byte[]
        { (byte) 0xF0, 0x43, 0x10, 0x7F, 0x03, 0x37, (byte) channel, 0x0E,
          patch.getVolume().byteValue(), (byte) 0xF7 };

        sysex = new MidiCommand();
        sysex.setSysex( data );

        if (midiOut != null)
          midiOut.handleMidiCommand(sysex);
      }

    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Program Change: " + pce);
      System.out.println(e);
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }



  public void noteWindowChange( NoteWindowChangeEvent nwce,
                                QReceiver midiOut )
  {
    try {
      MidiCommand sysex;
      byte[] data;

      int channel = nwce.getChannel();

      if (nwce.getBottomNote() != null)
      {
        // send SysEx to set note window bottom for Part
        data = new byte[]
        { (byte) 0xF0, 0x43, 0x10, 0x7F, 0x03, 0x37, (byte) channel, 8,
          nwce.getBottomNote().byteValue(), (byte) 0xF7 };

        sysex = new MidiCommand();
        sysex.setSysex( data );

        if (midiOut != null)
          midiOut.handleMidiCommand(sysex);
      }

      if (nwce.getTopNote() != null)
      {
        // send SysEx to set note window top for Part
        data = new byte[]
          { (byte) 0xF0, 0x43, 0x10, 0x7F, 0x03, 0x37, (byte) channel, 9,
            nwce.getTopNote().byteValue(), (byte) 0xF7 };

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
