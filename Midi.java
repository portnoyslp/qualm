package qualm;

import javax.sound.midi.*;
import javax.sound.midi.Receiver;
import javax.sound.midi.MidiMessage;

public class Midi implements Receiver {

  public Midi(Transmitter t, Receiver r) {
    transmitter = t;
    receiver = r;
  }

  // Implementation of javax.sound.midi.Receiver

  public void close() { }

  public void send(MidiMessage midiMessage, long l) {
    // OK, we've received a message.
  }
  

  Transmitter transmitter;
  Receiver receiver;
}


