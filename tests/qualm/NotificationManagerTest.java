package qualm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import qualm.notification.CueChangeNote;
import qualm.notification.EventMapActivation;
import qualm.notification.PatchChangeNote;


public class NotificationManagerTest {

  private NotificationManager pluginManager;

  @Before
  public void setUp() throws Exception {
    pluginManager = new NotificationManager();
  }

  @Test
  public void addPluginToAllLists() throws Exception {
    pluginManager.addPlugin(AllPlugin.class.getName());
    assertEquals(1, pluginManager.getCuePlugins().size());
    assertEquals(1, pluginManager.getPatchPlugins().size());
    assertEquals(1, pluginManager.getMapperPlugins().size());
  }
  
  @Test
  public void removePluginFromAllLists() throws Exception {
    addPluginToAllLists();
    pluginManager.removePlugin(AllPlugin.class.getName());
    assertEquals(0, pluginManager.getCuePlugins().size());
    assertEquals(0, pluginManager.getPatchPlugins().size());
    assertEquals(0, pluginManager.getMapperPlugins().size());
  }

  @Test
  public void addDirect() throws Exception {
    AllPlugin plugin = new AllPlugin();
    pluginManager.addPlugin(plugin);
    assertEquals(1, pluginManager.getCuePlugins().size());
    assertEquals(plugin, pluginManager.getCuePlugins().iterator().next());
  }
  
  @Test
  public void pluginInitialized() throws Exception {
    CueChangeNote ccn = mock(CueChangeNote.class);
    pluginManager.addPlugin(ccn);
    verify(ccn).initialize();
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void unknownPlugin() throws Exception {
    pluginManager.addPlugin("qualm.UnknownPlugin");
  }
  
  @Test
  public void pluginShutdown() throws Exception {
    CueChangeNote ccn = mock(CueChangeNote.class);
    pluginManager.addPlugin(ccn);
    pluginManager.removePlugin(ccn.getClass().getName());
    verify(ccn).shutdown();
  }
  
  @Test
  public void handleCuePlugin() throws Exception {
    CueChangeNote ccn = mock(CueChangeNote.class);
    pluginManager.addPlugin(ccn);
    MasterController mc = mock(MasterController.class);
    
    pluginManager.handleCuePlugins(mc);
    verify(ccn).cueChange(mc);
  }
  
  @Test
  public void handlePatchPlugin() throws Exception {
    PatchChangeNote pcn = mock(PatchChangeNote.class);
    pluginManager.addPlugin(pcn);
    Patch p = new Patch("P1", 3);
    pluginManager.handlePatchPlugins(1, "K1", p);
    verify(pcn).patchChange(1, "K1", p);
  }
  
  @Test
  public void handleMapperPlugin() throws Exception {
    EventMapActivation emn = mock(EventMapActivation.class);
    pluginManager.addPlugin(emn);
    
    MasterController mc = mock(MasterController.class);
    pluginManager.handleMapperPlugins(mc);
    verify(emn).activeEventMapper(mc);
  }

  private static class AllPlugin 
  implements CueChangeNote, PatchChangeNote, EventMapActivation {

    public AllPlugin() { }

    @Override public void initialize() { }

    @Override public void shutdown() { }

    @Override public void cueChange(MasterController mc) { }

    @Override public void patchChange(int channel, String channelName, Patch patch) { }

    @Override public void activeEventMapper(MasterController mc) { }
  }
}
