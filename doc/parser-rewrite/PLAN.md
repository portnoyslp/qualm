# XML to YAML Migration Plan for Qualm

This document outlines the project plan to migrate the project's data format
from the current XML/DTD-based structure to a leaner YAML representation.

## 1. Overview

- **Current format:** XML files validated by `qualm.dtd` and parsed with
  `QDataXMLReader`/`QDataLoader`.
- **Location of examples:** `test-input.xml` and
  `tests/samples/xml/*.xml`.
- **Goal:** Introduce YAML as the primary configuration format while
  preserving backward compatibility.

## 2. Current XML Structure

The XML structure consists of:

- `qualm-data` (root) with optional `title`, required `midi-channels`,
  `patches`, and one or more `cue-stream` elements.
- `midi-channels`: list of `channel` elements (attributes: `num`,
  `device-type`).
- `patches`: list of `patch`/`patch-alias` elements containing patch
  definitions and references.
- `cue-stream`: optional `global` block with triggers and event maps, and
  repeating `cue` elements identified by `song` and `measure`.
- `cue`: may contain `trigger`, `events`, `map-events` sub‑elements.
- Event types: `note-on`, `note-off`, `control-change`, `program-change`,
  `sysex`, `note-window-change`, `advance`, `clear`.

The current DTD enforces attributes and provides examples via comments.

## 3. Proposed YAML Structure

Design considerations:

- Use dictionaries and lists for hierarchical data.
- Remove boilerplate tags; prefer `key: value` pairs.
- Represent channels as map keyed by channel number.
- Use explicit `-` for lists of patches, cues, events, etc.
- Provide easy aliasing for patches using a `target` field.

### Example snippet

```yaml
title: "Test File for Qualm"

midi_channels:
  1:
    name: "Lower Keyboard"
    device_type: "Roland JV-1080"

patches:
  Nice_Piano:
    bank: User
    num: 1
    name: "Nice Piano"
  P5_alias:
    target: Nice_Piano
    name: "Good Piano"

cue_streams:
  First_Stream:
    global:
      triggers:
        - note_on:
            channel: 1
            note: C6
    cues:
      - song: "1a"
        measure: 10
        events:
          - program_change:
              channel: 1
              patch: P3
```

A full schema will be documented separately.

## 4. Migration Phases

1. **Analysis & Design** – Document YAML format, map XML → YAML,
   determine backwards-compatibility strategy.
2. **Core YAML Parser** – Implement `YAMLDataLoader`/`YAMLDataWriter` using
   SnakeYAML; add tests.
3. **Format Conversion Tool** – Utility to convert existing XML samples to
   YAML and verify equivalence.
4. **Integration & Compatibility** – Update loading logic in
   `MasterController`, `QualmREPL`, etc.; detect format by extension.
5. **Testing & Validation** – Add unit/integration tests, performance
   benchmarks, and round‑trip verification.
6. **Documentation & Migration Guide** – Update README and add new docs.
7. **Deprecation & Cleanup** – Gradually phase out XML support after
   providing migration path.

## 5. Timeline (estimates)

| Phase | Duration | Weeks |
|-------|----------|-------|
| Analysis & Design | 10d | 1–2 |
| Parser | 15d | 3–4 |
| Converter | 5d | 5 |
| Integration | 12d | 5–7 |
| Testing | 10d | 8–9 |
| Documentation | 5d | 10 |
| Cleanup | ongoing | post-release |

## 6. Risks & Mitigation

- **Data loss during conversion** – extensive round-trip tests.
- **User resistance** – maintain both formats; provide clear docs.
- **Performance regression** – benchmark YAML parsing.

## 7. Success Criteria

- YAML files are smaller and easier to read.
- All sample and test files converted with identical behavior.
- Tests pass for both formats; migration is transparent.
- Documentation complete and rollout communicated.

---
*Plan created by GitHub Copilot on March 4, 2026.*
