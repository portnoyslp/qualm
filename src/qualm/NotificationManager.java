package qualm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import qualm.notification.CueChangeNote;
import qualm.notification.EventMapActivation;
import qualm.notification.PatchChangeNote;
import qualm.notification.QualmNotification;

public class NotificationManager {
  public Collection<CueChangeNote> cuePlugins = new ArrayList<CueChangeNote>();
  public Collection<PatchChangeNote> patchPlugins = new ArrayList<PatchChangeNote>();
  public Collection<EventMapActivation> mapperPlugins = new ArrayList<EventMapActivation>();
  
  public Collection<CueChangeNote> getCuePlugins() {
    return cuePlugins;
  }
  
  public Collection<PatchChangeNote> getPatchPlugins() {
    return patchPlugins;
  }
  
  public Collection<EventMapActivation> getMapperPlugins() {
    return mapperPlugins;
  }
  
  private void addCuePlugin(CueChangeNote plugin) {
    cuePlugins.add(plugin);
  }
  
  private void addPatchPlugin(PatchChangeNote plugin) {
    patchPlugins.add(plugin);
  }
  
  private void addMapperPlugin(EventMapActivation plugin) {
    mapperPlugins.add(plugin);
  }

  public void handleCuePlugins(MasterController masterController) {
    for (CueChangeNote plugin : getCuePlugins()) {
      plugin.cueChange(masterController);
    }
  }
  
  public void handlePatchPlugins(int ch, String name, Patch p) {
    for (PatchChangeNote plugin : getPatchPlugins()) {
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
    if (CueChangeNote.class.isAssignableFrom(cls)) {
      addCuePlugin( (CueChangeNote) qp );
      added = true;
    }
    if (PatchChangeNote.class.isAssignableFrom(cls)) {
      addPatchPlugin( (PatchChangeNote) qp );
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
  
    Iterator<CueChangeNote> cuePluginIter = getCuePlugins().iterator();
    while(cuePluginIter.hasNext()) {
      CueChangeNote obj = cuePluginIter.next();
      // remove plugins that match the name.
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        cuePluginIter.remove();
      }
    }
    Iterator<PatchChangeNote> patchPluginIter = getPatchPlugins().iterator();
    while (patchPluginIter.hasNext()) {
      // remove plugins that match the name.
      PatchChangeNote obj = patchPluginIter.next();
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