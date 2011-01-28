package qualm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class EventMapper {

  public EventMapper () { } 

  public void setFromTemplate(EventTemplate f) { mapFrom = f; }
  public void setToTemplates(List templateList) { mapToList = templateList; }
  public void addToTemplate(EventTemplate t) { 
    if (mapToList == null) {
      mapToList = new ArrayList();
    }
    mapToList.add(t);
  }

  public EventTemplate getFromTemplate() { return mapFrom; }
  public List getToTemplateList() { return mapToList; }

  public boolean match(MidiMessage m) { return mapFrom.match(m); }

  public MidiMessage[] mapEvent(MidiMessage m) {
    if (!match(m)) 
      return null;

    // OK, we have a match.  Generate a series of new MIDI messages for the output.
    ShortMessage sm = (ShortMessage)m;
    ShortMessage[] out = new ShortMessage[ mapToList.size() ];

    Iterator iter = mapToList.iterator();
    int i=0;
    while (iter.hasNext()) {
      EventTemplate mapTo = (EventTemplate) iter.next();
      out[i] = new ShortMessage();
      try {
	out[i].setMessage( mapTo.type,
			   (mapTo.channel != EventTemplate.DONT_CARE ? 
			    mapTo.channel : sm.getChannel()),
			   (mapTo.extra1Min != EventTemplate.DONT_CARE ? 
			    mapTo.extra1Min : sm.getData1()),
			   sm.getData2() // data taken from input
			   );
      } catch (InvalidMidiDataException imde) {
	System.out.println("Unable to create mapped event for " + 
			   MidiMessageParser.messageToString(m));
	out[i] = null;
      }
      i++;
    }

    return out;
  }

  public String toString() { return "EM[" + mapFrom + "=>" + mapToList + "]"; }

  
  EventTemplate mapFrom;
  List mapToList;
  
}
