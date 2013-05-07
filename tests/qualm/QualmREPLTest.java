package qualm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Unit tests for {@link QualmREPL} 
 */
public class QualmREPLTest {
  
  @Mock MasterController controller;
  StringWriter output;
  Writer input;
  QualmREPL repl;
  
  @Before
  public void setUp() throws Exception {
    output = new StringWriter();
    PipedReader reader = new PipedReader();
    input = new PipedWriter(reader);
    repl = new QualmREPL(reader, output);
    setupController();
  }

  private void setupController() throws Exception {
    controller = mock(MasterController.class); 
    repl.setMasterController(controller);
  }
  
  @Test
  public void confirmControllerSetup() throws Exception {
    verify(controller).setREPL(repl);
  }
  
  @Test
  public void showVersion() throws Exception {
    repl.processLine("version");
    assertEquals(Qualm.versionString() + "\n", output.toString());
  }
  
  @Test
  public void setMidiOutput() throws Exception {
    repl.processLine("showmidi");
    verify(controller).setDebugMIDI(true);
    repl.processLine("unshowmidi");
    verify(controller).setDebugMIDI(false);
  }
}