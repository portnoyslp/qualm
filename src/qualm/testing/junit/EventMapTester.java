package qualm.testing.junit;

import junit.framework.*;
import junit.textui.*;

import javax.sound.midi.*;

import qualm.*;

public class EventMapTester extends TestCase {
  public EventMapTester (String name) { super(name); }

  public static void main(String[] args) {
    TestRunner.runAndWait(new TestSuite(EventMapTester.class));
  }

  public void testEventMap() throws Exception {
    // simple event mapping.
  String evm1 = "<qualm-data>\n" + 
"  <title>EVM-1</title>\n" + 
"  <midi-channels>\n" + 
"    <channel num=\"1\">Kbd</channel>\n" + 
"  </midi-channels>\n" + 
"  <patches>\n" + 
"    <patch id=\"P1\" num=\"1\">Patch 1</patch>\n" + 
"    <patch id=\"P2\" num=\"2\">Patch 2</patch>\n" + 
"  </patches>\n" + 
"  <cue-stream>\n" + 
"    <global>\n" + 
"      <trigger><note-on channel=\"1\" note=\"c4\"/></trigger>\n" + 
"      <map-events>\n" +
"	<map-from><control-change channel=\"1\" control=\"damper\"/></map-from>\n" +
"	<map-to><control-change channel=\"2\" control=\"soft\"/></map-to>\n" +
"      </map-events>\n" +
"    </global>\n" + 
"    <cue song=\"1\" measure=\"1\">\n" + 
"      <events><program-change channel=\"1\" patch=\"P1\"/></events>\n" + 
"    </cue>\n" + 
"  </cue-stream>\n" + 
"</qualm-data>\n";

    FakeMIDI fm = FakeMIDI.prepareTest(evm1);
    fm.addOutgoing((long)0, ShortMessage.CONTROL_CHANGE, 0, 64, 10 );
    fm.addOutgoing((long)1500, ShortMessage.CONTROL_CHANGE, 1, 64, 10 ); //wrong channel; ignore
    fm.addOutgoing((long)1600, ShortMessage.CONTROL_CHANGE, 0, 65, 10 ); //wrong control; ignore
    fm.addOutgoing((long)1700, ShortMessage.CONTROL_CHANGE, 0, 64, 50 );
    fm.run();
    java.util.ArrayList msgs = fm.receivedMessages();

    fm.printOutMessages();

    assertTrue(msgs.size() == 3);
    FakeMIDI.assertMIDI(msgs.get(0),ShortMessage.PROGRAM_CHANGE,0,0,0); // init patch
    FakeMIDI.assertMIDI(msgs.get(1),ShortMessage.CONTROL_CHANGE,1,67,10); // first ctrl
    FakeMIDI.assertMIDI(msgs.get(2),ShortMessage.CONTROL_CHANGE,1,67,50); // last ctrl
  }

}
