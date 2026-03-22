package qualm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Writes a QData object in Qualm YAML format.
 *
 * Mirrors QDataXMLReader.outputXML() for the XML format.
 * Uses shorthands where possible (pc: scalar, compact trigger strings).
 *
 * Global triggers and event_maps are reconstructed by finding the longest
 * common prefix shared by all cues in a stream.
 */
public class QDataYAMLWriter {

  public static void outputYAML(QData qd, Writer out) throws IOException {
    new QDataYAMLWriter(new PrintWriter(out)).write(qd);
  }

  private final PrintWriter out;

  private QDataYAMLWriter(PrintWriter out) {
    this.out = out;
  }

  private void write(QData qd) {
    if (qd.getTitle() != null)
      out.println("title: " + quoteString(qd.getTitle()));
    writeChannels(qd.getMidiChannels());
    writePatches(qd.getPatches());
    writeCueStreams(qd.getCueStreams());
    out.flush();
  }

  // --- channels ---

  private void writeChannels(String[] channels) {
    boolean any = false;
    for (String ch : channels)
      if (ch != null) { any = true; break; }
    if (!any) return;

    out.println();
    out.println("midi_channels:");
    for (int i = 0; i < channels.length; i++) {
      if (channels[i] != null) {
        out.println("  " + (i + 1) + ":");
        out.println("    name: " + quoteString(channels[i]));
      }
    }
  }

  // --- patches ---

  private void writePatches(Collection<Patch> patches) {
    if (patches.isEmpty()) return;
    List<Patch> sorted = new ArrayList<>(patches);
    Collections.sort(sorted, new Comparator<Patch>() {
      public int compare(Patch a, Patch b) { return a.getID().compareTo(b.getID()); }
    });

    out.println();
    out.println("patches:");
    for (Patch p : sorted) {
      out.println("  " + p.getID() + ":");
      if (!p.getID().equals(p.getDescription()))
        out.println("    name: " + quoteString(p.getDescription()));
      if (p.getBank() != null)
        out.println("    bank: " + p.getBank());
      out.println("    num: " + p.getNumber());
      if (p.getVolume() != null)
        out.println("    volume: " + p.getVolume());
    }
  }

  // --- streams ---

  private void writeCueStreams(Collection<QStream> streams) {
    if (streams.isEmpty()) return;
    out.println();
    out.println("cue_streams:");
    for (QStream qs : streams)
      writeStream(qs);
  }

  private void writeStream(QStream qs) {
    String title = qs.getTitle();
    out.println("  " + (title != null ? title : "~") + ":");

    int defaultChannel = detectDefaultChannel(qs);
    if (defaultChannel >= 0)
      out.println("    channel: " + (defaultChannel + 1));

    List<Trigger> globalTriggers = detectGlobalTriggerPrefix(qs);
    List<EventMapper> globalMaps = detectGlobalMapPrefix(qs);

    if (!globalTriggers.isEmpty() || !globalMaps.isEmpty()) {
      out.println("    global:");
      if (!globalTriggers.isEmpty()) {
        out.println("      triggers:");
        for (Trigger t : globalTriggers)
          writeTrigger(t, defaultChannel, "        ");
      }
      if (!globalMaps.isEmpty()) {
        out.println("      event_maps:");
        for (EventMapper em : globalMaps)
          writeEventMapper(em, defaultChannel, "        ");
      }
    }

    if (!qs.getCues().isEmpty()) {
      out.println("    cues:");
      for (Cue cue : qs.getCues())
        writeCue(cue, defaultChannel, globalTriggers.size(), globalMaps.size(), "      ");
    }
  }

  private List<Trigger> detectGlobalTriggerPrefix(QStream qs) {
    return detectCommonPrefix(qs, true);
  }

  private List<EventMapper> detectGlobalMapPrefix(QStream qs) {
    return detectCommonPrefix(qs, false);
  }

  private <T> List<T> detectCommonPrefix(QStream qs, boolean forTriggers) {
    List<Cue> cues = new ArrayList<>(qs.getCues());
    if (cues.size() < 2) return Collections.emptyList();

    List<T> candidate = cueTriggerOrMapList(cues.get(0), forTriggers);
    for (int i = 1; i < cues.size(); i++) {
      List<T> cueList = cueTriggerOrMapList(cues.get(i), forTriggers);
      int prefixLen = 0;
      while (prefixLen < candidate.size() && prefixLen < cueList.size()
          && candidate.get(prefixLen).equals(cueList.get(prefixLen)))
        prefixLen++;
      candidate = candidate.subList(0, prefixLen);
      if (candidate.isEmpty()) return Collections.emptyList();
    }
    return new ArrayList<>(candidate);
  }

  private <T> List<T> cueTriggerOrMapList(Cue cue, boolean forTriggers) {
    if (forTriggers) {
      List<Trigger> t = cue.getTriggers() == null ? Collections.<Trigger>emptyList()
          : new ArrayList<>(cue.getTriggers());
      @SuppressWarnings("unchecked") List<T> result = (List<T>) t;
      return result;
    } else {
      List<EventMapper> m = cue.getEventMaps() == null ? Collections.<EventMapper>emptyList()
          : new ArrayList<>(cue.getEventMaps());
      @SuppressWarnings("unchecked") List<T> result = (List<T>) m;
      return result;
    }
  }

  /** Returns the 0-indexed channel used by all ProgramChangeEvents, or -1. */
  private int detectDefaultChannel(QStream qs) {
    int found = Integer.MIN_VALUE;
    for (Cue cue : qs.getCues()) {
      if (cue.getEvents() == null) continue;
      for (QEvent ev : cue.getEvents()) {
        if (ev instanceof ProgramChangeEvent) {
          int ch = ev.getChannel();
          if (found == Integer.MIN_VALUE) found = ch;
          else if (found != ch) return -1;
        }
      }
    }
    return found == Integer.MIN_VALUE ? -1 : found;
  }

  // --- cue ---

  private void writeCue(Cue cue, int defaultChannel,
      int globalTriggerCount, int globalMapCount, String indent) {
    out.println(indent + "\"" + cue.getCueNumber() + "\":");
    String i2 = indent + "  ";

    List<ProgramChangeEvent> pcs = new ArrayList<>();
    List<QEvent> otherEvents = new ArrayList<>();
    if (cue.getEvents() != null) {
      for (QEvent ev : cue.getEvents()) {
        if (ev instanceof ProgramChangeEvent)
          pcs.add((ProgramChangeEvent) ev);
        else
          otherEvents.add(ev);
      }
    }

    // pc: shorthand
    if (!pcs.isEmpty()) {
      if (pcs.size() == 1 && pcs.get(0).getChannel() == defaultChannel) {
        out.println(i2 + "pc: " + pcs.get(0).getPatch().getID());
      } else {
        out.println(i2 + "pc:");
        for (ProgramChangeEvent pce : pcs)
          out.println(i2 + "  " + (pce.getChannel() + 1) + ": " + pce.getPatch().getID());
      }
    }

    if (!otherEvents.isEmpty()) {
      out.println(i2 + "events:");
      for (QEvent ev : otherEvents)
        writeEvent(ev, defaultChannel, i2 + "  ");
    }

    // write only the cue-local triggers (after the global prefix)
    List<Trigger> cueTriggers = cue.getTriggers() == null
        ? Collections.<Trigger>emptyList()
        : new ArrayList<>(cue.getTriggers()).subList(
            globalTriggerCount, cue.getTriggers().size());
    if (!cueTriggers.isEmpty()) {
      out.println(i2 + "triggers:");
      for (Trigger t : cueTriggers)
        writeTrigger(t, defaultChannel, i2 + "  ");
    }

    // write only the cue-local event_maps (after the global prefix)
    List<EventMapper> cueMaps = cue.getEventMaps() == null
        ? Collections.<EventMapper>emptyList()
        : new ArrayList<>(cue.getEventMaps()).subList(
            globalMapCount, cue.getEventMaps().size());
    if (!cueMaps.isEmpty()) {
      out.println(i2 + "event_maps:");
      for (EventMapper em : cueMaps)
        writeEventMapper(em, defaultChannel, i2 + "  ");
    }
  }

  // --- events ---

  private void writeEvent(QEvent ev, int defaultChannel, String indent) {
    if (ev instanceof MidiEvent) {
      MidiCommand cmd = ((MidiEvent) ev).getMidiCommand();
      int ch = cmd.getChannel();
      switch (cmd.getType()) {
        case MidiCommand.NOTE_ON:
          out.println(indent + "- note_on:");
          if (ch != defaultChannel)
            out.println(indent + "    channel: " + (ch + 1));
          out.println(indent + "    note: " +
              Utilities.midiNumberToNoteName(cmd.getData1()).toLowerCase());
          break;
        case MidiCommand.NOTE_OFF:
          out.println(indent + "- note_off:");
          if (ch != defaultChannel)
            out.println(indent + "    channel: " + (ch + 1));
          out.println(indent + "    note: " +
              Utilities.midiNumberToNoteName(cmd.getData1()).toLowerCase());
          break;
        case MidiCommand.CONTROL_CHANGE:
          out.println(indent + "- control_change:");
          if (ch != defaultChannel)
            out.println(indent + "    channel: " + (ch + 1));
          out.println(indent + "    control: " + cmd.getData1());
          out.println(indent + "    value: " + cmd.getData2());
          break;
        case MidiCommand.SYSEX:
          out.println(indent + "- sysex: \"" + cmd.hexData() + "\"");
          break;
        default:
          out.println(indent + "# unrecognized event type: " + cmd.getType());
      }
    } else if (ev instanceof StreamAdvance) {
      StreamAdvance sa = (StreamAdvance) ev;
      out.println(indent + "- advance:");
      out.println(indent + "    stream: " + sa.getStreamID());
      if (sa.getSong() != null)
        out.println(indent + "    song: \"" + sa.getSong() + "\"");
      if (sa.getMeasure() != null)
        out.println(indent + "    measure: \"" + sa.getMeasure() + "\"");
    } else if (ev instanceof NoteWindowChangeEvent) {
      NoteWindowChangeEvent nw = (NoteWindowChangeEvent) ev;
      int ch = nw.getChannel();
      out.println(indent + "- note_window_change:");
      if (ch != defaultChannel)
        out.println(indent + "    channel: " + (ch + 1));
      if (nw.getBottomNote() != null)
        out.println(indent + "    bottom: " + nw.getBottomNote());
      if (nw.getTopNote() != null)
        out.println(indent + "    top: " + nw.getTopNote());
    } else {
      out.println(indent + "# unrecognized event: " + ev);
    }
  }

  // --- triggers ---

  private void writeTrigger(Trigger t, int defaultChannel, String indent) {
    EventTemplate et = t.getTemplate();
    boolean isNoteOn = et.getType() == MidiCommand.NOTE_ON;
    boolean isSingleNote = et.extra1Min != EventTemplate.DONT_CARE
        && et.extra1Min == et.extra1Max;
    int delay = t.getDelay();

    if (isNoteOn && isSingleNote && !t.getReverse() && delay == 0) {
      // compact forward: - "ch:note"
      out.println(indent + "- \"" + triggerNoteString(et, defaultChannel) + "\"");
    } else if (isNoteOn && isSingleNote && t.getReverse()) {
      // compact reverse: - reverse: "ch:note"  [optional delay on next line]
      out.print(indent + "- reverse: \"" + triggerNoteString(et, defaultChannel) + "\"");
      if (delay > 0)
        out.print("\n" + indent + "  delay: " + delay);
      out.println();
    } else {
      // full map form
      out.println(indent + "- " + getTypeName(et.getType()) + ":");
      int ch = et.channel();
      if (ch >= 0 && ch != defaultChannel)
        out.println(indent + "    channel: " + (ch + 1));
      writeEventTemplateBody(et, indent + "    ");
      if (t.getReverse())
        out.println(indent + "  reverse: true");
      if (delay > 0)
        out.println(indent + "  delay: " + delay);
    }
  }

  private String triggerNoteString(EventTemplate et, int defaultChannel) {
    String note = Utilities.midiNumberToNoteName(et.extra1Min).toLowerCase();
    if (et.channel() == defaultChannel)
      return note;
    return (et.channel() + 1) + ":" + note;
  }

  // --- event_maps ---

  private void writeEventMapper(EventMapper em, int defaultChannel, String indent) {
    out.println(indent + "- from:");
    out.println(indent + "    " + getTypeName(em.getFromTemplate().getType()) + ":");
    writeEventTemplateAttrs(em.getFromTemplate(), defaultChannel, indent + "      ");
    out.println(indent + "  to:");
    for (EventTemplate et : em.getToTemplateList()) {
      out.println(indent + "    - " + getTypeName(et.getType()) + ":");
      writeEventTemplateAttrs(et, defaultChannel, indent + "        ");
    }
  }

  private void writeEventTemplateAttrs(EventTemplate et, int defaultChannel, String indent) {
    int ch = et.channel();
    if (ch != EventTemplate.DONT_CARE && ch != defaultChannel)
      out.println(indent + "channel: " + (ch + 1));
    writeEventTemplateBody(et, indent);
  }

  private void writeEventTemplateBody(EventTemplate et, String indent) {
    if (et.getType() == MidiCommand.CONTROL_CHANGE) {
      out.println(indent + "control: " + et.getExtra1());
    } else {
      if (et.extra1Min == EventTemplate.DONT_CARE) return;
      String noteStr;
      if (et.extra1Min == et.extra1Max) {
        noteStr = Utilities.midiNumberToNoteName(et.extra1Min).toLowerCase();
      } else {
        noteStr = Utilities.midiNumberToNoteName(et.extra1Min).toLowerCase()
            + "-" + Utilities.midiNumberToNoteName(et.extra1Max).toLowerCase();
      }
      out.println(indent + "note: " + noteStr);
    }
  }

  // --- helpers ---

  private String getTypeName(int midiType) {
    switch (midiType) {
      case MidiCommand.NOTE_ON:        return "note_on";
      case MidiCommand.NOTE_OFF:       return "note_off";
      case MidiCommand.CONTROL_CHANGE: return "control_change";
      default:                         return "unknown_" + midiType;
    }
  }

  private String quoteString(String s) {
    if (s == null) return "~";
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
