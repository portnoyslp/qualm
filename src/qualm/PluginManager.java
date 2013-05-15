package qualm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import qualm.notification.CueChangeNotification;
import qualm.notification.EventMapperNotification;
import qualm.notification.PatchChangeNotification;
import qualm.notification.QualmPlugin;

public class PluginManager {
  public Collection<CueChangeNotification> cuePlugins = new ArrayList<CueChangeNotification>();
  public Collection<PatchChangeNotification> patchPlugins = new ArrayList<PatchChangeNotification>();
  public Collection<EventMapperNotification> mapperPlugins = new ArrayList<EventMapperNotification>();
  
  public Collection<CueChangeNotification> getCuePlugins() {
    return cuePlugins;
  }
  
  public Collection<PatchChangeNotification> getPatchPlugins() {
    return patchPlugins;
  }
  
  public Collection<EventMapperNotification> getMapperPlugins() {
    return mapperPlugins;
  }
  
  private void addCuePlugin(CueChangeNotification plugin) {
    cuePlugins.add(plugin);
  }
  
  private void addPatchPlugin(PatchChangeNotification plugin) {
    patchPlugins.add(plugin);
  }
  
  private void addMapperPlugin(EventMapperNotification plugin) {
    mapperPlugins.add(plugin);
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
      if (QualmPlugin.class.isAssignableFrom(cls)) {
        boolean added = addPlugin((QualmPlugin)cls.newInstance());
        if (added) {
          return;
        }
      }
    } catch (Exception e) { }
    
    throw new IllegalArgumentException("Could not start plugin '" + name + "'");
  }

  public boolean addPlugin(QualmPlugin qp) throws ClassNotFoundException {
    Class<? extends QualmPlugin> cls = qp.getClass();
    qp.initialize();
    boolean added = false;
    if (CueChangeNotification.class.isAssignableFrom(cls)) {
      addCuePlugin( (CueChangeNotification) qp );
      added = true;
    }
    if (PatchChangeNotification.class.isAssignableFrom(cls)) {
      addPatchPlugin( (PatchChangeNotification) qp );
      added = true;
    }
    if (EventMapperNotification.class.isAssignableFrom(cls)) {
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
    Iterator<EventMapperNotification> mapperPluginIter = getMapperPlugins().iterator();
    while(mapperPluginIter.hasNext()) {
      EventMapperNotification obj = mapperPluginIter.next();
      // remove plugins that match the name.
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        mapperPluginIter.remove();
      }
    }

    // shutdown all the removed plugins
    for (QualmPlugin qp : removed) 
      qp.shutdown();
  
    return removed;
  }

}