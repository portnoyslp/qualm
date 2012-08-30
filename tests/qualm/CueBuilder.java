package qualm;

import java.util.*;

public class CueBuilder {
  String cueNum = "1.1";
  Collection<Trigger> triggers = new ArrayList<Trigger>();
  Collection<QEvent> events = new ArrayList<QEvent>();
  Collection<EventMapper> eventMaps = new ArrayList<EventMapper>();

  public CueBuilder withCueNumber(String cNum) {
    this.cueNum = cNum;
    return this;
  }

  public CueBuilder addTriggers(Collection<Trigger> triggers) {
    this.triggers.addAll(triggers);
    return this;
  }

  public CueBuilder addTrigger(Trigger trigger) {
    this.triggers.add(trigger);
    return this;
  }

  public CueBuilder addEvents(Collection<QEvent> events) {
    this.events.addAll(events);
    return this;
  }

  public CueBuilder addEvent(QEvent event) {
    this.events.add(event);
    return this;
  }

  public CueBuilder addProgramChangeEvent( int ch, Patch p ) {
    this.events.add( new ProgramChangeEvent( ch, null, p ));
    return this;
  }                     

  public CueBuilder addEventMaps(Collection<EventMapper> eventMaps) {
    this.eventMaps.addAll(eventMaps);
    return this;
  }

  public CueBuilder addEventMap(EventMapper eventMap) {
    this.eventMaps.add(eventMap);
    return this;
  }

  public Cue build() {
    Cue c = new Cue( cueNum );
    c.setTriggers(triggers);
    for (QEvent ev : events) {
      if ( ev.getCue() == null ) 
        ev.setCue( c );
    }
    c.setEvents(events);
    c.setEventMaps(eventMaps);
    return c;
  }
}
