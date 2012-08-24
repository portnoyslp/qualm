package qualm.delegates;

import qualm.*;

import org.junit.*;
import static org.mockito.Mockito.*;

import static qualm.MidiCommand.*;

public class RolandXV5080DelegateTest {

  QReceiver mockQR;
  ChangeDelegate delegate;

  @Before
  public void setUp() {
    mockQR = mock(QReceiver.class);
    delegate = new RolandXV5080Delegate();
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
    patch.setBank("PrC"); // MSB 87, LSB 66
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 87));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 66));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9));
  }

  @Test
  public void selectUserRhythmSet() {
    Patch patch = new Patch("User Rhythm", 3); 
    patch.setBank("User Rhythm"); // MSB 86, LSB 0
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 86));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 0));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 2));
  }    

  @Test
  public void selectExpansionPatch() {
    Patch patch = new Patch("SR-JV80-05/100", 10); 
    patch.setBank("SR-JV80-05"); // MSB 89, LSB 8
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 89));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 8));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9));
  }    

  @Test
  public void bankLSBChangesForLargeExpansionBank() {
    Patch patch = new Patch("SR-JV80-05/140", 140); 
    patch.setBank("SR-JV80-05"); // MSB 89, LSB 8
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    // bank selection commands provide MSB and LSB; large bank increases LSB by 1
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 89));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 9));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, (140 - 128)-1));
  }

  @Test
  public void selectExpansionRhythmSet() {
    Patch patch = new Patch("SR-JV80-02 Rhythm", 3); 
    patch.setBank("SR-JV80-02 Rhythm"); // MSB 88, LSB 2
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 88));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 2));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 2));
  }    

  @Test
  public void selectGM2Set() {
    Patch patch = new Patch("GM-2/17 60s Organ", 17); 
    patch.setBank("GM-2"); // MSB 121, LSB 2
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 0, 121));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 32, 2));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 16));
  }    

}
