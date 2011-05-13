package qualm;

/**
 * New interface for generic MIDI data receivers.  This separates us from javax.sound.midi.* classes.
 * 
 * @author speters
 */
public interface QReceiver {
  
  public void handleMidiCommand(MidiCommand midi);
  
}
