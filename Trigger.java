package qualm;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class Trigger {

  public Trigger (EventTemplate et) { template = et; } 

  public void setDelay(int d) { delay=d; }
  public int getDelay() { return delay; }

  public EventTemplate getTemplate() { return template; }

  public String toString() {
    return "trig[" + template + "]";
  }

  public boolean match(MidiMessage m) {
    return template.match(m);
  }
  
  protected int delay;
  protected EventTemplate template;
  
}
