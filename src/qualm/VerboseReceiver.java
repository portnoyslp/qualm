package qualm;

// Verbose Receiver -- adds debugging to output messages.

public class VerboseReceiver extends AbstractQReceiver {

  static boolean debugMIDI;

  public VerboseReceiver( QReceiver out ) {
    setTarget( out );
  }

  public static boolean getDebugMIDI() { return debugMIDI; }
  public static void setDebugMIDI(boolean db) { debugMIDI = db; }

  public void handleMidiCommand(MidiCommand midiCommand) {
    if (debugMIDI)
      Qualm.LOG.info( "->" + midiCommand );
    if (getTarget() != null)
      getTarget().handleMidiCommand(midiCommand);
  }

} // VerboseReceiver
