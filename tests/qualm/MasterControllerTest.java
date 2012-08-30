package qualm;

import static org.mockito.Mockito.*;
import org.junit.*;

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

}
