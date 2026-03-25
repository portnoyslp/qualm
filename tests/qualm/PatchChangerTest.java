package qualm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class PatchChangerTest {

  PatchChanger patchChanger;
  ChangeDelegate mockDelegate;

  @Before
  public void setup() {
    patchChanger = new PatchChanger();
    mockDelegate = mock(ChangeDelegate.class);
    patchChanger.installDelegateForChannel(0, mockDelegate, "MockDelegate");
  }

  @Test
  public void confirmDelegateInstalled() {
    assertEquals("MockDelegate", patchChanger.getRequestedDeviceForChannel(0));
  }

  @Test
  public void confirmPatchChangeMessagesSent() {
    ProgramChangeEvent pce = new ProgramChangeEvent( 0, null, null );
    patchChanger.patchChange(pce, null);
    verify(mockDelegate).patchChange( pce, null );
  }

  @Test
  public void confirmNoteWindowMessagesSent() {
    NoteWindowChangeEvent nwce = new NoteWindowChangeEvent( 0, null, 30, 50 );
    patchChanger.noteWindowChange(nwce, null);
    verify(mockDelegate).noteWindowChange( nwce, null );
  }

  @Test(expected=RuntimeException.class)
  public void exceptionForUnknownChannel() {
    ProgramChangeEvent pce = new ProgramChangeEvent( 1, null, null );
    patchChanger.patchChange(pce, null);
  }

  @Test
  public void partialDelegateNamesHandled() {
    patchChanger.addPatchChanger(1, "Alesis 8.1");
    assertEquals(qualm.delegates.AlesisDelegate.class, patchChanger.delegateForChannel(1).getClass());
  }

  @Test(expected=RuntimeException.class)
  public void unknownDelegate() {
    patchChanger.addPatchChanger(1, "Foobar Baz");
  }
}
