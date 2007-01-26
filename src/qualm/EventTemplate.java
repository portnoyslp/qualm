package qualm;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class EventTemplate {

  public EventTemplate () { } 

  public EventTemplate (EventTemplate et) {
      this.type = et.type;
      this.channel = et.channel;
      this.extra1 = et.extra1;
      this.extra2 = et.extra2;
  }

  public static EventTemplate createNoteOnEventTemplate( int ch, int note ) { 
    EventTemplate t = new EventTemplate();
    t.type = ShortMessage.NOTE_ON;
    t.channel = ch;
    t.extra1 = note;
    return t;
  }
  public static EventTemplate createNoteOffEventTemplate( int ch, int note ) {  
    EventTemplate t = new EventTemplate();
    t.type = ShortMessage.NOTE_OFF;
    t.channel = ch;
    t.extra1 = note;
    return t;
  }

  public static EventTemplate createControlEventTemplate( int ch, String ctrl, 
							  String thresh ) {
    EventTemplate t = new EventTemplate();
    t.type = ShortMessage.CONTROL_CHANGE;
    t.channel = ch;

    try {
      t.extra1 = Integer.parseInt(ctrl);
    } catch (NumberFormatException nfe) {
      if (ctrl.equals("modulation"))
	t.extra1 = 1;
      else if (ctrl.equals("breath")) 
	t.extra1 = 2;
      else if (ctrl.equals("foot"))
	t.extra1 = 4;
      else if (ctrl.equals("volume"))
	t.extra1 = 7;
      else if (ctrl.equals("balance"))
	t.extra1 = 8;
      else if (ctrl.equals("pan"))
	t.extra1 = 10;
      else if (ctrl.equals("expression"))
	t.extra1 = 11;
      else if (ctrl.equals("effect 1"))
	t.extra1 = 12;
      else if (ctrl.equals("effect 2"))
	t.extra1 = 13;
      else if (ctrl.equals("damper"))
	t.extra1 = 64;
      else if (ctrl.equals("sustain"))
	t.extra1 = 64;
      else if (ctrl.equals("portamento"))
	t.extra1 = 65;
      else if (ctrl.equals("sustenuto"))
	t.extra1 = 66;
      else if (ctrl.equals("soft"))
	t.extra1 = 67;
      else if (ctrl.equals("legato"))
	t.extra1 = 68;
      else 
	throw new IllegalArgumentException("Cannot parse control change type '" +
					   ctrl + "'");
    }

    if (thresh != null) 
      t.extra2 = Integer.parseInt(thresh);
    return t;
  }

  private String[] names = { "NoteOn", "NoteOff", "Control", "Clear" };
  public String toString() {
    String name = null;
    if (type == ShortMessage.NOTE_ON) { name = "NoteOn"; }
    if (type == ShortMessage.NOTE_OFF) { name = "NoteOff"; } 
    if (type == ShortMessage.CONTROL_CHANGE) { name = "Control"; } 
    return "event[" + name + "/" + channel + "/" + extra1 + "]";
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

  protected int type;
  protected int channel;
  protected int extra1;
  protected int extra2;
  
}
