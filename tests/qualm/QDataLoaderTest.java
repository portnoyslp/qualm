package qualm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

public class QDataLoaderTest {

  QData qd;
  String fname;
  
  @Before
  public void loadQDL1() {
    fname = "tests/qualm/qdl-1.xml";
    QDataLoader qdl = new QDataLoader();
    qd = qdl.readFile( new java.io.File(fname) );
  }
  
  @Test
  public void checkTitle() {
    assertEquals("QDL-1", qd.getTitle());
  }
  
  @Test
  public void checkChannels() {
    // check channels
    String ch[] = qd.getMidiChannels();
    assertEquals("Lower Kbd",ch[0]);
    assertEquals("Upper Kbd",ch[1]);
    assertEquals("Drums",ch[9]);
    assertNull(ch[2]);
  }
  
  @Test
  public void checkPatches() {
    assertTrue(5 == qd.getPatches().size());
    assertEquals(qd.lookupPatch("P1").getID(),"P1");
    assertEquals(qd.lookupPatch("P1").getDescription(),"Patch 1");
    assertTrue(qd.lookupPatch("P1").getNumber() == 1);
    assertEquals(qd.lookupPatch("Timpani").getID(),"Timpani");
    assertEquals(qd.lookupPatch("Timpani").getDescription(),"Timpani");
    assertTrue(qd.lookupPatch("Timpani").getNumber() == 5);
  }
  
  @Test
  public void checkPatchVolumes() {
    assertNull(qd.lookupPatch("P1").getVolume()); 
    assertEquals(qd.lookupPatch("P2").getVolume().intValue(), 20);
    assertEquals(qd.lookupPatch("P3").getVolume().intValue(), 
		 (int)((80*127)/100));
    assertEquals(qd.lookupPatch("P3_2").getVolume().intValue(), 127);
  }
  
  @Test
  public void checkStreams() {
    assertTrue(2==qd.getCueStreams().size());
    QStream s = (QStream) ((List<QStream>)qd.getCueStreams()).get(0);
    assertEquals(s.getTitle(),"First_Stream");
    assertTrue(2==s.getCues().size());
    s = (QStream)  ((List<QStream>)qd.getCueStreams()).get(1);
    assertEquals(s.getTitle(),"Second_Stream");
    assertTrue(2==s.getCues().size());
  }
  
  @Test
  public void spotCheckCues() {
    // check a subset of the cues
    QStream s = (QStream) ((List<QStream>)qd.getCueStreams()).get(0);
    Cue q = (Cue) s.getCues().first();
    assertEquals(q,new Cue("3.1"));
    assertEquals(q.getEvents().size(), 1);
    assertEquals(q.getEventMaps().size(), 1);
    assertEquals(q.getTriggers().size(), 3);
    
    s = (QStream) ((List<QStream>)qd.getCueStreams()).get(1);
    q = (Cue) s.getCues().last();
    assertEquals(q,new Cue("2.10"));
    assertEquals(q.getEvents().size(), 2);
    assertEquals(q.getEventMaps().size(), 0);
    assertEquals(q.getTriggers().size(), 1);
  }

  @Test
  public void checkForDelay() {
    QStream s = (QStream) ((List<QStream>)qd.getCueStreams()).get(0);
    Cue q = (Cue) s.getCues().first();
    assertTrue(q.getTriggers().toString().indexOf("dly2500")>-1);
  }
  
  @Test
  public void checkSysex() {
    QStream s = (QStream) ((List<QStream>)qd.getCueStreams()).get(0);
    Cue q = (Cue) s.getCues().first();

    // check on the sysex event; should be 11 bytes
    s = (QStream) ((List<QStream>)qd.getCueStreams()).get(0);
    q = (Cue) s.getCues().last();
    Collection<QEvent> eventColl = q.getEvents();
    assertEquals(eventColl.size(),4);
    for(QEvent qe : eventColl) {
      if (qe instanceof MidiEvent &&
	  ((MidiEvent)qe).getMidiCommand().getType() == MidiCommand.SYSEX) {
	assertEquals( ((MidiEvent)qe).getMidiCommand().hexData(), "F04110421240007F0041F7");
      }
    }
  }
  
  @Test
  public void checkWritingAndReading() {
    // make sure we don't have any differences if we write and then read the doc.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    QDataXMLReader.outputXML(qd, baos);
    
    QDataLoader qdl2 = new QDataLoader();
    InputSource src = new InputSource(new StringReader(baos.toString()));
    QData readIn = qdl2.readSource(src);

    assertEquals(qd,readIn);
  }
  
  @Test
  public void spotCheckOutputXML() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    QDataXMLReader.outputXML(qd,baos);
    String outputDoc = removeCRs(baos.toString());

    // Count up various elements to make sure they match expectations
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
    f.close();
    return new String(buffer);
  }

}
