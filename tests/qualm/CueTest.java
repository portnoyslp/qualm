package qualm;

import org.junit.Test;
import static org.junit.Assert.*;

import qualm.Cue;

public class CueTest {

  @Test
  public void testCueNumber() {
    Cue q = new Cue( "23", "42" );
    assertEquals("23.42", q.getCueNumber());
  }

  @Test
  public void testEquality() {
    Cue q = new Cue( "23", "42" );
    Cue q2 = new Cue ( "23.42" );
    assertEquals(q,q2);
  }

  @Test
  public void testCompareTo() {
    Cue q = new Cue( "23", "42" );
    Cue q2 = new Cue ( "23.42" );
    assertTrue(q.compareTo(q2) == 0);
  }

  @Test
  public void testDefaultMeasure() {
    Cue q = new Cue( "23" );
    assertEquals("23.1", q.getCueNumber());
  }

  @Test
  public void testCompareWithSongSuffixes() {
    Cue q = new Cue( "23" );
    Cue q2 = new Cue( "23a" );
    assertTrue(q.compareTo(q2) < 0);
  }

  @Test
  public void testCompareWithMeasureSuffixes() {
    Cue q = new Cue( "23.42" );
    Cue q2 = new Cue( "23.42A" );
    assertTrue(q.compareTo(q2) < 0);
  }

  @Test
  public void testCompareWithSuffixOnly() {
    Cue q = new Cue( "23.a" );
    Cue q2 = new Cue( "23.1" );
    assertTrue(q.compareTo(q2) < 0);
  }
}
