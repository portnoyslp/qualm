package qualm;

import java.util.ArrayList;
import java.util.List;

public class EventMapper {

  public EventMapper () { } 

  public void setFromTemplate(EventTemplate f) { mapFrom = f; }
  public void setToTemplates(List<EventTemplate> templateList) { mapToList = templateList; }
  public void addToTemplate(EventTemplate t) { 
    mapToList.add(t);
  }

  public EventTemplate getFromTemplate() { return mapFrom; }
  public List<EventTemplate> getToTemplateList() { return mapToList; }

  public boolean match(MidiCommand m) { return mapFrom.match(m); }

  public MidiCommand[] mapEvent(MidiCommand cmd) {
    if (!match(cmd)) 
      return new MidiCommand[0];

    // OK, we have a match.  Generate a series of new MIDI messages for the output.
    MidiCommand out[] = new MidiCommand[ mapToList.size() ];

    int i=0;
    for (EventTemplate mapTo : mapToList) {
      out[i] = new MidiCommand(
            (mapTo.channel != EventTemplate.DONT_CARE ? mapTo.channel : cmd.getChannel()), 
            mapTo.type,
            (mapTo.extra1Min != EventTemplate.DONT_CARE ? mapTo.extra1Min : cmd.getData1()),
            cmd.getData2() // data taken from input
            );
      i++;
    }

    return out;
  }

  public String toString() { return "EM[" + mapFrom + "=>" + mapToList + "]"; }

  
  EventTemplate mapFrom;
  List<EventTemplate> mapToList = new ArrayList<EventTemplate>();
  
}
