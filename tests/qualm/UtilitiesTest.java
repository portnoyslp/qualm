package qualm;

import org.junit.Test;
import static org.junit.Assert.*;

public class UtilitiesTest {

  @Test
  public void parseSimpleNoteName() {
    assertEquals(60, Utilities.noteNameToMidi("c4"));
  }

  public void parseCaseInsensitive() {
    assertEquals(60, Utilities.noteNameToMidi("C4"));
  }

  public void parseAccidentals() {
    assertEquals(61, Utilities.noteNameToMidi("C#4"));
    assertEquals(61, Utilities.noteNameToMidi("Db4"));
  }

  public void octaveBreaks() {
    assertEquals(59, Utilities.noteNameToMidi("b3"));
    assertEquals(60, Utilities.noteNameToMidi("c4"));
  }

  public void convertToName() {
    assertEquals("C4", Utilities.midiNumberToNoteName(60));
    assertEquals("C#4", Utilities.midiNumberToNoteName(61));
    assertEquals("C3", Utilities.midiNumberToNoteName(48));
  }
}
