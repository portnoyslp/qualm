package qualm;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class EventTemplate {

  public EventTemplate () { } 

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
      /* XXX Should parse name into integer */
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
