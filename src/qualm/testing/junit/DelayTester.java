package qualm.testing.junit;

import junit.framework.*;
import junit.textui.*;

import javax.sound.midi.*;

import qualm.*;

public class DelayTester extends TestCase {
  public DelayTester (String name) { super(name); }

  public static void main(String[] args) {
    TestRunner.runAndWait(new TestSuite(DelayTester.class));
  }

  public void testDelay() throws Exception {
    // simple advancement.
    String delay1 = "<qualm-data>\n" + 
"  <title>ADV-1</title>\n" + 
"  <midi-channels>\n" + 
"    <channel num=\"1\">Kbd</channel>\n" + 
"  </midi-channels>\n" + 
"  <patches>\n" + 
"    <patch id=\"P1\" num=\"1\">Patch 1</patch>\n" + 
"    <patch id=\"P2\" num=\"2\">Patch 2</patch>\n" + 
"    <patch id=\"P3\" num=\"3\">Patch 3</patch>\n" + 
"  </patches>\n" + 
"  <cue-stream>\n" + 
"    <global>\n" + 
"      <trigger delay=\"2.5\"><note-on channel=\"1\" note=\"c4\"/></trigger>\n" + 
"      <trigger reverse=\"yes\"><note-on channel=\"1\" note=\"b3\"/></trigger>\n" + 
"    </global>\n" + 
"    <cue song=\"1\" measure=\"1\">\n" + 
"      <events><program-change channel=\"1\" patch=\"P1\"/></events>\n" + 
"    </cue>\n" + 
"    <cue song=\"2\" measure=\"1\">\n" + 
"      <events><program-change channel=\"1\" patch=\"P2\"/></events>\n" + 
"    </cue>\n" + 
"    <cue song=\"3\" measure=\"1\">\n" + 
"      <events><program-change channel=\"1\" patch=\"P3\"/></events>\n" + 
"    </cue>\n" + 
"  </cue-stream>\n" + 
"</qualm-data>\n";

    // Send two forward triggers, separated by a second.  Only the first should fire. Then we reverse.

    FakeMIDI fm = FakeMIDI.prepareTest(delay1);
    fm.addOutgoing((long)0, ShortMessage.NOTE_ON, 0, 60, 10); // switch to P2
    fm.addOutgoing((long)1000, ShortMessage.NOTE_ON, 0, 60, 10); // switch to P2; should be ignored
    fm.addOutgoing((long)3000, ShortMessage.NOTE_ON, 0, 59, 10); // reverse; to P1
    fm.run();
    java.util.ArrayList msgs = fm.receivedMessages();

    fm.printOutMessages();

    assertTrue(msgs.size() == 3);
    FakeMIDI.assertMIDI(msgs.get(0),ShortMessage.PROGRAM_CHANGE,0,0,0); // init patch
    FakeMIDI.assertMIDI(msgs.get(1),ShortMessage.PROGRAM_CHANGE,0,1,0); // advance
    FakeMIDI.assertMIDI(msgs.get(2),ShortMessage.PROGRAM_CHANGE,0,0,0); // reverse

    // was the first message delayed about 2500ms?
    FakeMIDI.assertTS(msgs.get(1),2500);
  }

}
