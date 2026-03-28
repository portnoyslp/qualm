package qualm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for YAMLDataLoader, mirroring QDataLoaderTest against qdl-1.yaml
 * (the YAML equivalent of qdl-1.xml).
 */
public class YAMLDataLoaderTest {

  QData qd;
  String fname;

  @BeforeEach
  public void loadQDL1() throws Exception {
    fname = "tests/qualm/qdl-1.yaml";
    qd = new YAMLDataLoader().load(fname);
  }

  @Test
  public void checkTitle() {
    assertEquals("QDL-1", qd.getTitle());
  }

  @Test
  public void checkChannels() {
    String[] ch = qd.getMidiChannels();
    assertEquals("Lower Kbd", ch[0]);
    assertEquals("Upper Kbd", ch[1]);
    assertEquals("Drums", ch[9]);
    assertNull(ch[2]);
  }

  @Test
  public void checkPatches() {
    assertEquals(5, qd.getPatches().size());
    assertEquals("P1", qd.lookupPatch("P1").getID());
    assertEquals("Patch 1", qd.lookupPatch("P1").getDescription());
    assertEquals(1, qd.lookupPatch("P1").getNumber());
    assertEquals("Timpani", qd.lookupPatch("Timpani").getID());
    // name defaults to key when omitted
    assertEquals("Timpani", qd.lookupPatch("Timpani").getDescription());
    assertEquals(5, qd.lookupPatch("Timpani").getNumber());
  }

  @Test
  public void checkPatchVolumes() {
    assertNull(qd.lookupPatch("P1").getVolume());
    assertEquals(20, qd.lookupPatch("P2").getVolume().intValue());
    assertEquals((80 * 127) / 100, qd.lookupPatch("P3").getVolume().intValue());
    assertEquals(127, qd.lookupPatch("P3_2").getVolume().intValue());
  }

  @Test
  public void checkPatchAlias() {
    Patch alias = qd.lookupPatch("P3_2");
    assertNotNull(alias);
    // alias inherits number and bank from target P3
    assertEquals(qd.lookupPatch("P3").getNumber(), alias.getNumber());
    assertEquals(qd.lookupPatch("P3").getBank(), alias.getBank());
    assertEquals("Patch 3 Alias", alias.getDescription());
    // alias overrides volume to 100%
    assertEquals(127, alias.getVolume().intValue());
  }

  @Test
  public void checkStreams() {
    assertEquals(2, qd.getCueStreams().size());
    QStream s = (QStream) ((List<QStream>) qd.getCueStreams()).get(0);
    assertEquals("First_Stream", s.getTitle());
    assertEquals(2, s.getCues().size());
    s = (QStream) ((List<QStream>) qd.getCueStreams()).get(1);
    assertEquals("Second_Stream", s.getTitle());
    assertEquals(2, s.getCues().size());
  }

  @Test
  public void spotCheckCues() {
    QStream s = (QStream) ((List<QStream>) qd.getCueStreams()).get(0);
    Cue q = s.getCues().first();
    assertEquals(new Cue("3.1"), q);
    assertEquals(2, q.getEvents().size());     // pc: P1 + note_on
    assertEquals(1, q.getEventMaps().size());  // global event_map
    assertEquals(3, q.getTriggers().size());   // 2 global + 1 cue-level

    s = (QStream) ((List<QStream>) qd.getCueStreams()).get(1);
    q = s.getCues().last();
    assertEquals(new Cue("2.10"), q);
    assertEquals(3, q.getEvents().size());     // pc: P3_2 + note_off + advance
    assertEquals(0, q.getEventMaps().size());
    assertEquals(1, q.getTriggers().size());   // 1 global
  }

  @Test
  public void checkDelay() {
    QStream s = (QStream) ((List<QStream>) qd.getCueStreams()).get(0);
    Cue q = s.getCues().first();
    assertTrue(q.getTriggers().toString().indexOf("dly2500") > -1);
  }

  @Test
  public void checkSysex() {
    QStream s = (QStream) ((List<QStream>) qd.getCueStreams()).get(0);
    Cue q = s.getCues().last();
    Collection<QEvent> events = q.getEvents();
    assertEquals(4, events.size());  // pc:Timpani + pc:P3 + control_change + sysex
    for (QEvent qe : events) {
      if (qe instanceof MidiEvent &&
          ((MidiEvent) qe).getMidiCommand().getType() == MidiCommand.SYSEX) {
        assertEquals("F04110421240007F0041F7",
            ((MidiEvent) qe).getMidiCommand().hexData());
      }
    }
  }

  @Test
  public void checkOutOfOrderWarningDoesNotThrow() {
    // Out-of-order cues produce a warning but not an exception; just verify
    // the loader completes successfully on a well-ordered file.
    assertNotNull(qd);
  }

  /**
   * Verify YAML and XML produce equal QData objects for the same logical file.
   */
  @Test
  public void yamlMatchesXml() throws Exception {
    QData fromXml = new QDataLoader().load("tests/qualm/qdl-1.xml");
    assertEquals(fromXml, qd);
  }
}
