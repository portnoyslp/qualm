package qualm;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import qualm.notification.CueChangeNote;

public class MasterControllerTest {
  MasterController mc;
  QReceiver mockQR;
  
  @Before
  public void setUp() throws Exception {
    mockQR = mock(QReceiver.class);
    mc = new MasterController(mockQR);
  }

  @Test
  public void addingControllerSetsMaster() {
    QController mockControl = mock(QController.class);
    mc.addController(mockControl);
    verify(mockControl).setMaster(mc);
  }
  
  @Test
  public void advanceStreamCallsQController() {
    QController mockControl = mock(QController.class);
    when(mockControl.getTitle()).thenReturn("mock");
    mc.addController(mockControl);
    mc.advanceStream("mock");
    verify(mockControl).advancePatch();
  }
  
  @Test
  public void reverseStreamCallsQController() {
    QController mockControl = mock(QController.class);
    when(mockControl.getTitle()).thenReturn("mock");
    mc.addController(mockControl);
    mc.reverseStream("mock");
    verify(mockControl).reversePatch();
  }
  
  @Test
  public void handleMidiCommandCallsQController() {
    QController mockControl = mock(QController.class);
    mc.addController(mockControl);
    mc.handleMidiCommand( null );
    verify(mockControl).handleMidiCommand( null );
  }

  @Test
  public void cuePluginUpdate() throws Exception {
    CueChangeNote ccn = mock(CueChangeNote.class);
    mc.getPluginManager().addPlugin(ccn);
    mc.getPluginManager().handleCuePlugins(mc);
    verify(ccn).cueChange(mc);
  }
  
  public void loading() throws Exception {
    String filename = "tests/qualm/qdl-1.xml";
    mc.loadFilename(filename);
    // we should have removed all controllers, and added a control for each stream
    verify(mc, times(1)).removeControllers();
    verify(mc, times(2)).addController((QController)anyObject());
  }
}
