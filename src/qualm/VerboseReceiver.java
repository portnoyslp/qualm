package qualm;

// Verbose Receiver -- adds debugging to output messages.

public class VerboseReceiver extends AbstractQReceiver {

  boolean debugMIDI;

  public VerboseReceiver( QReceiver out ) {
    setTarget( out );
  }

  public boolean getDebugMIDI() { return debugMIDI; }
  public void setDebugMIDI(boolean db) { debugMIDI = db; }

  public void handleMidiCommand(MidiCommand midiCommand) {
    if (debugMIDI)
      System.out.println( "->" + midiCommand );
    if (getTarget() != null)
      getTarget().handleMidiCommand(midiCommand);
  }

} // VerboseReceiver
