package qualm;

/**
 * New interface for generic MIDI data receivers.  This separates us from javax.sound.midi.* classes.
 * 
 * @author speters
 */
public interface QReceiver {
  
  public void setForwarder(QReceiver qr);
  public QReceiver getForwarder();
  
  public void handleNoteOn(int channel, int noteNumber, int velocity);
  public void handleNoteOff(int channel, int noteNumber);
  public void handleControlChange(int channel, int control, int value);
  public void handleProgramChange(int channel, int program);
  
}
