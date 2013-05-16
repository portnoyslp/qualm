package qualm;

import static org.junit.matchers.JUnitMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import qualm.notification.BaseQualmNotifier;
import qualm.notification.CueChange;
import qualm.notification.EventMapActivation;
import qualm.notification.PatchChange;

/**
 * Unit tests for {@link QualmREPL}.
 */
public class QualmREPLTest {
  
  @Mock MasterController controller;
  @Mock NotificationManager notificationManager;
  @Mock PreferencesManager preferencesManager;
  
  StringWriter output;
  Writer input;
  QualmREPL repl;
  Preferences prefs;
  
  @Before
  public void setUp() throws Exception {
    // set up system preferences to use testing class
    System.setProperty("java.util.prefs.PreferencesFactory", "qualm.testing.MapPreferencesFactory");
    prefs = Preferences.userNodeForPackage(QualmREPL.class);

    output = new StringWriter();
    PipedReader reader = new PipedReader();
    input = new PipedWriter(reader);
    setupController();
    repl = new QualmREPL(reader, output, controller);
  }
  
  private void setupController() throws Exception {
    controller = mock(MasterController.class);
    notificationManager = mock(NotificationManager.class);
    preferencesManager = mock(PreferencesManager.class);
    when(controller.getNotificationManager()).thenReturn(notificationManager);
    when(controller.getPreferencesManager()).thenReturn(preferencesManager);
  }
  
  @Test
  public void confirmSetupAsNotifier() throws Exception {
    verify(notificationManager).addNotifier(repl);
  }
  
  @Test
  public void emptyAdvances() throws Exception {
    repl.processLine("");
    repl.processLine(null);
    repl.processLine("]");  // oops; hit the wrong key and rolled to the enter key
    repl.processLine("\\"); // oops; hit the wrong key and rolled to the enter key
    verify(controller, times(4)).advanceMainPatch();
  }

  @Test
  public void advanceCommand() throws Exception {
    repl.processLine("adv K1");
    verify(controller).advanceStream("K1");
  }

  @Test
  public void reverse() throws Exception {
    repl.processLine("rev qs2");
    verify(controller).reverseStream("qs2");
  }

  @Test
  public void dumpCommand() throws Exception {
    when(controller.getQData()).thenReturn(minimalData());
    repl.processLine("dump");
    assertNotNull(output.toString());
  }

  @Test
  public void showXmlCommand() throws Exception {
    when(controller.getQData()).thenReturn(minimalData());
    repl.processLine("showxml");
    assertNotNull(output.toString());
  }

  @Test
  public void resetCommand() throws Exception {
    repl.processLine("reset");
    verify(controller).gotoCue("0.0");
  }

  @Test
  public void showVersion() throws Exception {
    repl.processLine("version");
    assertEquals(Qualm.versionString() + "\n", output.toString());
  }
  
  @Test
  public void endNotificationsUpdatesPrompt() throws Exception {
    QController qc = mock(QController.class);
    when(controller.getControllers()).thenReturn(Collections.singleton(qc));
    when(qc.getCurrentCue()).thenReturn(new Cue("1.1"));
    when(qc.getPendingCue()).thenReturn(new Cue("2.5"));
    repl.endNotifications();
    verify(qc).getCurrentCue();
    verify(qc).getPendingCue();
    assertThat(output.toString(), containsString("1.1-2.5"));
  }
  
  @Test
  public void patchUpdateOutput() throws Exception {
    Patch p = new Patch("Patch1", 23);
    p.setDescription("aPatch");
    repl.patchChange(0, "Ch1", p);
    assertThat(output.toString(), containsString("Ch1 -> aPatch"));
  }
  
  @Test
  public void setMidiOutput() throws Exception {
    repl.processLine("showmidi");
    verify(controller).setDebugMIDI(true);
    repl.processLine("unshowmidi");
    verify(controller).setDebugMIDI(false);
  }

  @Test
  public void addUnknownPlugin() throws Exception {
    String badPlugin = "qualm.plugins.DoesNotExist";
    doThrow(new IllegalArgumentException())
      .when(notificationManager).addNotification(badPlugin);
    repl.processLine("plugin " + badPlugin);
    assertThat(output.toString(), containsString("Unable to create or identify"));
  }
  
  @Test
  public void removeUnknownPlugin() throws Exception {
    repl.processLine("plugin remove qualm.plugins.DoesNotExist");
    assertThat(output.toString(), containsString("Unable to find running"));
  }

  @Test
  public void basicPluginHandling() throws Exception {
    String pluginName = "qualm.QualmREPLTest$AllPlugin";
    repl.processLine("plugin " + pluginName);
    verify(notificationManager).addNotification(pluginName);
    
    repl.processLine("plugin remove " + pluginName);
    verify(controller).removePlugin(pluginName);
  }
  
  @Test
  public void pluginList() throws Exception {
    List<CueChange> plugins = new ArrayList<CueChange>();
    plugins.add(new AllPlugin());
    when(notificationManager.getCueNotifiers()).thenReturn(plugins);

    repl.processLine("plugin list");
    assertThat(output.toString(), containsString("cue qualm.QualmREPLTest$AllPlugin"));
  }

  @Test
  public void preferenceSaving() throws Exception {
    repl.processLine("save");
    verify(preferencesManager).savePreferences();
  }
  
  @Test
  public void preferenceLoadingAtStartup() throws Exception {
    verify(preferencesManager).loadPreferences();
  }
  
  @Test
  public void loadingAndReloading() throws Exception {
    String filename = "tests/qualm/qdl-1.xml";
    repl.processLine("load " + filename);
    repl.processLine("reload");

    verify(controller, times(2)).loadFilename(filename);
  }

  private QData minimalData() {
    return new QDataBuilder()
      .withTitle("Minimal")
      .addMidiChannel( 0, null, "Ch1")
      .addStream(new QStreamBuilder()
		 .addCue(new Cue.Builder()
			 .setCueNumber("1.1")
			 .build())
		 .build())
      .build();
  }
  
  private static class AllPlugin extends BaseQualmNotifier
    implements CueChange, PatchChange, EventMapActivation {

    public AllPlugin() { }
    @Override public void cueChange(MasterController mc) { }
    @Override public void patchChange(int channel, String channelName, Patch patch) { }
    @Override public void activeEventMapper(MasterController mc) { }
  }
}