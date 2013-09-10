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
  private int sysexDelayMillis = 0;
  private long nextSysexTime = -1; // earliest millis at which next sysex may be sent

  private static final int minimalSleepMsec = 1;

  public JavaMidiReceiver(Transmitter trans, Receiver rec) {
    midiIn = trans;
    midiOut = rec;
  }

  /* Sets a minimum time (in milliseconds) which must elapse between
   * successive SysEx command transmissions.  Useful for coping with
   * lower-quality USB MIDI interfaces.
   *
   * Caution: this option introduces a BLOCKING DELAY into Qualm's
   * MIDI event handling!
   */
  public void setSysexDelay(int millis) {
    this.sysexDelayMillis = millis;
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

        if (sysexDelayMillis > 0) {
          // delay this sysex if not enough time has elapsed since the
          // previous one
          long delayMillis = nextSysexTime - Clock.asMillis();
          if (delayMillis > 0) {
            Qualm.LOG.finer("Delaying "+delayMillis+"ms before sending SysEx");
            try {
              while (Clock.asMillis() < nextSysexTime) {
                Thread.sleep(minimalSleepMsec);
              }
            } catch (InterruptedException e) {}
          }
          // note earliest time at which a subsequent SysEx may be sent
          nextSysexTime = Clock.asMillis() + sysexDelayMillis;
        }

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
