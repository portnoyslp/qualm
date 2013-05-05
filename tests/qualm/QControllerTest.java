package qualm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static qualm.MidiCommand.NOTE_ON;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

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
    qs.addCue(firstCue());
    qs.addCue(secondCue());
    
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
    verify(mockMaster).sendEvents(argThat(containsEvents(firstCue().getEvents())));
    assertEquals( pending, qc.getCurrentCue() );
  }

  @Test
  public void triggersWork() {
    qc.changesForCue( "1.1" );
    Cue pending = qc.getPendingCue();
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 60, 100 ) );
    verify(mockMaster).sendEvents(argThat(containsEvents(secondCue().getEvents())));
    assertEquals( pending, qc.getCurrentCue() );
  }

  @Test
  public void triggersCauseTimeouts() {
    qc.changesForCue( "1.1" );
    Cue first = qc.getCurrentCue();
    Cue second = qc.getPendingCue();
    
    timeSource.setMillis((long) 1000); // set time; we shouldn't accept any further triggers until 2000
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 60, 100 ) );
    verify(mockMaster).sendEvents(argThat(containsEvents(secondCue().getEvents())));
    assertEquals( second, qc.getCurrentCue() );

    timeSource.setMillis((long) 1500); // not accepting triggers
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 60, 100 ) );
    verifyNoMoreInteractions(mockMaster);
    assertEquals( second, qc.getCurrentCue() );

    timeSource.setMillis((long) 2200); // accepting triggers again
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 60, 100 ) ); // this is a reverse trigger, so back to first cue.
    verify(mockMaster).sendEvents(argThat(containsEvents(firstCue().getEvents())));

    assertEquals( first, qc.getCurrentCue() );
  }

  @Test
  public void triggerWithDelay() {
    // We use a 200ms timeout for verify's here, because the extra thread might take a while.
    qc.changesForCue( "1.1" );
    Cue pending = qc.getPendingCue();
    timeSource.setMillis((long) 1000); // set time
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 108, 100 ) ); // delay of 10 seconds = 10000
    verifyNoMoreInteractions(mockMaster);

    timeSource.setMillis((long) 5000); // time not there, we shouldn't see any triggers
    ContainsEventMatcher matchesSecondCue = containsEvents(secondCue().getEvents());
    verify(mockMaster, timeout(200).never()).sendEvents(argThat(matchesSecondCue));

    timeSource.setMillis((long) 12000); // by now we should have triggered
    verify(mockMaster, timeout(200).times(1)).sendEvents(argThat(matchesSecondCue));

    assertEquals( pending, qc.getCurrentCue() );
  }

  @Test
  public void doubleTriggerGetsIgnored() {
    // We use a 200ms timeout for verify's here, because the extra thread might take a while.
    qc.changesForCue( "1.1" );
    Cue pending = qc.getPendingCue();
    timeSource.setMillis((long) 1000); // set time
    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 108, 100 ) ); // delay of 10 seconds = 10000
    verifyNoMoreInteractions(mockMaster);

    ContainsEventMatcher matchesSecondCue = containsEvents(secondCue().getEvents());
    timeSource.setMillis((long) 5000); // time not there, we shouldn't see any triggers
    verify(mockMaster, timeout(200).never()).sendEvents(argThat(matchesSecondCue));

    qc.handleMidiCommand( new MidiCommand( 0, NOTE_ON, 108, 100 ) ); // same trigger, should get ignored.
    verify(mockMaster, timeout(200).never()).sendEvents(argThat(matchesSecondCue));

    timeSource.setMillis((long) 16000); // by now we should have triggered, just the once.
    verify(mockMaster, timeout(200).times(1)).sendEvents(argThat(matchesSecondCue));

    assertEquals( pending, qc.getCurrentCue() );
  }

  private Cue firstCue() {
    Cue c = new Cue("1.1");
    ArrayList<QEvent> evList = new ArrayList<QEvent>();
    evList.add(new ProgramChangeEvent( 0, c, new Patch( "P1", 1)));
    c.setEvents(evList);
    ArrayList<Trigger> trigList = new ArrayList<Trigger>();
    trigList.add(new Trigger(EventTemplate.noteOn( 0, "c4" )));
    Trigger t = new Trigger(EventTemplate.noteOn( 0, "c8" ));
    t.setDelay(10000);
    trigList.add(t);
    c.setTriggers(trigList);
    return c;
  }

  private Cue secondCue() {
    ArrayList<QEvent> evList = new ArrayList<QEvent>();
    ArrayList<Trigger> trigList = new ArrayList<Trigger>();
    Cue c = new Cue("2.1");
    evList.add(new ProgramChangeEvent( 0, c, new Patch( "P2", 2)));
    c.setEvents(evList);
    trigList.add(new Trigger(EventTemplate.noteOn( 0, "c4" ), Trigger.REVERSE));
    c.setTriggers(trigList);
    return c;
  }


  private ContainsEventMatcher containsEvents(Collection<QEvent> evts) {
    return new ContainsEventMatcher(evts);
  }

  class TestTimeSource implements TimeSource {
    long curTime = 0;
    public long millis() { return curTime; }
    public void setMillis(long t) { curTime = t; }
  }
  
  class ContainsEventMatcher extends ArgumentMatcher<Collection<QEvent>> {
    final Collection<QEvent> events;
    
    public ContainsEventMatcher(Collection<QEvent> evts) {
      this.events = evts;
    }
    
    public void describeTo(org.hamcrest.Description desc) {
      desc.appendText("matching events " + events);
    }
    
    public boolean matches(Object obj) {
      @SuppressWarnings("unchecked")
      Collection<QEvent> coll = (Collection<QEvent>)obj;
      for (QEvent match : events) {
        boolean matchFound = false;
        for (QEvent input : coll) {
          if (input instanceof ProgramChangeEvent && match instanceof ProgramChangeEvent) {
            ProgramChangeEvent pceMatch = (ProgramChangeEvent) match;
            ProgramChangeEvent inputMatch = (ProgramChangeEvent) input;
            if (pceMatch.getChannel() == inputMatch.getChannel() &&
                pceMatch.getPatch().equals(inputMatch.getPatch())) {
              matchFound = true;
              break;
            }
          }
        }
        if (!matchFound) return false;
      }
      return true;
    }
  }
}
