package qualm;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
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

public class JavaMidiReceiverTest {

  Transmitter mockTransmitter;
  Receiver mockReceiver;
  QReceiver mockQR;
  JavaMidiReceiver jmr;

  /* Uses Mockito to create Transmitter and Receiver mocks, and uses
   * them to ensure that JMR is mapping the events correctly. */
  
  @Before
  public void setUp() {
    mockTransmitter = mock(Transmitter.class);
    mockReceiver = mock(Receiver.class);
    jmr = new JavaMidiReceiver(mockTransmitter, mockReceiver);
    mockQR = mock(QReceiver.class);
    jmr.setTarget( mockQR );
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

}
