package qualm;

import org.junit.*;
import static org.junit.Assert.*;

import javax.sound.midi.*;

public class MidiCommandTest {

  @Test
  public void commandEquality() {
    MidiCommand playMiddleC = new MidiCommand( 0, MidiCommand.NOTE_ON, 60, 100 );
    MidiCommand compareTo = new MidiCommand( 0, MidiCommand.NOTE_ON, 60, 100 );
    assertEquals( playMiddleC, compareTo );
  }

}
