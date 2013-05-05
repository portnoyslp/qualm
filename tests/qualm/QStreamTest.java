package qualm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class QStreamTest {

  @Test
  public void titleEquality() {
    QStream qs1 = new QStreamBuilder().withTitle("S1").build();
    QStream qs2 = new QStreamBuilder().withTitle("S1").build();
    QStream qs3 = new QStreamBuilder().withTitle("S2").build();
    assertEquals(qs1, qs2);
    assertFalse(qs1.equals(qs3));
  }

  @Test
  public void cuesEquality() {
    Cue.Builder cb = new Cue.Builder().setCueNumber("1.1");
    Cue.Builder cb2 = new Cue.Builder().setCueNumber("1.2");
    QStream qs1 = new QStreamBuilder().addCue(cb.build()).build();
    QStream qs2 = new QStreamBuilder().addCue(cb.build()).build();
    QStream qs3 = new QStreamBuilder().addCue(cb2.build()).build();
    assertEquals(qs1, qs2);
    assertFalse(qs1.equals(qs3));
  }
}
