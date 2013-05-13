package qualm;


import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import qualm.plugins.CueChangeNotification;
import qualm.plugins.EventMapperNotification;
import qualm.plugins.PatchChangeNotification;

public class PluginManagerTest {

  private PluginManager pluginManager;
  final static AtomicReference<String> lastCue = new AtomicReference<String>(null);
  final static AtomicInteger pluginCount = new AtomicInteger(0);

  @Before
  public void setUp() throws Exception {
    pluginManager = new PluginManager();
  }

  @Test
  public void addPluginToAllLists() throws Exception {
    pluginManager.addPlugin("qualm.PluginManagerTest$AllPlugin");
    assertEquals(1, pluginManager.getCuePlugins().size());
    assertEquals(1, pluginManager.getPatchPlugins().size());
    assertEquals(1, pluginManager.getMapperPlugins().size());
  }
  
  @Test
  public void removePluginFromAllLists() throws Exception {
    addPluginToAllLists();
    pluginManager.removePlugin("qualm.PluginManagerTest$AllPlugin");
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

  private static class AllPlugin 
  implements CueChangeNotification, PatchChangeNotification, EventMapperNotification {

    public AllPlugin() { }

    @Override public void initialize() { 
      pluginCount.incrementAndGet();
    }

    @Override public void shutdown() {
      pluginCount.decrementAndGet();
    }

    @Override
    public void cueChange(MasterController mc) {
      lastCue.set(mc.mainQC().getCurrentCue().getCueNumber());
    }

    @Override
    public void patchChange(int channel, String channelName, Patch patch) { }

    @Override
    public void activeEventMapper(MasterController mc) { }
  }
}
