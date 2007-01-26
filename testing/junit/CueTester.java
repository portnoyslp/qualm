package qualm.testing.junit;

import junit.framework.*;
import junit.textui.*;

import qualm.Cue;

public class CueTester extends TestCase {
  public CueTester (String name) { super(name); }

  public static void main(String[] args) {
    TestRunner.runAndWait(new TestSuite(CueTester.class));
  }

  public void testCue() throws Exception {
    Cue q,q2;
    q = new Cue( "23", "42" );
    assertEquals("23.42", q.getCueNumber());
    assertTrue(q.compareTo(q)==0);
    q2 = new Cue ( "23.42" );
    assertEquals("23.42", q2.getCueNumber());
    assertEquals("23",q2.getSong());
    assertEquals("42",q2.getMeasure());
    assertTrue(q.compareTo(q2)==0);
    assertTrue(q.equals(q2));
    q = new Cue ( "23" );
    assertEquals("23.1", q.getCueNumber());
    assertEquals("23",q.getSong());
    assertEquals("1",q.getMeasure());
    assertTrue(q.compareTo(q2)<0);
    assertTrue(q2.compareTo(q)>0);
    q2 = new Cue ("23a");
    assertTrue(q.compareTo(q2)<0);
    q2 = new Cue ("22a");
    assertTrue(q.compareTo(q2)>0);
  }
}
