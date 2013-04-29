package qualm;

import org.junit.*;
import static org.junit.Assert.*;

import javax.sound.midi.*;
import static javax.sound.midi.ShortMessage.*;

public class AdvanceTester {
  @Test
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
    fm.addOutgoing((long)0, NOTE_ON, 0, 60, 10); // switch to P2
    fm.addOutgoing((long)300, NOTE_ON, 0, 59, 10); // ignored; too soon
    fm.addOutgoing((long)1500, NOTE_ON, 0, 60, 10); // ignored; at end
    fm.addOutgoing((long)3000, NOTE_ON, 0, 59, 10); // reverse; to P1
    fm.addOutgoing((long)4500, NOTE_ON, 0, 59, 10); // reverse; same patch
    fm.run();
    java.util.ArrayList<Object> msgs = fm.receivedMessages();

    fm.printOutMessages();

    assertTrue(msgs.size() == 4);
    FakeMIDI.assertMIDI(msgs.get(0),PROGRAM_CHANGE,0,0,0); // init patch
    FakeMIDI.assertMIDI(msgs.get(1),PROGRAM_CHANGE,0,1,0); // advance
    FakeMIDI.assertMIDI(msgs.get(2),PROGRAM_CHANGE,0,0,0); // reverse
    FakeMIDI.assertMIDI(msgs.get(3),PROGRAM_CHANGE,0,0,0); // reverse (re-inits)
  }
}
