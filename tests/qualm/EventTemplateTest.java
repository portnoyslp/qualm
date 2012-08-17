package qualm;

import org.junit.*;
import static org.junit.Assert.*;

import static qualm.MidiCommand.*;
import static qualm.EventTemplate.DONT_CARE;

public class EventTemplateTest {

  @Test
  public void matchNoteRange() {
    // c4 = middle C = MIDI 60, A#5 = MIDI 82
    EventTemplate et = EventTemplate.createNoteOnEventTemplate( 0, "c4-A#5" );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_ON, 59 )));
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 82 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_ON, 83 )));
  }

  @Test
  public void matchOpenEndedHighRange() {
    // c4 = middle C = MIDI 60, A#5 = MIDI 82
    EventTemplate et = EventTemplate.createNoteOnEventTemplate( 0, "c4-" );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_ON, 59 )));
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 82 )));
  }

  @Test
  public void matchOpenEndedLowRange() {
    // c4 = middle C = MIDI 60, A#5 = MIDI 82
    EventTemplate et = EventTemplate.createNoteOnEventTemplate( 0, "-a#5" );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60 )));
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 82 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_ON, 83 )));
  }

  @Test
  public void dontMatchDifferentChannel() {
    EventTemplate et = EventTemplate.createNoteOnEventTemplate( 0, "c4" );
    assertFalse(et.match( new MidiCommand( 1, NOTE_ON, 60 )));
  }

  @Test
  public void matchDontCareChannel() {
    EventTemplate et = EventTemplate.createNoteOnEventTemplate( DONT_CARE, "c4" );
    assertTrue(et.match( new MidiCommand( 1, NOTE_ON, 60 )));
  }

  @Test
  public void dontMatchWrongType() {
    EventTemplate et = EventTemplate.createNoteOnEventTemplate( DONT_CARE, null );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_OFF, 60 )));
    assertFalse(et.match( new MidiCommand( 0, CONTROL_CHANGE, 60 )));
  }

  @Test
  public void matchControllerString() {
    // sustain controller is MIDI 64
    EventTemplate et = EventTemplate.createControlEventTemplate( 0, "sustain", null );
    assertTrue(et.match( new MidiCommand( 0, CONTROL_CHANGE, 64, 120 )));
  }

  @Test
  public void matchControllerThreshold() {
    // volume controller is MIDI 7
    EventTemplate et = EventTemplate.createControlEventTemplate( 0, "volume", "100" );
    System.err.println(et);
    assertFalse(et.match( new MidiCommand( 0, CONTROL_CHANGE, 7, 20 )));
    assertTrue(et.match( new MidiCommand( 0, CONTROL_CHANGE, 7, 120 )));
  }

  @Test
  public void noteOnWithZeroMatchesNoteOff() {
    EventTemplate et = EventTemplate.createNoteOffEventTemplate( 0, "c4" );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60, 0 )));
  }
    
}
