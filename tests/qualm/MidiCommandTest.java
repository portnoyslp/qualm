package qualm;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

public class MidiCommandTest {

  @Test
  public void commandEquality() {
    MidiCommand playMiddleC = new MidiCommand( 0, MidiCommand.NOTE_ON, 60, 100 );
    MidiCommand compareTo = new MidiCommand( 0, MidiCommand.NOTE_ON, 60, 100 );
    assertEquals( playMiddleC, compareTo );
  }

  @Test
  public void trivialEquality() {
    MidiCommand noteOff = new MidiCommand( 1, MidiCommand.NOTE_OFF, 60 );
    assertEquals(noteOff, noteOff);
    Assert.assertFalse(noteOff.equals(null));
  }

  @Test
  public void hashCodeHandling() {
    HashMap<MidiCommand, String> map = new HashMap<MidiCommand, String>();
    MidiCommand test = new MidiCommand(0, MidiCommand.NOTE_OFF, 60);
    map.put(test, "1");
    assertEquals("1", map.get(test));
  }

  @Test
  public void hexData() {
    byte[] data = new byte[] { (byte)0xF0, 8, 60, 125, (byte) 0xF7 };
    MidiCommand sysex = new MidiCommand();
    sysex.setSysex( data );
    assertEquals( "F0083C7DF7", sysex.hexData() );
    assertEquals( "[DATA: F0083C7DF7]", sysex.toString());
  }

  @Test
  public void setParams() {
    MidiCommand cmd = new MidiCommand();
    cmd.setParams( 1, MidiCommand.NOTE_OFF, (byte)72 );
    assertEquals("[NoteOff chan:2 d1:72 d2:0]", cmd.toString());
  }

  @Test
  public void printing() {
    MidiCommand cc = new MidiCommand(1, MidiCommand.CONTROL_CHANGE, 12, 34);
    MidiCommand pb = new MidiCommand(2, MidiCommand.PITCH_BEND, 23, 45);
    assertEquals("[ControlChange chan:2 d1:12 d2:34]", cc.toString());
    assertEquals("[PitchBend chan:3 d1:23 d2:45]", pb.toString());
  }

}
