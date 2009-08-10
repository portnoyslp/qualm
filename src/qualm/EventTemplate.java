package qualm;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class EventTemplate {

  public EventTemplate () { } 

  public EventTemplate (EventTemplate et) {
      this.type = et.type;
      this.channel = et.channel;
      this.extra1Min = et.extra1Min;
      this.extra1Max = et.extra1Max;
      this.extra2Min = et.extra2Min;
      this.extra2Max = et.extra2Max;
  }

  public static EventTemplate createNoteOnEventTemplate( int ch, String noteRange ) { 
    EventTemplate t = new EventTemplate();
    t.type = ShortMessage.NOTE_ON;
    t.channel = ch;
    t._setNoteRange(noteRange);
    return t;
  }
  public static EventTemplate createNoteOffEventTemplate( int ch, String noteRange ) {  
    EventTemplate t = new EventTemplate();
    t.type = ShortMessage.NOTE_OFF;
    t.channel = ch;
    t._setNoteRange(noteRange);
    return t;
  }

  public static EventTemplate createControlEventTemplate( int ch, String ctrl, 
							  String thresh ) {
    EventTemplate t = new EventTemplate();
    t.type = ShortMessage.CONTROL_CHANGE;
    t.channel = ch;

    int extra1;
    try {
      extra1 = Integer.parseInt(ctrl);
    } catch (NumberFormatException nfe) {
      if (ctrl.equals("modulation"))
	extra1 = 1;
      else if (ctrl.equals("breath")) 
	extra1 = 2;
      else if (ctrl.equals("foot"))
	extra1 = 4;
      else if (ctrl.equals("volume"))
	extra1 = 7;
      else if (ctrl.equals("balance"))
	extra1 = 8;
      else if (ctrl.equals("pan"))
	extra1 = 10;
      else if (ctrl.equals("expression"))
	extra1 = 11;
      else if (ctrl.equals("effect 1"))
	extra1 = 12;
      else if (ctrl.equals("effect 2"))
	extra1 = 13;
      else if (ctrl.equals("damper"))
	extra1 = 64;
      else if (ctrl.equals("sustain"))
	extra1 = 64;
      else if (ctrl.equals("portamento"))
	extra1 = 65;
      else if (ctrl.equals("sustenuto"))
	extra1 = 66;
      else if (ctrl.equals("soft"))
	extra1 = 67;
      else if (ctrl.equals("legato"))
	extra1 = 68;
      else 
	throw new IllegalArgumentException("Cannot parse control change type '" +
					   ctrl + "'");
    }
    // no range allowed for control events
    t.extra1Min = extra1;
    t.extra1Max = extra1;

    if (thresh != null) 
      t.extra2Min = Integer.parseInt(thresh);
    return t;
  }

  private void _setNoteRange(String rangeString) {
    // for now, just treat it like a simple value
    int x = Utilities.noteNameToMidi(rangeString);
    extra1Min = x;
    extra1Max = x;
  }

  private String[] names = { "NoteOn", "NoteOff", "Control", "Clear" };
  public String toString() {
    String name = null;
    if (type == ShortMessage.NOTE_ON) { name = "NoteOn"; }
    if (type == ShortMessage.NOTE_OFF) { name = "NoteOff"; } 
    if (type == ShortMessage.CONTROL_CHANGE) { name = "Control"; } 
    return "event[" + name + "/" + channel + "/" + extra1Min + "-" + extra1Max + "]";
  }

  public boolean match(MidiMessage m) {
    if (m instanceof ShortMessage) {
      ShortMessage sm = (ShortMessage)m;
      // shortcut test
      if (type != sm.getCommand()) 
	return false;

      // check for bad channel or first data byte
      if (channel != sm.getChannel() || extra1Min != sm.getData1())
	return false;

      // other checks.
      if (type == ShortMessage.NOTE_ON || 
	  type == ShortMessage.NOTE_OFF ||
	  type == ShortMessage.CONTROL_CHANGE && extra2Min <= sm.getData2())
	return true;
    }
    return false;
  }

  protected int type;
  protected int channel;
  protected int extra1Min;
  protected int extra1Max;
  protected int extra2Min;
  protected int extra2Max;
  
}
