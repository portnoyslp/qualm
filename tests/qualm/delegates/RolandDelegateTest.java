package qualm.delegates;

import qualm.*;

import org.junit.*;
import static org.mockito.Mockito.*;

import static qualm.MidiCommand.*;

public class RolandDelegateTest {

  QReceiver mockQR;
  ChangeDelegate delegate;

  @Before
  public void setUp() {
    mockQR = mock(QReceiver.class);
    delegate = new RolandDelegate();
  }

  @Test
  public void specifiedBankName() {
    Patch patch = new Patch("Test 10", 10);
    patch.setBank("81/2"); // MSB 81, LSB 2
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    // bank selection commands provide MSB and LSB
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 81));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 2));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9));
    // note that patch numbers are zero-based, so it's 1 less than specified.
  }

  @Test
  public void bankSelectWorks() {
    Patch patch = new Patch("PrC/10", 10);
    patch.setBank("PrC"); // MSB 81, LSB 2
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 81));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 2));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9));
  }

  @Test
  public void bankLSBChangesForLargePatchBank() {
    Patch patch = new Patch("XpA/140", 140); 
    patch.setBank("XpA"); // MSB 84, LSB 0
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    // bank selection commands provide MSB and LSB
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 84));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 1)); // LSB increased by 1
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, (140 - 128)-1));
  }

}
