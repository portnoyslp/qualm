package qualm;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

public class QAdvancerTest {

  QData qd;
  QAdvancer qa;

  @Before
  public void setup() {
    QData qd = new QData();
    // Build up the data for use 
    QStream qs = new QStream();
    Cue c = new Cue("1.1");
    ArrayList<QEvent> evList = new ArrayList<QEvent>();
    evList.add(new ProgramChangeEvent( 0, c, new Patch( "P1", 1)));
    c.setEvents(evList);
    qs.addCue(c);
    
    c = new Cue("2.1");
    evList = new ArrayList<QEvent>();
    evList.add(new ProgramChangeEvent( 0, c, new Patch( "P2", 2)));
    c.setEvents(evList);
    qs.addCue(c);
    
    qd.addMidiChannel( 0, null, "Ch1" );
    qd.addCueStream( qs );
    qd.prepareCueStreams();

    qa = new QAdvancer( qs, qd );
  }

  @Test
  public void testAdvancePatchWithNoCurrent() {
    Collection<QEvent> evs = qa.advancePatch ( );
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      assertEquals( 0, qe.getChannel() );
      assertEquals( "1.1", qe.getCue().getCueNumber() );
    }
  }

  @Test
  public void testAdvancePatchWithCurrent() {
    Collection<QEvent> evs = qa.switchToMeasure("1.1");
    evs = qa.advancePatch();
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      assertEquals( 0, qe.getChannel() );
      assertEquals( "2.1", qe.getCue().getCueNumber() );
    }
  }

  @Test
  public void testSwitchToMeasure() {
    Collection<QEvent> evs = qa.switchToMeasure( "2.1" );
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      assertEquals( 0, qe.getChannel() );
      assertEquals( "2.1", qe.getCue().getCueNumber() );
    }
  }

  @Test
  public void testReversePatch() {
    Collection<QEvent> evs = qa.switchToMeasure( "2.1" );
    evs = qa.reversePatch();
    assertEquals(1, evs.size());
    for (QEvent qe : evs) {
      assertEquals( 0, qe.getChannel() );
      assertEquals( "2.1", qe.getCue().getCueNumber() );
    }
  }

}
