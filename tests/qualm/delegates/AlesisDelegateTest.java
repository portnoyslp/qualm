package qualm.delegates;

import qualm.*;

import org.junit.*;
import static org.mockito.Mockito.*;

import static qualm.MidiCommand.*;

public class AlesisDelegateTest {

  QReceiver mockQR;
  ChangeDelegate delegate;

  @Before
  public void setUp() {
    mockQR = mock(QReceiver.class);
    delegate = new AlesisDelegate();
  }

  @Test
  public void simpleBankName() {
    Patch patch = new Patch("Pr2/10", 10);
    patch.setBank("Pr2"); // MSB 2
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    // Alesis patches are zero-based like MIDI, so the patch numbers match
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 2));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 10));
  }

  @Test
  public void cardBankName() {
    Patch patch = new Patch("GM 10", 10);
    patch.setBank("Card1"); // MSB 0 LSB 0
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 5));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 10));
  }

  @Test
  public void noteWindowChangeLow() {
    delegate.noteWindowChange( new NoteWindowChangeEvent( 0, null, new Integer(30), null ), mockQR );

    // Alesis sends a SYSEX with the following: F0 00 00 0E 0E 10 24 00
    // <0[channel][bot/top]x> <0xxxxxxx> where x is the value, and bot/top is whether its the low or hi range.
    // and then F7
    // Also, Alesis uses C3 (MIDI 48) as middle C, so we have to adjust the note range values by 12.
    byte[] data = new byte[] { (byte) 0xF0, 0, 0, 0x0E, 0x0E, 0x10, 0x24, 0, 0, 30+12, (byte) 0xF7 };
    MidiCommand mc = new MidiCommand();
    mc.setSysex( data );
    verify(mockQR).handleMidiCommand(mc);
  }

  @Test
  public void noteWindowChangeHigh() {
    delegate.noteWindowChange( new NoteWindowChangeEvent( 0, null, null, new Integer(60)), mockQR );

    // Alesis sends a SYSEX with the following: F0 00 00 0E 0E 10 24 00
    // <0[channel][bot/top]x> <0xxxxxxx> where x is the value, and bot/top is whether its the low or hi range.
    // and then F7
    // Also, Alesis uses C3 (MIDI 48) as middle C, so we have to adjust the note range values by 12.
    byte[] data = new byte[] { (byte) 0xF0, 0, 0, 0x0E, 0x0E, 0x10, 0x24, 0, 2, 60+12, (byte) 0xF7 };
    MidiCommand mc = new MidiCommand();
    mc.setSysex( data );
    verify(mockQR).handleMidiCommand(mc);
  }

}
