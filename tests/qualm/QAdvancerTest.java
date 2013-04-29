package qualm;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

public class QAdvancerTest {

  QAdvancer qa;

  @Before
  public void setup() {
    QStream qs = new QStreamBuilder()
      .addCue(new CueBuilder()
              .withCueNumber( "1.1" )
              .addProgramChangeEvent( 0, new Patch("P1", 1))
              .build())
      .addCue(new CueBuilder()
              .withCueNumber( "2.1" )
              .addProgramChangeEvent( 0, new Patch("P2", 2))
              .build())
      .build();
    QData qd = new QDataBuilder()
      .addMidiChannel( 0, null, "Ch1")
      .addStream(qs)
      .build();
    qa = new QAdvancer( qs, qd );
  }

  @Test
  public void advancePatchWithNoCurrent() {
    Collection<QEvent> evs = qa.advancePatch ( );
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      assertEquals( 0, qe.getChannel() );
      assertEquals( "1.1", qe.getCue().getCueNumber() );
    }
  }

  @Test
  public void advancePatchWithCurrent() {
    Collection<QEvent> evs = qa.switchToMeasure("1.1");
    evs = qa.advancePatch();
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      assertEquals( 0, qe.getChannel() );
      assertEquals( "2.1", qe.getCue().getCueNumber() );
    }
  }

  @Test
  public void switchToMeasure() {
    Collection<QEvent> evs = qa.switchToMeasure( "2.1" );
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      assertEquals( 0, qe.getChannel() );
      assertEquals( "2.1", qe.getCue().getCueNumber() );
    }
  }

  @Test
  public void reversePatch() {
    Collection<QEvent> evs = qa.switchToMeasure( "2.1" );
    evs = qa.reversePatch();
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      assertEquals( 0, qe.getChannel() );
      assertEquals( "2.1", qe.getCue().getCueNumber() );
    }
  }

  /* exploit a specific bug, when a channel is not used in the first
   * cue, but we reverse to the the first cue. */
  @Test
  public void reverseWithEmptyChannel() {
    QStream qs = new QStreamBuilder()
      .addCue(new CueBuilder()
              .withCueNumber( "1.1" )
              .addProgramChangeEvent( 0, new Patch("P1", 1))
              .build())
      .addCue(new CueBuilder()
              .withCueNumber( "2.1" )
              .addProgramChangeEvent( 0, new Patch("P2", 2))
              .addProgramChangeEvent( 1, new Patch("P1", 2))
              .build())
      .build();
    QData qd = new QDataBuilder()
      .addMidiChannel( 0, null, "Ch1")
      .addMidiChannel( 1, null, "Ch2")
      .addStream(qs)
      .build();
    QAdvancer qa2 = new QAdvancer( qs, qd );

    Collection<QEvent> evs = qa.switchToMeasure( "2.1" );
    evs = qa.reversePatch();
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      /* Only reverse to the one channel we care about */
      assertEquals( 0, qe.getChannel() );
    }
  }

}
