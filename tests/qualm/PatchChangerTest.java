package qualm;

import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PatchChangerTest {

  ChangeDelegate mockDelegate;

  @Before
  public void setup() {
    mockDelegate = mock(ChangeDelegate.class);
    PatchChanger.installDelegateForChannel(0, mockDelegate, "MockDelegate");
  }

  @Test
  public void confirmDelegateInstalled() {
    assertEquals("MockDelegate", PatchChanger.getRequestedDeviceForChannel(0));
  }

  @Test
  public void confirmPatchChangeMessagesSent() {
    ProgramChangeEvent pce = new ProgramChangeEvent( 0, null, null ); // don't care about Cue or Patch
    PatchChanger.patchChange(pce, null);
    verify(mockDelegate).patchChange( pce, null );
  }

  @Test
  public void confirmNoteWindowMessagesSent() {
    NoteWindowChangeEvent nwce = new NoteWindowChangeEvent( 0, null, 
							    new Integer(30), 
							    new Integer(50) ); 
    PatchChanger.noteWindowChange(nwce, null);
    verify(mockDelegate).noteWindowChange( nwce, null );
  }

  @Test(expected=RuntimeException.class)
  public void exceptionForUnknownChannel() {
    ProgramChangeEvent pce = new ProgramChangeEvent( 1, null, null ); // don't care about Cue or Patch
    PatchChanger.patchChange(pce, null);
  }

}
