package qualm;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

/**
 * Adaptor for translating between javax.sound.midi messages and Qualm messaging.
 */

public class JavaMidiReceiver extends AbstractQReceiver implements QReceiver, Receiver {
  public Transmitter midiIn;
  public Receiver midiOut;

  public JavaMidiReceiver(Transmitter trans, Receiver rec) {
    midiIn = trans;
    midiOut = rec;
  }
  
  /* Receives the given MidiCommand from Qualm and sends it out through the MIDI interface.
   * @see qualm.BasicReceiver#handleMidiCommand(qualm.MidiCommand)
   */
  public void handleMidiCommand(MidiCommand mc) {
    try {
      MidiMessage msg;

      if (mc.getType() == MidiCommand.SYSEX) {
        byte[] data = mc.getData();
        msg = new SysexMessage();
        ((SysexMessage)msg).setMessage(data,data.length);
        
      } else {
        msg = new ShortMessage();
        ((ShortMessage)msg).setMessage(mc.getType(),mc.getChannel(),mc.getData1(),mc.getData2());
        
      }
      
      midiOut.send(msg, -1);
      
    } catch (InvalidMidiDataException ume) {
      // ignore MIDI problems.
    }
  }

  public void close() {
    midiIn.close();
    midiOut.close();
  }

  /* Receives a MidiMessage from the internal Transmitter, and sends it out through the forwarding target.
   * @see javax.sound.midi.Receiver#send(javax.sound.midi.MidiMessage, long)
   */
  public void send(MidiMessage message, long timeStamp) {
    if (message instanceof ShortMessage) {
      ShortMessage sm = (ShortMessage)message;
      MidiCommand mc = new MidiCommand(sm.getChannel(),sm.getCommand(),sm.getData1(),sm.getData2());
      getTarget().handleMidiCommand(mc);
    }
    if (message instanceof SysexMessage) {
      SysexMessage sysex = (SysexMessage)message;
      MidiCommand mc = new MidiCommand();
      /* The sysex we receive should probably keep track of the status byte as well */
      byte[] data = new byte[sysex.getLength()];
      data[0] = (byte) sysex.getStatus();
      System.arraycopy(sysex.getData(), 0, data, 1, sysex.getLength() - 1);
      mc.setSysex(data);
      getTarget().handleMidiCommand(mc);
    }
  }


}
