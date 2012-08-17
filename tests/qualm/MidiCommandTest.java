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

  @Test
  public void hexData() {
    byte[] data = new byte[] { (byte)0xF0, 8, 60, 125, (byte) 0xF7 };
    MidiCommand sysex = new MidiCommand();
    sysex.setSysex( data );
    assertEquals( "F0083C7DF7", sysex.hexData() );
  }

}
