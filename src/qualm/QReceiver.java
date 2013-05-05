package qualm;

/**
 * Interface for generic MIDI receivers in Qualm.
 */

public interface QReceiver {
  public void handleMidiCommand(MidiCommand midi);
}
