package qualm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class QDataYAMLWriterTest {

  // --- helpers ---

  private String write(QData qd) throws Exception {
    StringWriter sw = new StringWriter();
    QDataYAMLWriter.outputYAML(qd, sw);
    return sw.toString();
  }

  private Trigger forwardTrigger(int ch, String note) {
    return new Trigger(EventTemplate.noteOn(ch, note), Trigger.FORWARD);
  }

  private Trigger reverseTrigger(int ch, String note) {
    return new Trigger(EventTemplate.noteOn(ch, note), Trigger.REVERSE);
  }

  private EventMapper controlMapper(int fromCh, String fromCtrl,
      int toCh, String toCtrl) {
    EventMapper em = new EventMapper();
    em.setFromTemplate(EventTemplate.control(fromCh, fromCtrl, null));
    em.setToTemplates(Arrays.asList(EventTemplate.control(toCh, toCtrl, null)));
    return em;
  }

  // --- title ---

  @Test
  public void writesTitle() throws Exception {
    QData qd = new QDataBuilder().withTitle("My Show").build();
    assertTrue(write(qd).contains("title: \"My Show\""));
  }

  // --- channels ---

  @Test
  public void writesChannels() throws Exception {
    QData qd = new QDataBuilder()
        .addMidiChannel(0, null, "Lower Kbd")
        .addMidiChannel(1, null, "Upper Kbd")
        .build();
    String yaml = write(qd);
    assertTrue(yaml.contains("midi_channels:"));
    assertTrue(yaml.contains("  1:\n    name: \"Lower Kbd\""));
    assertTrue(yaml.contains("  2:\n    name: \"Upper Kbd\""));
  }

  // --- patches ---

  @Test
  public void writesPatch() throws Exception {
    Patch p = new Patch("P1", 1);
    p.setDescription("Patch One");
    p.setBank("PrA");
    QData qd = new QDataBuilder().addPatch(p).build();
    String yaml = write(qd);
    assertTrue(yaml.contains("  P1:\n"));
    assertTrue(yaml.contains("    name: \"Patch One\""));
    assertTrue(yaml.contains("    bank: PrA"));
    assertTrue(yaml.contains("    num: 1"));
  }

  @Test
  public void omitsPatchNameWhenSameAsId() throws Exception {
    Patch p = new Patch("Strings", 5);
    p.setDescription("Strings");
    QData qd = new QDataBuilder().addPatch(p).build();
    assertFalse(write(qd).contains("name:"));
  }

  @Test
  public void writesPatchVolume() throws Exception {
    Patch p = new Patch("P1", 1);
    p.setDescription("P1");
    p.setVolume(64);
    QData qd = new QDataBuilder().addPatch(p).build();
    assertTrue(write(qd).contains("    volume: 64"));
  }

  // --- default channel and pc: shorthand ---

  @Test
  public void usesDefaultChannelWhenAllPcsMatch() throws Exception {
    Patch p = new Patch("P1", 1);
    p.setDescription("P1");
    QData qd = new QDataBuilder()
        .addPatch(p)
        .addStream(new QStreamBuilder().withTitle("S")
            .addCue(new Cue.Builder().setCueNumber("1.1")
                .addProgramChangeEvent(0, p).build())
            .addCue(new Cue.Builder().setCueNumber("2.1")
                .addProgramChangeEvent(0, p).build())
            .build())
        .build();
    String yaml = write(qd);
    assertTrue(yaml.contains("    channel: 1"));
    assertTrue(yaml.contains("      pc: P1"));
    assertFalse(yaml.contains("pc:\n"));
  }

  @Test
  public void usesPcMapWhenChannelsMixed() throws Exception {
    Patch p1 = new Patch("P1", 1); p1.setDescription("P1");
    Patch p2 = new Patch("P2", 2); p2.setDescription("P2");
    QData qd = new QDataBuilder()
        .addPatch(p1).addPatch(p2)
        .addStream(new QStreamBuilder().withTitle("S")
            .addCue(new Cue.Builder().setCueNumber("1.1")
                .addProgramChangeEvent(0, p1)
                .addProgramChangeEvent(1, p2)
                .build())
            .build())
        .build();
    String yaml = write(qd);
    assertFalse(yaml.contains("    channel:"));
    assertTrue(yaml.contains("        pc:\n"));
  }

  // --- global trigger hoisting ---

  @Test
  public void hoistsSharedTriggersToGlobal() throws Exception {
    Trigger shared = forwardTrigger(0, "c6");
    Trigger cue1Only = forwardTrigger(0, "c5");
    Trigger cue2Only = forwardTrigger(0, "d5");

    QData qd = new QDataBuilder()
        .addStream(new QStreamBuilder().withTitle("S")
            .addCue(new Cue.Builder().setCueNumber("1.1")
                .addTrigger(shared).addTrigger(cue1Only).build())
            .addCue(new Cue.Builder().setCueNumber("2.1")
                .addTrigger(shared).addTrigger(cue2Only).build())
            .build())
        .build();
    String yaml = write(qd);
    assertTrue(yaml.contains("    global:"));
    assertTrue(yaml.contains("      triggers:"));
    // shared trigger appears once in global, not duplicated on each cue
    assertTrue(countOccurrences(yaml, "c6") == 1);
    // cue-local triggers still present
    assertTrue(yaml.contains("c5"));
    assertTrue(yaml.contains("d5"));
  }

  @Test
  public void doesNotWriteGlobalWhenNoSharedTriggers() throws Exception {
    QData qd = new QDataBuilder()
        .addStream(new QStreamBuilder().withTitle("S")
            .addCue(new Cue.Builder().setCueNumber("1.1")
                .addTrigger(forwardTrigger(0, "c6")).build())
            .addCue(new Cue.Builder().setCueNumber("2.1")
                .addTrigger(forwardTrigger(0, "d6")).build())
            .build())
        .build();
    assertFalse(write(qd).contains("global:"));
  }

  // --- global event_map hoisting ---

  @Test
  public void hoistsSharedEventMapsToGlobal() throws Exception {
    EventMapper shared = controlMapper(0, "damper", 1, "damper");

    QData qd = new QDataBuilder()
        .addStream(new QStreamBuilder().withTitle("S")
            .addCue(new Cue.Builder().setCueNumber("1.1")
                .addEventMap(shared).build())
            .addCue(new Cue.Builder().setCueNumber("2.1")
                .addEventMap(shared).build())
            .build())
        .build();
    String yaml = write(qd);
    assertTrue(yaml.contains("    global:"));
    assertTrue(yaml.contains("      event_maps:"));
    // appears once in global, not per-cue
    assertTrue(countOccurrences(yaml, "from:") == 1);
  }

  // --- compact trigger forms ---

  @Test
  public void usesCompactForwardTrigger() throws Exception {
    QData qd = new QDataBuilder()
        .addStream(new QStreamBuilder().withTitle("S")
            .addCue(new Cue.Builder().setCueNumber("1.1")
                .addTrigger(forwardTrigger(0, "c6")).build())
            .build())
        .build();
    // forward note_on trigger should use "ch:note" compact form
    assertTrue(write(qd).contains("- \"1:c6\""));
  }

  @Test
  public void usesCompactReverseTrigger() throws Exception {
    QData qd = new QDataBuilder()
        .addStream(new QStreamBuilder().withTitle("S")
            .addCue(new Cue.Builder().setCueNumber("1.1")
                .addTrigger(reverseTrigger(0, "c6")).build())
            .build())
        .build();
    assertTrue(write(qd).contains("- reverse: \"1:c6\""));
  }

  // --- helper ---

  private int countOccurrences(String text, String target) {
    int count = 0, idx = 0;
    while ((idx = text.indexOf(target, idx)) != -1) { count++; idx++; }
    return count;
  }
}
