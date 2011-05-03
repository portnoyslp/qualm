package qualm;

// Verbose Receiver -- adds debugging to output messages.

public class VerboseReceiver implements QReceiver {

  QReceiver midiOut;
  boolean debugMIDI;

  public VerboseReceiver( QReceiver out ) {
    setForwarder( out );
  }

  public void setForwarder( QReceiver out ) {
    midiOut = out;
  }
  public QReceiver getForwarder() { return midiOut; }

  public boolean getDebugMIDI() { return debugMIDI; }
  public void setDebugMIDI(boolean db) { debugMIDI = db; }

  public void send(MidiCommand midiCommand) {
    if (debugMIDI)
      System.out.println( "->" + MidiMessageParser.messageToString(midiMessage) );
    if (midiOut != null)
      midiOut.send(midiMessage,l);
  }

  public void close() {
    if (midiOut != null) midiOut.close();
  }

} // VerboseReceiver
