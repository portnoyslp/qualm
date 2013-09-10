package qualm;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

/**
 * Unit tests for {@link JavaMidiReceiver}.
 */
public class JavaMidiReceiverTest {

  Transmitter mockTransmitter;
  Receiver mockReceiver;
  QReceiver mockQR;
  JavaMidiReceiver jmr;
  TestTimeSource timeSource = new TestTimeSource();
  
  @Before
  public void setUp() {
    mockTransmitter = mock(Transmitter.class);
    mockReceiver = mock(Receiver.class);
    jmr = new JavaMidiReceiver(mockTransmitter, mockReceiver);
    mockQR = mock(QReceiver.class);
    jmr.setTarget( mockQR );
    Clock.setTimeSource(timeSource);
  }

  @Test
  public void sendingJavaMidiSendsToQReceiver() {
    ShortMessage sm = new ShortMessage();
    try {
      sm.setMessage( ShortMessage.NOTE_ON, 0, 60, 100 );
    } catch (Exception e) { } 
    jmr.send( sm, -1 );

    MidiCommand playMiddleC = new MidiCommand( 0, MidiCommand.NOTE_ON, 60, 100 );
    verify(mockQR).handleMidiCommand( playMiddleC );
  }

  @Test
  public void sendingMidiCommandSendsToJavaReceiver() {
    MidiCommand playMiddleC = new MidiCommand( 0, MidiCommand.NOTE_ON, 60, 100 );
    jmr.handleMidiCommand(playMiddleC);

    ShortMessage sm = new ShortMessage();
    try {
      sm.setMessage( ShortMessage.NOTE_ON, 0, 60, 100 );
    } catch (Exception e) { } 
    
    verify(mockReceiver).send( argThat(new MidiMatcher(sm)), anyLong() );
  }

  @Test
  public void sendingJavaSysexToQReceiver() {
    byte[] data = new byte[]{ (byte)0xF0, 1, 2, 4, 8, (byte)0xF7 };

    SysexMessage sm = new SysexMessage();
    try { sm.setMessage(data, data.length); }
    catch (Exception e) {}
    jmr.send( sm, -1 );

    MidiCommand sysexCmd = new MidiCommand();
    sysexCmd.setSysex(data);

    verify(mockQR).handleMidiCommand( sysexCmd );
  }

  @Test
  public void sendingSysexToJavaReceiver() {
    byte[] data = new byte[]{ (byte)0xF0, 1, 2, 4, 8, (byte)0xF7 };
    MidiCommand sysexCmd = new MidiCommand();
    sysexCmd.setSysex(data);
    jmr.handleMidiCommand(sysexCmd);

    SysexMessage sm = new SysexMessage();
    try { sm.setMessage(data, data.length); }
    catch (Exception e) {}

    verify(mockReceiver).send( argThat(new MidiMatcher(sm)), anyLong() );
  }
  
  @Test
  public void sysexDelayWhenSpecified() throws Exception {
    jmr.setSysexDelay(1000);
    timeSource.setMillis(0);
    
    byte[] data = new byte[]{ (byte)0xF0, 1, 2, 4, 8, (byte)0xF7 };
    MidiCommand sysexCmd = new MidiCommand();
    sysexCmd.setSysex(data);
    jmr.handleMidiCommand(sysexCmd);
    
    SysexMessage sm = new SysexMessage();
    try { sm.setMessage(data, data.length); }
    catch (Exception e) {}
    
    verify(mockReceiver).send( argThat(new MidiMatcher(sm)), anyLong() );

    // immediately send another message. This will block, so do it in a separate thread.
    final MidiCommand sysexCmdCopy = sysexCmd; 
    Thread sendSecondSysex = new Thread(new Runnable() {
      public void run() {
        jmr.handleMidiCommand(sysexCmdCopy);
      }
    });
    sendSecondSysex.start();
    // still should only have sent one sysex.
    verify(mockReceiver).send( argThat(new MidiMatcher(sm)), anyLong() );
    
    // time passes...
    timeSource.setMillis(1100);
    sendSecondSysex.join(200); // wait a little while for the spawned thread to finish
    assertTrue(!sendSecondSysex.isAlive());
    // should now have sent two sysex.
    verify(mockReceiver, times(2)).send( argThat(new MidiMatcher(sm)), anyLong() );
  }

  @Test
  public void closeClosesBothTransmitterAndReceiver() {
    jmr.close();
    verify(mockTransmitter).close();
    verify(mockReceiver).close();
  }

  /* We create a MidiMatcher method because MidiMessages don't have equals() */
  class MidiMatcher extends ArgumentMatcher<MidiMessage> {
    private final MidiMessage target;
    public MidiMatcher(MidiMessage mm) {
      target = mm;
    }

    public boolean matches( Object obj ) {
      MidiMessage mm = (MidiMessage) obj;
      return (mm.getLength() == target.getLength() 
              && Arrays.equals(mm.getMessage(), target.getMessage()));
    }
  }

  class TestTimeSource implements TimeSource {
    long curTime = 0;
    public long millis() { return curTime; }
    public void setMillis(long t) { curTime = t; }
  }
}
