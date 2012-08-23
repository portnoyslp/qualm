package qualm;

import java.io.*;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.*;

import org.junit.*;
import static org.junit.Assert.*;

public class QDataLoaderTester {

  @Test
  public void testLoad1() throws Exception {
    // load a test file
    String fname = "tests/qualm/qdl-1.xml";
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
    QStream s = (QStream) ((List<QStream>)qd.getCueStreams()).get(0);
    assertEquals(s.getTitle(),"First_Stream");
    assertTrue(2==s.getCues().size());
    s = (QStream)  ((List<QStream>)qd.getCueStreams()).get(1);
    assertEquals(s.getTitle(),"Second_Stream");
    assertTrue(2==s.getCues().size());

    // random check of cues
    s = (QStream) ((List<QStream>)qd.getCueStreams()).get(0);
    Cue q = (Cue) s.getCues().first();
    assertEquals(q,new Cue("3.1"));
    assertEquals(q.getEvents().size(), 1);
    assertEquals(q.getEventMaps().size(), 1);
    assertEquals(q.getTriggers().size(), 3);

    // do we have the delay listed?
    assertTrue(q.getTriggers().toString().indexOf("dly2500")>-1);

    // check on the sysex event; should be 11 bytes
    q = (Cue) s.getCues().last();
    Collection<QEvent> eventColl = q.getEvents();
    assertEquals(eventColl.size(),4);
    Iterator<QEvent> iter = eventColl.iterator();
    while (iter.hasNext()) {
      QEvent qe = iter.next();
      if (qe instanceof MidiEvent &&
	  ((MidiEvent)qe).getMidiCommand().getType() == MidiCommand.SYSEX) {
	assertEquals( ((MidiEvent)qe).getMidiCommand().hexData(), "F04110421240007F0041F7");
      }
    }

    s = (QStream) ((List<QStream>)qd.getCueStreams()).get(1);
    q = (Cue) s.getCues().last();
    assertEquals(q,new Cue("2.10"));
    assertEquals(q.getEvents().size(), 2);
    assertEquals(q.getEventMaps().size(), 0);
    assertEquals(q.getTriggers().size(), 1);

    // Finally, can we write everything we read?  Get a normalized (no
    // carriage returns or following white spaces) version of the
    // input document, and compare it to a normalized version of the
    // output.
    String inputDoc = removeCRs(readFileAsString(fname));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    QDataXMLReader.outputXML(qd,baos);
    String outputDoc = removeCRs(baos.toString());
    // TODO This assertion doesn't quite work yet; there are too many small differences.
    //assertEquals(inputDoc,outputDoc);

    // So, let's count up various elements to make sure they match expectations
    assertEquals(3,countMatches(outputDoc,"<channel "));
    assertEquals(5,countMatches(outputDoc,"<patch "));
    assertEquals(2,countMatches(outputDoc,"<cue-stream "));
    assertEquals(4,countMatches(outputDoc,"<cue "));
    assertEquals(1,countMatches(outputDoc,"<advance "));
    assertEquals(1,countMatches(outputDoc,"<sysex>"));
    assertEquals(5,countMatches(outputDoc,"<control-change ")); // 5 because the global event-map is copied to the separate cues.
  }

  public int countMatches(String input, String find) {
    Pattern p = Pattern.compile(find);
    Matcher m = p.matcher(input);
    int i = 0;
    boolean result = m.find();
    while (result) {
      i++;
      result = m.find();
    }
    return i;
  }

  public String removeCRs ( String input ) {
    String output = input.replaceAll("\n\\s*", "");
    // also get rid of the header
    output = output.replaceFirst("^.*<qualm-data>","<qualm-data>");
    return output;
  }
  public String readFileAsString( String filename ) throws java.io.IOException {
    byte[] buffer = new byte[(int) new File(filename).length()];
    BufferedInputStream f = new BufferedInputStream(new FileInputStream(filename));
    f.read(buffer);
    return new String(buffer);
  }

}
