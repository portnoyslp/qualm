package qualm;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class Trigger {

  public Trigger () { } 

  public static Trigger createNoteOnTrigger( int ch, int note ) { 
    Trigger t = new Trigger();
    t.type = ShortMessage.NOTE_ON;
    t.channel = ch;
    t.extra1 = note;
    return t;
  }
  public static Trigger createNoteOffTrigger( int ch, int note ) {  
    Trigger t = new Trigger();
    t.type = ShortMessage.NOTE_OFF;
    t.channel = ch;
    t.extra1 = note;
    return t;
  }
  public static Trigger createControlTrigger( int ch, int ctrl, int thresh ) {  
    Trigger t = new Trigger();
    t.type = ShortMessage.CONTROL_CHANGE;
    t.channel = ch;
    t.extra1 = ctrl;
    t.extra2 = thresh;
    return t;
  }
  public static Trigger createClearTrigger( int ch, int duration ) {  
    Trigger t = new Trigger();
    t.type = CLEAR;
    t.channel = ch;
    t.extra1 = duration;
    return t;
  }

  public void setDelay(int d) { delay=d; }
  public int getDelay() { return delay; }

  private String[] names = { "NoteOn", "NoteOff", "Control", "Clear" };
  public String toString() {
    return "trig[" + names[type-1] + "/" + channel + "/" + extra1 + "]";
  }

  public boolean match(MidiMessage m) {
    if (m instanceof ShortMessage) {
      ShortMessage sm = (ShortMessage)m;
      // shortcut test
      if (type != sm.getCommand()) 
	return false;

      // check for bad channel or first data byte
      if (channel != sm.getChannel() || extra1 != sm.getData1())
	return false;

      // other checks.
      if (type == ShortMessage.NOTE_ON || 
	  type == ShortMessage.NOTE_OFF ||
	  type == ShortMessage.CONTROL_CHANGE && extra2 <= sm.getData2())
	return true;
    }
    return false;
  }
  
  public static int CLEAR = -1;

  protected int delay;
  protected int type;
  protected int channel;
  protected int extra1;
  protected int extra2;
  
}
