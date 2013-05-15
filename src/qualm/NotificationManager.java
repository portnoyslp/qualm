package qualm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import qualm.notification.CueChange;
import qualm.notification.EventMapActivation;
import qualm.notification.PatchChange;
import qualm.notification.QualmNotification;

public class NotificationManager {
  public Collection<CueChange> cuePlugins = new ArrayList<CueChange>();
  public Collection<PatchChange> patchPlugins = new ArrayList<PatchChange>();
  public Collection<EventMapActivation> mapperPlugins = new ArrayList<EventMapActivation>();
  
  public Collection<CueChange> getCuePlugins() {
    return cuePlugins;
  }
  
  public Collection<PatchChange> getPatchPlugins() {
    return patchPlugins;
  }
  
  public Collection<EventMapActivation> getMapperPlugins() {
    return mapperPlugins;
  }
  
  private void addCuePlugin(CueChange plugin) {
    cuePlugins.add(plugin);
  }
  
  private void addPatchPlugin(PatchChange plugin) {
    patchPlugins.add(plugin);
  }
  
  private void addMapperPlugin(EventMapActivation plugin) {
    mapperPlugins.add(plugin);
  }

  public void handleCuePlugins(MasterController masterController) {
    for (CueChange plugin : getCuePlugins()) {
      plugin.cueChange(masterController);
    }
  }
  
  public void handlePatchPlugins(int ch, String name, Patch p) {
    for (PatchChange plugin : getPatchPlugins()) {
      plugin.patchChange(ch,name,p);
    }
  }
  
  public void handleMapperPlugins(MasterController masterController) {
    for (EventMapActivation plugin : getMapperPlugins()) {
      plugin.activeEventMapper(masterController);
    }
  }
  
  public void addPlugin(String name) {
    // should be a class spec; try instantiating an object for this
    Class<?> cls;
    try {
      cls = Class.forName(name);
      if (QualmNotification.class.isAssignableFrom(cls)) {
        boolean added = addPlugin((QualmNotification)cls.newInstance());
        if (added) {
          return;
        }
      }
    } catch (Exception e) { }
    
    throw new IllegalArgumentException("Could not start plugin '" + name + "'");
  }

  public boolean addPlugin(QualmNotification qp) throws ClassNotFoundException {
    Class<? extends QualmNotification> cls = qp.getClass();
    qp.initialize();
    boolean added = false;
    if (CueChange.class.isAssignableFrom(cls)) {
      addCuePlugin( (CueChange) qp );
      added = true;
    }
    if (PatchChange.class.isAssignableFrom(cls)) {
      addPatchPlugin( (PatchChange) qp );
      added = true;
    }
    if (EventMapActivation.class.isAssignableFrom(cls)) {
      addMapperPlugin( (EventMapActivation) qp );
      added = true;
    }
    return added;
  }

  public Set<QualmNotification> removePlugin(String name) {
    Set<QualmNotification> removed = new HashSet<QualmNotification>();
  
    Iterator<CueChange> cuePluginIter = getCuePlugins().iterator();
    while(cuePluginIter.hasNext()) {
      CueChange obj = cuePluginIter.next();
      // remove plugins that match the name.
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        cuePluginIter.remove();
      }
    }
    Iterator<PatchChange> patchPluginIter = getPatchPlugins().iterator();
    while (patchPluginIter.hasNext()) {
      // remove plugins that match the name.
      PatchChange obj = patchPluginIter.next();
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        patchPluginIter.remove();
      }
    }
    Iterator<EventMapActivation> mapperPluginIter = getMapperPlugins().iterator();
    while(mapperPluginIter.hasNext()) {
      EventMapActivation obj = mapperPluginIter.next();
      // remove plugins that match the name.
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        mapperPluginIter.remove();
      }
    }

    // shutdown all the removed plugins
    for (QualmNotification qp : removed) 
      qp.shutdown();
  
    return removed;
  }

}