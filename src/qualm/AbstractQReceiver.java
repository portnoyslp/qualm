package qualm;

/**
 * Base class for MIDI-receiving classes. 
 */
public abstract class AbstractQReceiver implements QReceiver {

  private QReceiver target;
  
  public QReceiver getTarget() { return target; }
  public void setTarget(QReceiver t) {
    if (t == null) 
      throw new IllegalArgumentException("Setting target to null; this shouldn't happen");
    this.target = t; 
  }
}
