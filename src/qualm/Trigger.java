package qualm;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class Trigger {

  public Trigger (EventTemplate et, boolean rev) { 
    template = et; 
    setReverse(rev);
  } 
  public Trigger (EventTemplate et) { 
    template = et; 
  } 

  public void setDelay(int d) { delay=d; }
  public int getDelay() { return delay; }

  public void setReverse(boolean rev) { reverse = rev; }
  public boolean getReverse() { return reverse; }

  public EventTemplate getTemplate() { return template; }

  public String toString() {
    return "trig[" + template + (getReverse()?"rev":"") + "]";
  }

  public boolean match(MidiMessage m) {
    return template.match(m);
  }
  
  protected int delay;
  protected boolean reverse = false;
  protected EventTemplate template;
  
}
