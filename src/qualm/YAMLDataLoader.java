package qualm;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Reads a qualm YAML file and builds a QData structure.
 *
 * Supports all five shorthands defined in the YAML spec:
 *   1. "song.measure" cue keys
 *   2. stream-level default channel
 *   3. pc: program-change shorthand (scalar or map)
 *   4. compact trigger strings ("ch:note" / {reverse: "ch:note"})
 *   5. implicit patch name (defaults to patch ID key)
 */
public class YAMLDataLoader {

  public QData load(String fileSpecification) throws IOException {
    URL inputURL;
    try {
      inputURL = new URL(fileSpecification);
    } catch (MalformedURLException e) {
      try {
        inputURL = new java.io.File(fileSpecification).toURI().toURL();
      } catch (MalformedURLException e1) {
        throw new IllegalArgumentException("unable to build filename", e1);
      }
    }
    InputStream inputStream = inputURL.openConnection().getInputStream();
    return readStream(inputStream);
  }

  @SuppressWarnings("unchecked")
  public QData readStream(InputStream stream) {
    Map<String, Object> doc = (Map<String, Object>) new Yaml().load(stream);
    QData qdata = buildQData(doc);
    qdata.prepareCueStreams();
    return qdata;
  }

  @SuppressWarnings("unchecked")
  private QData buildQData(Map<String, Object> doc) {
    QData qdata = new QData();

    if (doc.containsKey("title"))
      qdata.setTitle((String) doc.get("title"));

    // midi_channels: map of Integer(1-16) -> {name, device_type?}
    Map<Object, Object> channels = (Map<Object, Object>) doc.get("midi_channels");
    if (channels != null) {
      for (Map.Entry<Object, Object> e : channels.entrySet()) {
        int num = ((Number) e.getKey()).intValue();
        Map<String, Object> chData = (Map<String, Object>) e.getValue();
        String name = (String) chData.get("name");
        String deviceType = (String) chData.get("device_type");
        qdata.addMidiChannel(num - 1, deviceType, name);
      }
    }

    // patches: two passes so aliases can resolve their targets
    Map<String, Object> patchSection = (Map<String, Object>) doc.get("patches");
    if (patchSection != null) {
      for (Map.Entry<String, Object> e : patchSection.entrySet()) {
        Map<String, Object> pData = (Map<String, Object>) e.getValue();
        if (!pData.containsKey("target"))
          buildPatch(e.getKey(), pData, qdata);
      }
      for (Map.Entry<String, Object> e : patchSection.entrySet()) {
        Map<String, Object> pData = (Map<String, Object>) e.getValue();
        if (pData.containsKey("target"))
          buildPatchAlias(e.getKey(), pData, qdata);
      }
    }

    // cue_streams: map of String/null -> stream body
    Map<Object, Object> streamSection = (Map<Object, Object>) doc.get("cue_streams");
    if (streamSection != null) {
      for (Map.Entry<Object, Object> e : streamSection.entrySet()) {
        String streamId = (e.getKey() == null) ? null : e.getKey().toString();
        buildStream(streamId, (Map<String, Object>) e.getValue(), qdata);
      }
    }

    return qdata;
  }

  // ---- patch building ----

  private void buildPatch(String id, Map<String, Object> data, QData qdata) {
    Patch p = new Patch(id, ((Number) data.get("num")).intValue());
    String name = (String) data.get("name");
    p.setDescription(name != null ? name : id);
    if (data.containsKey("bank"))
      p.setBank((String) data.get("bank"));
    if (data.containsKey("volume"))
      p.setVolume(parseVolume(data.get("volume")));
    qdata.addPatch(p);
  }

  private void buildPatchAlias(String id, Map<String, Object> data, QData qdata) {
    String targetId = (String) data.get("target");
    Patch target = qdata.lookupPatch(targetId);
    if (target == null) {
      System.err.println("WARNING: patch alias '" + id +
          "' references unknown target '" + targetId + "'");
      return;
    }
    Patch p = new Patch(id, target.getNumber());
    p.setBank(target.getBank());
    String name = (String) data.get("name");
    p.setDescription(name != null ? name : id);
    if (data.containsKey("volume"))
      p.setVolume(parseVolume(data.get("volume")));
    else
      p.setVolume(target.getVolume());
    qdata.addPatch(p);
  }

  private Integer parseVolume(Object vol) {
    if (vol instanceof Integer)
      return (Integer) vol;
    String s = vol.toString();
    if (s.endsWith("%")) {
      int pct = Integer.parseInt(s.substring(0, s.length() - 1));
      return (pct * 127) / 100;
    }
    return Integer.parseInt(s);
  }

  // ---- stream building ----

  @SuppressWarnings("unchecked")
  private void buildStream(String streamId, Map<String, Object> data, QData qdata) {
    QStream qs = new QStream();
    if (streamId != null)
      qs.setTitle(streamId);

    // stream-level default channel (0-indexed internally)
    int defaultChannel = -1;
    if (data.containsKey("channel"))
      defaultChannel = ((Number) data.get("channel")).intValue() - 1;

    List<Trigger> globalTriggers = new ArrayList<Trigger>();
    List<EventMapper> globalMaps = new ArrayList<EventMapper>();
    Map<String, Object> globalData = (Map<String, Object>) data.get("global");
    if (globalData != null) {
      globalTriggers = parseTriggers(
          (List<Object>) globalData.get("triggers"), defaultChannel);
      globalMaps = parseEventMaps(
          (List<Object>) globalData.get("event_maps"), defaultChannel);
    }

    Map<Object, Object> cuesData = (Map<Object, Object>) data.get("cues");
    if (cuesData != null) {
      Cue previousCue = null;
      for (Map.Entry<Object, Object> e : cuesData.entrySet()) {
        if (!(e.getKey() instanceof String)) {
          throw new IllegalArgumentException(
              "Cue key must be a quoted string like \"3.1\", got: " +
              e.getKey() + " (" + e.getKey().getClass().getSimpleName() + ")");
        }
        String cueKey = (String) e.getKey();
        int dotIdx = cueKey.indexOf('.');
        if (dotIdx < 0)
          throw new IllegalArgumentException(
              "Cue key must be \"song.measure\" format: " + cueKey);
        String song = cueKey.substring(0, dotIdx);
        String measure = cueKey.substring(dotIdx + 1);

        Cue cue = new Cue(song, measure);
        if (previousCue != null && cue.compareTo(previousCue) < 0) {
          System.err.println("WARNING: cue \"" + cueKey +
              "\" appears out of order after \"" +
              previousCue.getCueNumber() + "\"");
        }
        previousCue = cue;

        buildCue(cue, (Map<String, Object>) e.getValue(),
            globalTriggers, globalMaps, defaultChannel, qdata);
        qs.addCue(cue);
      }
    }

    qdata.addCueStream(qs);
  }

  // ---- cue building ----

  @SuppressWarnings("unchecked")
  private void buildCue(Cue cue, Map<String, Object> data,
      List<Trigger> globalTriggers, List<EventMapper> globalMaps,
      int defaultChannel, QData qdata) {

    List<QEvent> events = new ArrayList<QEvent>();

    // pc: shorthand — program changes prepended before events
    Object pc = data.get("pc");
    if (pc instanceof String) {
      if (defaultChannel < 0)
        throw new IllegalArgumentException(
            "pc: scalar used without stream default channel at cue " +
            cue.getCueNumber());
      events.add(new ProgramChangeEvent(defaultChannel, cue,
          lookupPatchOrWarn((String) pc, qdata, cue)));
    } else if (pc instanceof Map) {
      Map<Object, Object> pcMap = (Map<Object, Object>) pc;
      for (Map.Entry<Object, Object> e : pcMap.entrySet()) {
        int ch = ((Number) e.getKey()).intValue() - 1;
        events.add(new ProgramChangeEvent(ch, cue,
            lookupPatchOrWarn(e.getValue().toString(), qdata, cue)));
      }
    }

    // full events list
    List<Object> eventList = (List<Object>) data.get("events");
    if (eventList != null) {
      for (Object evObj : eventList) {
        QEvent ev = parseEvent((Map<String, Object>) evObj,
            defaultChannel, cue, qdata);
        if (ev != null)
          events.add(ev);
      }
    }
    cue.setEvents(events);

    // cue-level triggers appended after global triggers
    List<Trigger> triggers = new ArrayList<Trigger>(globalTriggers);
    List<Object> triggerList = (List<Object>) data.get("triggers");
    if (triggerList != null)
      triggers.addAll(parseTriggers(triggerList, defaultChannel));
    cue.setTriggers(triggers);

    // event maps appended after global maps
    List<EventMapper> eventMaps = new ArrayList<EventMapper>(globalMaps);
    List<Object> mapList = (List<Object>) data.get("event_maps");
    if (mapList != null)
      eventMaps.addAll(parseEventMaps(mapList, defaultChannel));
    cue.setEventMaps(eventMaps);
  }

  // ---- event parsing ----

  @SuppressWarnings("unchecked")
  private QEvent parseEvent(Map<String, Object> map, int defaultChannel,
      Cue cue, QData qdata) {
    for (Map.Entry<String, Object> e : map.entrySet()) {
      String type = e.getKey();
      if ("program_change".equals(type)) {
        Map<String, Object> a = (Map<String, Object>) e.getValue();
        int ch = getChannel(a, defaultChannel);
        return new ProgramChangeEvent(ch, cue,
            lookupPatchOrWarn((String) a.get("patch"), qdata, cue));

      } else if ("note_on".equals(type)) {
        Map<String, Object> a = (Map<String, Object>) e.getValue();
        return new MidiEvent(EventTemplate.noteOn(
            getChannel(a, defaultChannel), (String) a.get("note")));

      } else if ("note_off".equals(type)) {
        Map<String, Object> a = (Map<String, Object>) e.getValue();
        return new MidiEvent(EventTemplate.noteOff(
            getChannel(a, defaultChannel), (String) a.get("note")));

      } else if ("control_change".equals(type)) {
        Map<String, Object> a = (Map<String, Object>) e.getValue();
        String value = a.containsKey("value") ? a.get("value").toString() : null;
        return new MidiEvent(EventTemplate.control(
            getChannel(a, defaultChannel), a.get("control").toString(), value));

      } else if ("sysex".equals(type)) {
        String hex = e.getValue().toString().replaceAll("\\s+", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
          data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
              + Character.digit(hex.charAt(i + 1), 16));
        MidiCommand mc = new MidiCommand();
        mc.setSysex(data);
        return new MidiEvent(mc);

      } else if ("note_window_change".equals(type)) {
        Map<String, Object> a = (Map<String, Object>) e.getValue();
        int ch = getChannel(a, defaultChannel);
        Integer top = a.containsKey("top") ? ((Number) a.get("top")).intValue() : null;
        Integer bottom = a.containsKey("bottom") ? ((Number) a.get("bottom")).intValue() : null;
        return new NoteWindowChangeEvent(ch, cue, bottom, top);

      } else if ("advance".equals(type)) {
        Map<String, Object> a = (Map<String, Object>) e.getValue();
        StreamAdvance sa = new StreamAdvance((String) a.get("stream"), cue);
        if (a.containsKey("song")) sa.setSong(a.get("song").toString());
        if (a.containsKey("measure")) sa.setMeasure(a.get("measure").toString());
        return sa;
      }
    }
    System.err.println("WARNING: unrecognized event: " + map);
    return null;
  }

  // ---- trigger parsing ----

  @SuppressWarnings("unchecked")
  private List<Trigger> parseTriggers(List<Object> list, int defaultChannel) {
    List<Trigger> result = new ArrayList<Trigger>();
    if (list == null) return result;
    for (Object item : list) {
      if (item instanceof String) {
        // compact forward trigger: "ch:note"
        result.add(triggerFromString((String) item, false, 0, defaultChannel));
      } else if (item instanceof Map) {
        Map<String, Object> map = (Map<String, Object>) item;
        // compact reverse trigger: {reverse: "ch:note"} or {reverse: "ch:note", delay: N}
        if (map.containsKey("reverse") && map.get("reverse") instanceof String) {
          int delay = map.containsKey("delay") ?
              ((Number) map.get("delay")).intValue() : 0;
          result.add(triggerFromString(
              (String) map.get("reverse"), true, delay, defaultChannel));
        } else {
          result.add(triggerFromMap(map, defaultChannel));
        }
      }
    }
    return result;
  }

  private Trigger triggerFromString(String s, boolean reverse, int delay,
      int defaultChannel) {
    int ch;
    String note;
    int colonIdx = s.indexOf(':');
    if (colonIdx >= 0) {
      ch = Integer.parseInt(s.substring(0, colonIdx)) - 1;
      note = s.substring(colonIdx + 1);
    } else {
      if (defaultChannel < 0)
        throw new IllegalArgumentException(
            "Trigger \"" + s + "\" has no channel and stream has no default");
      ch = defaultChannel;
      note = s;
    }
    Trigger t = new Trigger(EventTemplate.noteOn(ch, note), reverse);
    if (delay > 0) t.setDelay(delay);
    return t;
  }

  @SuppressWarnings("unchecked")
  private Trigger triggerFromMap(Map<String, Object> map, int defaultChannel) {
    boolean reverse = Boolean.TRUE.equals(map.get("reverse"));
    int delay = map.containsKey("delay") ?
        ((Number) map.get("delay")).intValue() : 0;
    EventTemplate et = parseEventTemplate(map, defaultChannel);
    Trigger t = new Trigger(et, reverse);
    if (delay > 0) t.setDelay(delay);
    return t;
  }

  // ---- event template parsing (triggers and event_maps) ----

  @SuppressWarnings("unchecked")
  private EventTemplate parseEventTemplate(Map<String, Object> map,
      int defaultChannel) {
    for (Map.Entry<String, Object> e : map.entrySet()) {
      String type = e.getKey();
      if ("reverse".equals(type) || "delay".equals(type)) continue;
      Map<String, Object> a = (Map<String, Object>) e.getValue();
      int ch = getChannel(a, defaultChannel);
      if ("note_on".equals(type)) {
        return EventTemplate.noteOn(ch, (String) a.get("note"));
      } else if ("note_off".equals(type)) {
        return EventTemplate.noteOff(ch, (String) a.get("note"));
      } else if ("control_change".equals(type)) {
        String thresh = a.containsKey("threshold") ?
            a.get("threshold").toString() : null;
        return EventTemplate.control(ch, a.get("control").toString(), thresh);
      }
      throw new IllegalArgumentException("Unknown trigger/map event type: " + type);
    }
    throw new IllegalArgumentException("No event type in map: " + map);
  }

  // ---- event_maps parsing ----

  @SuppressWarnings("unchecked")
  private List<EventMapper> parseEventMaps(List<Object> list,
      int defaultChannel) {
    List<EventMapper> result = new ArrayList<EventMapper>();
    if (list == null) return result;
    for (Object item : list) {
      Map<String, Object> mapData = (Map<String, Object>) item;
      EventMapper em = new EventMapper();
      em.setFromTemplate(parseEventTemplate(
          (Map<String, Object>) mapData.get("from"), defaultChannel));
      List<Object> toList = (List<Object>) mapData.get("to");
      for (Object toItem : toList)
        em.addToTemplate(parseEventTemplate(
            (Map<String, Object>) toItem, defaultChannel));
      result.add(em);
    }
    return result;
  }

  // ---- helpers ----

  private int getChannel(Map<String, Object> attrs, int defaultChannel) {
    if (attrs != null && attrs.containsKey("channel"))
      return ((Number) attrs.get("channel")).intValue() - 1;
    if (defaultChannel >= 0)
      return defaultChannel;
    throw new IllegalArgumentException(
        "No channel specified and stream has no default channel");
  }

  private Patch lookupPatchOrWarn(String id, QData qdata, Cue cue) {
    Patch p = qdata.lookupPatch(id);
    if (p == null)
      System.err.println("WARNING: unknown patch '" + id +
          "' in cue " + cue.getCueNumber());
    return p;
  }

  public static QData loadQDataFromFilename(String fileName) {
    try {
      return new YAMLDataLoader().load(fileName);
    } catch (IOException e) {
      throw new RuntimeException("Unable to load file: " + fileName, e);
    }
  }
}
