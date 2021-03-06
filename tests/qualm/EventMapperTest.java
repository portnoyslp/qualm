package qualm;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static qualm.MidiCommand.NOTE_OFF;
import static qualm.MidiCommand.NOTE_ON;

import org.junit.Test;

public class EventMapperTest {

  @Test
  public void absenceOfToTemplates() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.noteOn ( 0, null ) );
    MidiCommand[] out = em.mapEvent( new MidiCommand( 0, NOTE_ON, 60 )); 
    assertArrayEquals( out, new MidiCommand[0] );
  }

  @Test
  public void emptyWhenNoMatch() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.noteOn ( 0, null ) );
    MidiCommand[] out = em.mapEvent( new MidiCommand( 0, NOTE_OFF, 60 )); 
    assertArrayEquals( out, new MidiCommand[0] );
  }
    
  @Test
  public void channelMappedCorrectly() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.noteOn ( 0, null ) );
    em.addToTemplate( EventTemplate.noteOff ( EventTemplate.DONT_CARE, null ) );
    MidiCommand in = new MidiCommand( 0, NOTE_ON, 60 );
    MidiCommand[] out = em.mapEvent( in ); 
    assertTrue( out.length == 1 );
    assertEquals( out[0].getChannel(), in.getChannel() );
  }    

  @Test
  public void channelReplacement() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.noteOn ( 0, null ) );
    em.addToTemplate( EventTemplate.noteOff ( 1, null ) );
    MidiCommand in = new MidiCommand( 0, NOTE_ON, 60 );
    MidiCommand[] out = em.mapEvent( in ); 
    assertTrue( out.length == 1 );
    assertEquals( out[0].getChannel(), 1 );
  }

  @Test
    public void dataReplacement() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.noteOn ( 0, null ) );
    em.addToTemplate( EventTemplate.noteOff ( 1, null ) );
    MidiCommand in = new MidiCommand( 0, NOTE_ON, 60 );
    MidiCommand[] out = em.mapEvent( in ); 
    assertTrue( out.length == 1 );
    assertEquals( out[0].getData1(), in.getData1() );
 }

  @Test
  public void multipleToTemplates() {
    EventMapper em = new EventMapper();
    em.setFromTemplate( EventTemplate.noteOn ( 0, null ) );
    em.addToTemplate( EventTemplate.noteOff ( 1, null ) );
    em.addToTemplate( EventTemplate.noteOn ( 2, null ) );
    MidiCommand in = new MidiCommand( 0, NOTE_ON, 60 );
    MidiCommand[] out = em.mapEvent( in ); 
    assertTrue( out.length == 2 );
    assertTrue( out[0].getType() == NOTE_ON || out[1].getType() == NOTE_ON );
    assertTrue( out[0].getType() == NOTE_OFF || out[1].getType() == NOTE_OFF );
  }

}
