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
    assertTrue(4 == qd.getPatches().size());
    assertEquals(qd.lookupPatch("P1").getID(),"P1");
    assertEquals(qd.lookupPatch("P1").getDescription(),"Patch 1");
    assertTrue(qd.lookupPatch("P1").getNumber() == 1);
    assertEquals(qd.lookupPatch("Timpani").getID(),"Timpani");
    assertEquals(qd.lookupPatch("Timpani").getDescription(),"Timpani");
    assertTrue(qd.lookupPatch("Timpani").getNumber() == 5);
    
  }

}
