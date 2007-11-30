package qualm.testing.junit;

import junit.framework.*;
import junit.textui.*;

import qualm.*;

public class QDataLoaderTester extends TestCase {
  public QDataLoaderTester (String name) { super(name); }

  public static void main(String[] args) {
    TestRunner.runAndWait(new TestSuite(QDataLoaderTester.class));
  }

  public void testLoad1() throws Exception {
    // load a test file
    String fname = "src/qualm/testing/junit/qdl-1.xml";
    QDataLoader qdl = new QDataLoader();
    QData qd = qdl.readFile( new java.io.File(fname) );
    
    assertEquals("QDL-1", qd.getTitle());
    
    // check channels
    String ch[] = qd.getMidiChannels();
    assertEquals("Lower Kbd",ch[0]);
    assertEquals("Upper Kbd",ch[1]);
    assertEquals("Drums",ch[9]);
    assertNull(ch[2]);
    
    // check patches
    assertTrue(5 == qd.getPatches().size());
    assertEquals(qd.lookupPatch("P1").getID(),"P1");
    assertEquals(qd.lookupPatch("P1").getDescription(),"Patch 1");
    assertTrue(qd.lookupPatch("P1").getNumber() == 1);
    assertEquals(qd.lookupPatch("Timpani").getID(),"Timpani");
    assertEquals(qd.lookupPatch("Timpani").getDescription(),"Timpani");
    assertTrue(qd.lookupPatch("Timpani").getNumber() == 5);

    // patch volumes
    assertNull(qd.lookupPatch("P1").getVolume()); 
    assertEquals(qd.lookupPatch("P2").getVolume().intValue(), 20);
    assertEquals(qd.lookupPatch("P3").getVolume().intValue(), 
		 (int)((80*127)/100));
    assertEquals(qd.lookupPatch("P3_2").getVolume().intValue(), 127);

    // check streams
    assertTrue(2==qd.getCueStreams().size());
    QStream s = (QStream) ((java.util.List)qd.getCueStreams()).get(0);
    assertEquals(s.getTitle(),"First_Stream");
    assertTrue(2==s.getCues().size());
    s = (QStream)  ((java.util.List)qd.getCueStreams()).get(1);
    assertEquals(s.getTitle(),"Second_Stream");
    assertTrue(2==s.getCues().size());

    // random check of cues
    s = (QStream) ((java.util.List)qd.getCueStreams()).get(0);
    Cue q = (Cue) s.getCues().first();
    assertEquals(q,new Cue("3.1"));
    assertEquals(q.getEvents().size(), 1);
    assertEquals(q.getEventMaps().size(), 1);
    assertEquals(q.getTriggers().size(), 3);

    s = (QStream) ((java.util.List)qd.getCueStreams()).get(1);
    q = (Cue) s.getCues().last();
    assertEquals(q,new Cue("2.10"));
    assertEquals(q.getEvents().size(), 2);
    assertEquals(q.getEventMaps().size(), 0);
    assertEquals(q.getTriggers().size(), 1);

  }

}
