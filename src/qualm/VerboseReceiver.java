package qualm;

import javax.sound.midi.*;
import java.util.*;

// Verbose Receiver -- adds debugging to output messages.

public class VerboseReceiver implements Receiver {

  Receiver midiOut;
  boolean debugMIDI;

  public VerboseReceiver( Receiver out ) {
    midiOut = out;
  }

  public Receiver getMidiOut() { return midiOut; }

  public boolean getDebugMIDI() { return debugMIDI; }
  public void setDebugMIDI(boolean db) { debugMIDI = db; }

  public void send(MidiMessage midiMessage, long l) {
    if (debugMIDI)
      System.out.println( "->" + MidiMessageParser.messageToString(midiMessage) );
    midiOut.send(midiMessage,l);
  }

  public void close() {
    midiOut.close();
  }

} // VerboseReceiver
