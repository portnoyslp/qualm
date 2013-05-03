package qualm;

import org.junit.*;
import static org.junit.Assert.*;

public class MidiEventTest {

  @Test
  public void noteOnEventFromTemplate() {
    EventTemplate et = EventTemplate.noteOff( 0, "c4" );
    MidiEvent me = new MidiEvent(et);
    assertEquals(new MidiCommand(0,MidiCommand.NOTE_OFF,60,0), me.getMidiCommand() );
  }

  @Test
  public void controlEventFromTemplate() {
    EventTemplate et = EventTemplate.control( 0, "volume", "30" );
    MidiEvent me = new MidiEvent(et);
    assertEquals(new MidiCommand(0,MidiCommand.CONTROL_CHANGE,7,30), me.getMidiCommand());
  }

}
