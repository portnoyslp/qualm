# YAML Format Specification for Qualm Data

This document defines the YAML representation that replaces the current XML
structure. It is the authoritative reference for both implementers and users.

---

## Top-Level Structure

```yaml
title: <string>          # optional, human-readable name

midi_channels:           # mapping: channel number (1–16) -> metadata
  <1-16>:
    name: <string>       # required, display name
    device_type: <string> # optional, e.g. "Roland JV-1080"

patches:                 # mapping: patch ID -> properties
  <id>:
    name: <string>       # optional; defaults to the patch ID key if omitted
    num: <integer>       # required
    bank: <string>       # optional, e.g. "User", "PrA", "XpA"
    volume: <string|int> # optional; integer 0–127 or percentage string "23%"
    comment: <string>    # optional, free-form note (preserved but ignored at runtime)
    target: <id>         # if present, this is an alias — bank/num inherited from target

cue_streams:             # mapping: stream ID -> stream definition
  <id>:                  # use ~ (null) for an unnamed/anonymous stream
    channel: <1-16>      # optional default channel for all events in this stream
    global:
      triggers:          # list of trigger objects (see Triggers section)
        - <trigger>
      event_maps:
        - from:
            <event_template>
          to:
            - <event_template>
    cues:                # mapping: "song.measure" -> cue body (ALWAYS quote the key)
      "<song>.<measure>":
        triggers:        # optional list of trigger objects
          - <trigger>
        pc: <shorthand>  # optional program-change shorthand (see below)
        events:          # optional list of full event objects
          - <event>
        event_maps:      # optional
          - from:
              <event_template>
            to:
              - <event_template>
```

---

## Shorthand Forms

These five shorthands reduce typing for the most common patterns. Each has a
canonical long form that remains valid; the loader accepts both.

### 1. `"song.measure"` Cue Keys

Cues are keyed by `"song.measure"` string rather than listed with separate
`song:` and `measure:` fields. This matches the display format used at runtime
(e.g., `5b.22`).

```yaml
cues:
  "1a.10":
    pc: P3
  "2.35":
    pc: Nice_Piano
  "3.1":
    triggers:
      - "2:g2"
    pc:
      2: P3
```

**Always quote the key.** An unquoted key like `1.0:` is parsed by YAML as the
float `1.0`; `2.10:` becomes float `2.1`. Any measure of `0` or `10` is a
common trap. The loader rejects non-string keys with a clear error.

Cue ordering in the file is irrelevant — `QStream` sorts cues by the
song/measure comparator regardless. However, the loader prints a warning if
cues are encountered out of order, since that likely indicates a typo in a
key (e.g. `"3.16"` appearing before `"3.1"`).

### 2. Stream Default Channel

Declare a default MIDI channel at the stream level. Any event or event template
within the stream that omits `channel:` uses this value. An explicit `channel:`
on an individual event overrides the default.

```yaml
cue_streams:
  K1:
    channel: 1
    cues:
      "5.1":
        events:
          - program_change:
              patch: Acoustic_Piano   # channel: 1 implied
          - program_change:
              channel: 3              # explicit override
              patch: Drums
```

### 3. `pc:` Program-Change Shorthand

Because program changes make up the vast majority of cue events, a compact
`pc:` key is provided at the cue level.

**Scalar** — single patch on the stream default channel:

```yaml
"1a.10":
  pc: P3
```

**Map** — one or more patches keyed by channel number:

```yaml
"3.16":
  pc:
    4: P96
    1: P5
```

`pc:` may coexist with `events:` for cues that mix program changes with other
event types. Program changes from `pc:` are appended before any entries in
`events:`.

```yaml
"2.10":
  pc: P95
  events:
    - control_change:
        control: "10"
        value: 140
```

### 4. Compact Trigger Strings

The common trigger pattern — advance on a specific note-on — has two compact
forms depending on direction:

```yaml
# Forward trigger: scalar string
triggers:
  - "1:c6"

# Reverse trigger: single-key map whose value is the note string
triggers:
  - reverse: "1:c2"

# Reverse with delay: two-key map
triggers:
  - reverse: "1:c2"
    delay: 500
```

The string `"channel:note"` is always a `note_on` template. If the stream has
a default channel the prefix may be omitted: `"c6"` means `"<default>:c6"`.

A bare scalar list item (`- "1:c6"`) cannot carry sibling keys in YAML, so
any trigger needing `reverse:` or `delay:` uses a map form instead. The
compact `{reverse: "ch:note"}` is recognized when the value of `reverse:` is
a string rather than a boolean. Forward triggers with `delay:`, and any
trigger using `note_off` or `control_change`, require the full map form:

```yaml
triggers:
  - note_on:
      channel: 2
      note: g2
    delay: 2500
  - note_off:
      channel: 2
      note: c6
```

### 5. Implicit Patch Name

When a patch's display name is identical to its ID key, `name:` may be omitted:

```yaml
# Short form
patches:
  Accordion:
    bank: PrD
    num: 22

# Equivalent long form
patches:
  Accordion:
    name: "Accordion"
    bank: PrD
    num: 22
```

---

## Event Types (Full Form)

### `program_change`

```yaml
program_change:
  channel: 1       # required unless stream default applies
  patch: P3        # required; must reference an entry in patches
```

### `note_on` / `note_off`

```yaml
note_on:
  channel: 1
  note: "C4-D5"   # optional; note name, range, or omit for any
  value: 127       # optional, velocity
```

### `control_change`

```yaml
control_change:
  channel: 2
  control: "damper"  # named control or integer string
  value: 64          # optional
```

Named controls: `modulation`, `breath`, `foot`, `volume`, `balance`, `pan`,
`expression`, `damper`, `sustain`, `portamento`, `sustenuto`, `soft`,
`legato`, `all sound off`, `reset controllers`, `all notes off`.

### `note_window_change`

```yaml
note_window_change:
  channel: 1
  top: 84      # optional MIDI note number
  bottom: 60   # optional MIDI note number
```

At least one of `top` or `bottom` is required.

### `advance`

```yaml
advance:
  stream: Second_Stream  # required
  song: "2"              # optional
  measure: "128"         # optional
```

### `sysex`

```yaml
sysex: "F0 7E 7F 09 01 F7"   # space-separated hex bytes
```

---

## Trigger Objects

```yaml
- note_on:          # or note_off / control_change template
    channel: 1      # required unless stream default applies
    note: C6        # optional
  reverse: true     # optional boolean, default false
  delay: 500        # optional integer milliseconds, default 0
```

String shorthand: `"channel:note"` or `"note"` (uses stream default channel).

`delay` in XML is a float in **seconds**; in YAML it is an **integer in
milliseconds** (×1000).

---

## Patch IDs with Special Characters

Patch IDs containing spaces, `/`, `#`, `?`, or other non-alphanumeric
characters must be quoted as YAML string keys throughout the document:

```yaml
patches:
  "Hammond B3 / Moog Bass":
    bank: User
    num: 999
  "Distortion Guitar #2":
    target: "Distortion Guitar"
```

---

## Unnamed Cue Streams

A cue stream with no ID uses the YAML null key `~`:

```yaml
cue_streams:
  ~:
    global: ...
    cues: ...
```

`QStream` auto-generates an internal title for null-keyed streams.

---

## Complete XML → YAML Mapping Table

| XML element / attribute | YAML equivalent | Notes |
|---|---|---|
| `<qualm-data>` | document root | — |
| `<title>` | `title:` | scalar string |
| `<channel num="N" device-type="T">name</channel>` | `midi_channels: N: {name: ..., device_type: ...}` | key is integer 1–16 |
| `<patch id="X" bank="B" num="N">desc</patch>` | `patches: X: {name: desc, bank: B, num: N}` | `name` defaults to key when equal to id |
| `<patch id="X" .../>` (no text content) | `patches: X: {num: N, ...}` | name defaults to key |
| `comment="..."` on patch | `comment:` | preserved but ignored at runtime |
| `<patch-alias id="X" target="T">desc</patch-alias>` | `patches: X: {name: desc, target: T}` | volume may be overridden |
| `<cue-stream id="S">` | `cue_streams: S:` | — |
| `<cue-stream>` (no id) | `cue_streams: ~:` | QStream auto-generates title |
| `<global>` | `global:` within stream | — |
| `<trigger reverse="yes" delay="0.5">` | `- ...\n  reverse: true\n  delay: 500` | delay: s×1000 → ms integer |
| `<cue song="S" measure="M">` | `cues: "S.M":` | always quote the key |
| `<events>` | `events:` list | — |
| `<program-change channel="C" patch="P"/>` | `- program_change: {channel: C, patch: P}` or `pc:` shorthand | — |
| `<note-on channel="C" note="N" value="V"/>` | `- note_on: {channel: C, note: N, value: V}` | — |
| `<note-off channel="C" note="N"/>` | `- note_off: {channel: C, note: N}` | — |
| `<control-change channel="C" control="X" value="V"/>` | `- control_change: {channel: C, control: X, value: V}` | — |
| `<sysex>F0 ...</sysex>` | `- sysex: "F0 ..."` | hex string; whitespace normalized |
| `<note-window-change channel="C" top="T" bottom="B"/>` | `- note_window_change: {channel: C, top: T, bottom: B}` | — |
| `<advance stream="S" song="X" measure="M"/>` | `- advance: {stream: S, song: X, measure: M}` | song/measure optional |
| `<map-events>` | entry in `event_maps:` list | — |
| `<map-from>` | `from:` within event_map | one event template |
| `<map-to>` | entry in `to:` list | multiple allowed |
| `reverse="yes"` | `reverse: true` | any non-"false" XML value → true |
| `delay="0.5"` (seconds) | `delay: 500` (milliseconds) | multiply by 1000 |

---

## Validation & Conventions

- Channel numbers in `midi_channels` keys are integers 1–16.
- `num` on a patch is an integer; avoid leading zeros.
- Cue keys must always be quoted strings: `"5b.22"`, not `5b.22`.
- `song` and `measure` components are strings and may contain letters: `"4a"`,
  `"41c"`, `"aa"`.
- Volume may be an integer 0–127 or a percentage string `"23%"` (converted as
  `round(pct * 127 / 100)`).

---

## Backward-Compatibility Strategy

### Format Detection

The loader dispatches on file extension:

- `.xml` → `QDataLoader` (SAX-based)
- `.yaml` / `.yml` → `YAMLDataLoader` (SnakeYAML-based)

`MasterController` and `QualmREPL` pass a filename string; dispatch is
transparent to calling code.

### Transition Period

Both parsers are maintained simultaneously. XML remains supported until all
known users have migrated (Phase 7).

### Round-Trip Fidelity

`YAMLDataLoader` + `YAMLDataWriter` must round-trip to an equal `QData` object
(verified via `QData.equals()`). The conversion tool (Phase 3) validates this
for every XML sample.

### Known Lossy Mappings

| XML feature | YAML handling | Loss |
|---|---|---|
| `comment` attribute on patch | Preserved as `comment:` | None |
| `remark` attribute (DTD only) | Not in Java model; dropped | None |
| Whitespace / formatting | Not preserved | Cosmetic only |
| XML comments (`<!-- ... -->`) | Dropped | Cosmetic only |

---

## Full Example — `test-input.xml` Converted

```yaml
title: "Test File for Qualm"

midi_channels:
  1:
    name: "Lower Keyboard"
    device_type: "Roland JV-1080"
  2:
    name: "Upper Keyboard"
  4:
    name: "Drum Kit"
    device_type: "Alesis"

patches:
  Nice_Piano:
    bank: User
    num: 1
  P2:
    name: "TremStr/Str"
    bank: User
    num: 2
  P3:
    name: "Strs/FzzClav"
    bank: User
    num: 3
    comment: "split at C5"
  P4:
    name: "Rhodes"
    bank: PrA
    num: 15
    volume: "23%"
  P5:
    name: "Good Piano"
    target: Nice_Piano
  P95:
    num: 95
  P96:
    num: 96

cue_streams:
  First_Stream:
    channel: 1
    global:
      triggers:
        - "1:c6"
        - reverse: "1:c2"
      event_maps:
        - from:
            control_change:
              control: damper
          to:
            - control_change:
                channel: 2
                control: "80"
    cues:
      "1a.10":
        pc: P3
      "2.35":
        pc: Nice_Piano
        event_maps:
          - from:
              note_on:
                note: "C4-D5"
            to:
              - note_on:
                  channel: 2
          - from:
              note_off:
                note: "C4-D5"
            to:
              - note_off:
                  channel: 2
      "2.102":
        pc: P4
      "3.1":
        triggers:
          - "2:g2"
        pc:
          2: P3
      "3.16":
        pc:
          4: P96
          1: P5

  Second_Stream:
    channel: 2
    global:
      triggers:
        - "2:c6"
    cues:
      "1.1":
        pc: P96
      "2.10":
        pc: P95
        events:
          - control_change:
              control: "10"
              value: 140
```

---

*Specification finalized March 21, 2026.*
