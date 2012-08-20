package qualm;

import org.junit.Test;
import static org.junit.Assert.*;

public class UtilitiesTest {

  @Test
  public void parseSimpleNoteName() {
    assertEquals(60, Utilities.noteNameToMidi("c4"));
  }

  @Test
  public void parseCaseInsensitive() {
    assertEquals(60, Utilities.noteNameToMidi("C4"));
  }

  @Test
  public void parseAccidentals() {
    assertEquals(61, Utilities.noteNameToMidi("C#4"));
    assertEquals(61, Utilities.noteNameToMidi("Db4"));
  }

  @Test
  public void octaveBreaks() {
    assertEquals(59, Utilities.noteNameToMidi("b3"));
    assertEquals(60, Utilities.noteNameToMidi("c4"));
  }

  @Test
  public void testFlatsWithNoBlackKeys() {
    assertEquals(64, Utilities.noteNameToMidi("Fb4"));
    // Should this be Cb4 or Cb3?
    assertEquals(59, Utilities.noteNameToMidi("Cb3")); 
  }

  @Test
  public void convertToName() {
    assertEquals("C4", Utilities.midiNumberToNoteName(60));
    assertEquals("C#4", Utilities.midiNumberToNoteName(61));
    assertEquals("C3", Utilities.midiNumberToNoteName(48));
  }
}
