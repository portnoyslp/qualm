package qualm.delegates;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static qualm.MidiCommand.CONTROL_CHANGE;
import static qualm.MidiCommand.PROGRAM_CHANGE;

import org.junit.Before;
import org.junit.Test;

import qualm.ChangeDelegate;
import qualm.MidiCommand;
import qualm.NoteWindowChangeEvent;
import qualm.Patch;
import qualm.ProgramChangeEvent;
import qualm.QReceiver;

public class KorgDelegateTest {

  QReceiver mockQR;
  ChangeDelegate delegate;

  @Before
  public void setUp() {
    mockQR = mock(QReceiver.class);
    delegate = new KorgDelegate();
  }

  @Test
  public void simpleBankName() {
    Patch patch = new Patch("PrgA/10", 10);
    patch.setBank("PrgA"); // MSB 81, no LSB
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 81));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9));
  }

  @Test
  public void testGMBankName() {
    Patch patch = new Patch("GM 10", 10);
    patch.setBank("GM-a"); // MSB 0 LSB 0
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 0));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 0));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9));
  }

  @Test
  public void specifyRBank() {
    Patch patch = new Patch("r:17/10", 10);
    patch.setBank("r:17"); // MSB 17
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 17));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9));
  }

  @Test
  public void specifyYBank() {
    Patch patch = new Patch("y:17/10", 10);
    patch.setBank("y:17"); // MSB 0, LSB 17
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 0));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 17));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9));
  }

  @Test
  public void noteWindowChangeLow() {
    delegate.noteWindowChange( new NoteWindowChangeEvent( 0, null, new Integer(30), null ), mockQR );

    // Korg part delegate expects a SYSEX with the following: F0 42 30 42 12 01 <ch> 15 <note> F7
    byte[] data = new byte[] { (byte) 0xF0, 0x42, 0x30, 0x42, 0x12, 0x01, 0, 0x15, 30, (byte) 0xF7 };
    MidiCommand mc = new MidiCommand();
    mc.setSysex( data );
    verify(mockQR).handleMidiCommand(mc);
  }

  @Test
  public void noteWindowChangeHigh() {
    delegate.noteWindowChange( new NoteWindowChangeEvent( 0, null, null, new Integer(60)), mockQR );

    // Korg part delegate expects a SYSEX with the following: F0 42 30 42 12 01 <ch> 16 <note> F7
    byte[] data = new byte[] { (byte) 0xF0, 0x42, 0x30, 0x42, 0x12, 0x01, 0, 0x16, 60, (byte) 0xF7 };
    MidiCommand mc = new MidiCommand();
    mc.setSysex( data );
    verify(mockQR).handleMidiCommand(mc);
  }

}
