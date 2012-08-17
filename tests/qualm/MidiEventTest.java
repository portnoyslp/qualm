package qualm;

import org.junit.*;
import static org.junit.Assert.*;

import javax.sound.midi.*;

public class MidiEventTest {

  @Test
  public void noteOnEventFromTemplate() {
    EventTemplate et = EventTemplate.createNoteOffEventTemplate( 0, "c4" );
    MidiEvent me = new MidiEvent(et);
    assertEquals(new MidiCommand(0,MidiCommand.NOTE_OFF,60,0), me.getMidiCommand() );
  }

  @Test
  public void controlEventFromTemplate() {
    EventTemplate et = EventTemplate.createControlEventTemplate( 0, "volume", "30" );
    MidiEvent me = new MidiEvent(et);
    assertEquals(new MidiCommand(0,MidiCommand.CONTROL_CHANGE,7,30), me.getMidiCommand());
  }

}
