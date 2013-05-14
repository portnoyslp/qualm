package qualm;

import static org.junit.matchers.JUnitMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import qualm.plugins.CueChangeNotification;
import qualm.plugins.PatchChangeNotification;

public class PreferencesManagerTest {
  Preferences prefs;
  private static final String PLUGIN_KEY = "plugins";
  @Mock MasterController controller;
  @Mock PluginManager pluginManager;
  PreferencesManager preferencesManager;

  @Before
  public void setUp() throws Exception {
    // set up system preferences to use testing class
    System.setProperty("java.util.prefs.PreferencesFactory", "qualm.testing.MapPreferencesFactory");
    prefs = Preferences.userNodeForPackage(QualmREPL.class);
    
    controller = mock(MasterController.class);
    pluginManager = mock(PluginManager.class);
    when(controller.getPluginManager()).thenReturn(pluginManager);
    
    preferencesManager= new PreferencesManager();
    preferencesManager.setController(controller);
  }

  @Test
  public void preferenceLoading() throws Exception {
    prefs.put(PLUGIN_KEY, "qualm.QualmREPLTest$AllPlugin");
    preferencesManager.loadPreferences();
    verify(pluginManager).addPlugin("qualm.QualmREPLTest$AllPlugin");
  }
  
  @Test
  public void preferenceSaving() throws Exception {
    List<CueChangeNotification> cuePlugins = new ArrayList<CueChangeNotification>();
    List<PatchChangeNotification> patchPlugins = new ArrayList<PatchChangeNotification>();
    cuePlugins.add(new CuePlugin());
    patchPlugins.add(new PatchPlugin());
    when(pluginManager.getCuePlugins()).thenReturn(cuePlugins);
    when(pluginManager.getPatchPlugins()).thenReturn(patchPlugins);

    preferencesManager.savePreferences();
    assertThat(prefs.get(PLUGIN_KEY, ""), containsString("qualm.PreferencesManagerTest$CuePlugin"));
    assertThat(prefs.get(PLUGIN_KEY, ""), containsString("qualm.PreferencesManagerTest$PatchPlugin"));
  }
  
  private static class CuePlugin implements CueChangeNotification {
    @Override public void cueChange(MasterController mc) { }

    @Override public void initialize() { }
    @Override public void shutdown() { }
  }

  private static class PatchPlugin implements PatchChangeNotification {
    @Override public void patchChange(int channel, String chName, Patch p) { }

    @Override public void initialize() { }
    @Override public void shutdown() { }
  }
}
