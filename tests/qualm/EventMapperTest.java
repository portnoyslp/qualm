package qualm;

import org.junit.*;
import static org.junit.Assert.*;

public class EventMapperTest {

  @Test
  public void noToTemplates() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.createNoteOnEventTemplate ( 0, null ) );
    MidiCommand[] out = em.mapEvent( new MidiCommand( 0, MidiCommand.NOTE_ON, 60 )); 
    assertArrayEquals( out, new MidiCommand[0] );
  }

  @Test
  public void nullWhenNoMatch() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.createNoteOnEventTemplate ( 0, null ) );
    MidiCommand[] out = em.mapEvent( new MidiCommand( 0, MidiCommand.NOTE_OFF, 60 )); 
    assertNull( out );
  }
    
  @Test
  public void channelMappedCorrectly() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.createNoteOnEventTemplate ( 0, null ) );
    em.addToTemplate( EventTemplate.createNoteOffEventTemplate ( EventTemplate.DONT_CARE, null ) );
    MidiCommand in = new MidiCommand( 0, MidiCommand.NOTE_ON, 60 );
    MidiCommand[] out = em.mapEvent( in ); 
    assertTrue( out.length == 1 );
    assertEquals( out[0].getChannel(), in.getChannel() );
  }    

  @Test
  public void channelReplacement() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.createNoteOnEventTemplate ( 0, null ) );
    em.addToTemplate( EventTemplate.createNoteOffEventTemplate ( 1, null ) );
    MidiCommand in = new MidiCommand( 0, MidiCommand.NOTE_ON, 60 );
    MidiCommand[] out = em.mapEvent( in ); 
    assertTrue( out.length == 1 );
    assertEquals( out[0].getChannel(), 1 );
  }

  @Test
    public void dataReplacement() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.createNoteOnEventTemplate ( 0, null ) );
    em.addToTemplate( EventTemplate.createNoteOffEventTemplate ( 1, null ) );
    MidiCommand in = new MidiCommand( 0, MidiCommand.NOTE_ON, 60 );
    MidiCommand[] out = em.mapEvent( in ); 
    assertTrue( out.length == 1 );
    assertEquals( out[0].getData1(), in.getData1() );
  }

}
