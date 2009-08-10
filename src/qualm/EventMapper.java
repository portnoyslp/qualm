package qualm;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class EventMapper {

  public EventMapper () { } 

  public void setFromTemplate(EventTemplate f) { mapFrom = f; }
  public void setToTemplate(EventTemplate t) { mapTo = t; }

  public boolean match(MidiMessage m) { return mapFrom.match(m); }

  public MidiMessage mapEvent(MidiMessage m) {
    if (!match(m)) 
      return null; 

    // OK, we have a match.  Generate a new MIDI message for the output.
    ShortMessage out = new ShortMessage();
    ShortMessage sm = (ShortMessage)m;

    try {
      out.setMessage( mapTo.type,
		      (mapTo.channel != EventTemplate.DONT_CARE ? 
		       mapTo.channel : sm.getChannel()),
		      (mapTo.extra1Min != EventTemplate.DONT_CARE ? 
		       mapTo.extra1Min : sm.getData1()),
		      sm.getData2() // data taken from input
		      );
    } catch (InvalidMidiDataException imde) {
      System.out.println("Unable to create mapped event for " + 
			 MidiMessageParser.messageToString(m));
      return null;
    }

    return out;
  }

  public String toString() { return "EM[" + mapFrom + "," + mapTo + "]"; }

  
  EventTemplate mapFrom;
  EventTemplate mapTo;
  
}
