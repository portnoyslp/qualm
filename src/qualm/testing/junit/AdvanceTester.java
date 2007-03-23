package qualm.testing.junit;

import junit.framework.*;
import junit.textui.*;

import javax.sound.midi.*;

import qualm.*;

public class AdvanceTester extends TestCase {
  public AdvanceTester (String name) { super(name); }

  public static void main(String[] args) {
    TestRunner.runAndWait(new TestSuite(AdvanceTester.class));
  }

  public void testAdvance() throws Exception {
    // simple advancement.
  String adv1 = "<qualm-data>\n" + 
"  <title>ADV-1</title>\n" + 
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
"      <trigger reverse=\"yes\"><note-on channel=\"1\" note=\"b3\"/></trigger>\n" + 
"    </global>\n" + 
"    <cue song=\"1\" measure=\"1\">\n" + 
"      <events><program-change channel=\"1\" patch=\"P1\"/></events>\n" + 
"    </cue>\n" + 
"    <cue song=\"2\" measure=\"1\">\n" + 
"      <events><program-change channel=\"1\" patch=\"P2\"/></events>\n" + 
"    </cue>\n" + 
"  </cue-stream>\n" + 
"</qualm-data>\n";

    FakeMIDI fm = FakeMIDI.prepareTest(adv1);
    fm.addOutgoing((long)0, ShortMessage.NOTE_ON, 0, 60, 10); // switch to P2
    fm.addOutgoing((long)300, ShortMessage.NOTE_ON, 0, 59, 10); // ignored; too soon
    fm.addOutgoing((long)1500, ShortMessage.NOTE_ON, 0, 60, 10); // ignored; at end
    fm.addOutgoing((long)3000, ShortMessage.NOTE_ON, 0, 59, 10); // reverse; to P1
    fm.addOutgoing((long)4500, ShortMessage.NOTE_ON, 0, 59, 10); // reverse; same patch
    fm.run();
    java.util.ArrayList msgs = fm.receivedMessages();

    System.out.println("Number of msgs received == " + msgs.size());
    java.util.Iterator iter = msgs.iterator();
    while (iter.hasNext()) {
      System.out.println("   " + MidiMessageParser.messageToString((MidiMessage)iter.next()));
    }

    assertTrue(msgs.size() == 4);
    FakeMIDI.assertMIDI(msgs.get(0),ShortMessage.PROGRAM_CHANGE,0,0,0); // init patch
    FakeMIDI.assertMIDI(msgs.get(1),ShortMessage.PROGRAM_CHANGE,0,1,0); // advance
    FakeMIDI.assertMIDI(msgs.get(2),ShortMessage.PROGRAM_CHANGE,0,0,0); // reverse
    FakeMIDI.assertMIDI(msgs.get(3),ShortMessage.PROGRAM_CHANGE,0,0,0); // reverse (re-inits)
  }

  /* Created to exploit a specific bug, when a channel is not used in the first cue, but we reverse to it anyway. */
  public void testReverseEmptyFirstQ() throws Exception {
    // simple advancement.
  String adv1 = "<qualm-data>\n" + 
"  <title>ADV-2</title>\n" + 
"  <midi-channels>\n" + 
"    <channel num='1'>K1</channel>\n" + 
"    <channel num='2'>K2</channel>\n" + 
"  </midi-channels>\n" + 
"  <patches>\n" + 
"    <patch id='P1' num='1'>Patch 1</patch>\n" + 
"    <patch id='P2' num='2'>Patch 2</patch>\n" + 
"  </patches>\n" + 
"  <cue-stream>\n" + 
"    <global>\n" + 
"      <trigger><note-on channel='1' note='c4'/></trigger>\n" + 
"      <trigger reverse='yes'><note-on channel='1' note='b3'/></trigger>\n" + 
"    </global>\n" + 
"    <cue song='1' measure='1'>\n" + 
"      <events><program-change channel='1' patch='P1'/></events>\n" + 
"    </cue>\n" + 
"    <cue song='2' measure='1'>\n" + 
"      <events><program-change channel='1' patch='P2'/></events>\n" + 
"      <events><program-change channel='2' patch='P1'/></events>\n" + 
"    </cue>\n" + 
"  </cue-stream>\n" + 
"</qualm-data>\n";

    FakeMIDI fm = FakeMIDI.prepareTest(adv1);
    fm.addOutgoing((long)0, ShortMessage.NOTE_ON, 0, 60, 10); // switch to P2
    fm.addOutgoing((long)1500, ShortMessage.NOTE_ON, 0, 59, 10); // reverse; to P1
    fm.run();

    java.util.ArrayList msgs = fm.receivedMessages();

    System.out.println("Number of msgs received == " + msgs.size());
    java.util.Iterator iter = msgs.iterator();
    while (iter.hasNext()) {
      System.out.println("   " + MidiMessageParser.messageToString((MidiMessage)iter.next()));
    }

    assertTrue(msgs.size() == 4);
    FakeMIDI.assertMIDI(msgs.get(0),ShortMessage.PROGRAM_CHANGE,0,0,0); // init patch
    FakeMIDI.assertMIDI(msgs.get(1),ShortMessage.PROGRAM_CHANGE,0,1,0); // advance K1
    FakeMIDI.assertMIDI(msgs.get(2),ShortMessage.PROGRAM_CHANGE,1,0,0); // advance K2
    FakeMIDI.assertMIDI(msgs.get(3),ShortMessage.PROGRAM_CHANGE,0,0,0); // reverse
  }


}
