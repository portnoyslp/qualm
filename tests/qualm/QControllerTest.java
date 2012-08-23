package qualm;

import java.util.*;

import static qualm.MidiCommand.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings( "unchecked" )
public class QControllerTest {

  QController qc;
  QReceiver mockReceiver;
  MasterController mockMaster;
  TestTimeSource timeSource;

  @Before
  public void setup() {
    mockReceiver = mock(QReceiver.class);
    mockMaster = mock(MasterController.class);
    
    QData qd = new QData();
    // Build up the data for use 
    QStream qs = new QStream();
    Cue c = new Cue("1.1");
    ArrayList<QEvent> evList = new ArrayList<QEvent>();
    evList.add(new ProgramChangeEvent( 0, c, new Patch( "P1", 1)));
    c.setEvents(evList);
    ArrayList<Trigger> trigList = new ArrayList<Trigger>();
    trigList.add(new Trigger(EventTemplate.createNoteOnEventTemplate( 0, "c4" )));
    c.setTriggers(trigList);
    qs.addCue(c);
    
    c = new Cue("2.1");
    evList = new ArrayList<QEvent>();
    evList.add(new ProgramChangeEvent( 0, c, new Patch( "P2", 2)));
    c.setEvents(evList);
    trigList = new ArrayList<Trigger>();
    trigList.add(new Trigger(EventTemplate.createNoteOnEventTemplate( 0, "c4" ), true));
    c.setTriggers(trigList);
    qs.addCue(c);
    
    qd.addMidiChannel( 0, null, "Ch1" );
    qd.addCueStream( qs );
    qd.prepareCueStreams();

    qc = new QController( mockReceiver, qs, qd );
    qc.setMaster( mockMaster );

    // override the clock
    timeSource = new TestTimeSource();
    Clock.setTimeSource(timeSource);
  }

  @After
  public void tearDown() {
    Clock.reset();
  }

  @Test
  public void advancePatchSendsToMaster() {
    Cue pending = qc.getPendingCue();
    qc.advancePatch();
    verify(mockMaster).sendEvents( (Collection<QEvent>) anyCollection());
    assertEquals( pending, qc.getCurrentCue() );
  }

  @Test
  public void triggersWork() {
    qc.changesForCue( "1.1" );
    Cue pending = qc.getPendingCue();
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 60, 100 ) );
    verify(mockMaster).sendEvents( anyCollection());
    assertEquals( pending, qc.getCurrentCue() );
  }

  @Test
  public void triggersCauseTimeouts() {
    qc.changesForCue( "1.1" );
    Cue start = qc.getCurrentCue();
    timeSource.setMillis((long) 1000); // set time; we shouldn't accept any further triggers until 2000
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 60, 100 ) );
    verify(mockMaster, times(1)).sendEvents(anyCollection());

    timeSource.setMillis((long) 1500); // not accepting triggers
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 60, 100 ) );
    verifyNoMoreInteractions(mockMaster);

    timeSource.setMillis((long) 2200); // accepting triggers again
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 60, 100 ) ); // this is a reverse trigger, so back to first cue.
    verify(mockMaster, times(2)).sendEvents( anyCollection());

    assertEquals( start, qc.getCurrentCue() );
  }


  class TestTimeSource implements TimeSource {
    long curTime = 0;
    public long millis() { return curTime; }
    public void setMillis(long t) { curTime = t; }
  }
}
