package qualm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import qualm.plugins.CueChangeNotification;
import qualm.plugins.EventMapperNotification;
import qualm.plugins.PatchChangeNotification;
import qualm.plugins.QualmPlugin;

public class PluginManager {
  public List<CueChangeNotification> cuePlugins = new ArrayList<CueChangeNotification>();
  public List<PatchChangeNotification> patchPlugins = new ArrayList<PatchChangeNotification>();
  public List<EventMapperNotification> mapperPlugins = new ArrayList<EventMapperNotification>();
  
  public List<CueChangeNotification> getCuePlugins() {
    return cuePlugins;
  }
  
  public List<PatchChangeNotification> getPatchPlugins() {
    return patchPlugins;
  }
  
  public List<EventMapperNotification> getMapperPlugins() {
    return mapperPlugins;
  }
  
  public void addCuePlugin(CueChangeNotification plugin) {
    getCuePlugins().add(plugin);
  }
  
  public void addPatchPlugin(PatchChangeNotification plugin) {
    getPatchPlugins().add(plugin);
  }
  
  public void addMapperPlugin(EventMapperNotification plugin) {
    getMapperPlugins().add(plugin);
  }

  public void handleCuePlugins(MasterController masterController) {
    for (CueChangeNotification plugin : getCuePlugins()) {
      plugin.cueChange(masterController);
    }
  }
  
  public void handlePatchPlugins(int ch, String name, Patch p) {
    for (PatchChangeNotification plugin : getPatchPlugins()) {
      plugin.patchChange(ch,name,p);
    }
  }
  
  public void handleMapperPlugins(MasterController masterController) {
    for (EventMapperNotification plugin : getMapperPlugins()) {
      plugin.activeEventMapper(masterController);
    }
  }
  
  public void addPlugin(String name) {
    // should be a class spec; try instantiating an object for this
    Class<?> cls;
    try {
      cls = Class.forName(name);
      if (Class.forName("qualm.plugins.QualmPlugin").isAssignableFrom(cls)) {
        addPlugin((QualmPlugin)cls.newInstance());
      }
    } catch (Exception e) { }
    
    throw new IllegalArgumentException("Could not start plugin '" + name + "'");
  }

  private boolean addPlugin(QualmPlugin qp) throws ClassNotFoundException {
    Class<? extends QualmPlugin> cls = qp.getClass();
    qp.initialize();
    boolean added = false;
    if (Class.forName("qualm.plugins.CueChangeNotification").isAssignableFrom(cls)) {
      addCuePlugin( (CueChangeNotification) qp );
      added = true;
    }
    if (Class.forName("qualm.plugins.PatchChangeNotification").isAssignableFrom(cls)) {
      addPatchPlugin( (PatchChangeNotification) qp );
      added = true;
    }
    if (Class.forName("qualm.plugins.EventMapperNotification").isAssignableFrom(cls)) {
      addMapperPlugin( (EventMapperNotification) qp );
      added = true;
    }
    return added;
  }

  public Set<QualmPlugin> removePlugin(String name) {
    Set<QualmPlugin> removed = new HashSet<QualmPlugin>();
  
    Iterator<CueChangeNotification> cuePluginIter = getCuePlugins().iterator();
    while(cuePluginIter.hasNext()) {
      CueChangeNotification obj = cuePluginIter.next();
      // remove plugins that match the name.
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        cuePluginIter.remove();
      }
    }
    Iterator<PatchChangeNotification> patchPluginIter = getPatchPlugins().iterator();
    while (patchPluginIter.hasNext()) {
      // remove plugins that match the name.
      PatchChangeNotification obj = patchPluginIter.next();
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        patchPluginIter.remove();
      }
    }
  
    // shutdown all the removed plugins
    for (QualmPlugin qp : removed) 
      qp.shutdown();
  
    return removed;
  }

}