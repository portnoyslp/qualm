package qualm;

public class Trigger {

  // Useful constants
  public static final boolean REVERSE = true;
  public static final boolean FORWARD = false;
  
  public Trigger (EventTemplate et, boolean rev) { 
    template = et; 
    setReverse(rev);
  } 
  public Trigger (EventTemplate et) { 
    template = et; 
  } 

  // delay is in ms
  public void setDelay(int d) { delay=d; }
  public int getDelay() { return delay; }

  public void setReverse(boolean rev) { reverse = rev; }
  public boolean getReverse() { return reverse; }

  public EventTemplate getTemplate() { return template; }

  public String toString() {
    return "trig[" + template + (getReverse()?" rev":"") + (getDelay()>0?" dly"+getDelay():"") + "]";
  }

  public boolean match(MidiCommand cmd) {
    return template.match(cmd);
  }
  
  protected int delay;
  protected boolean reverse = false;
  protected EventTemplate template;
  
}
