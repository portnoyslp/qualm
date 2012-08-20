package qualm.delegates;

import qualm.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import static qualm.MidiCommand.*;

public class StandardDelegateTest {

  QReceiver mockQR;
  ChangeDelegate delegate;

  @Before
  public void setUp() {
    mockQR = mock(QReceiver.class);
    delegate = new StandardDelegate();
  }

  @Test
  public void setPatchVolume() {
    Patch patch = new Patch("P1", 10);
    patch.setVolume(new Integer(40));
    delegate.patchChange( new ProgramChangeEvent( 0, null, patch ), mockQR );

    // note that patch numbers are 1 less than the specified.
    verify(mockQR).handleMidiCommand(new MidiCommand(0, PROGRAM_CHANGE, 9, 0));
    verify(mockQR).handleMidiCommand(new MidiCommand(0, CONTROL_CHANGE, 7, 40));

  }

}
