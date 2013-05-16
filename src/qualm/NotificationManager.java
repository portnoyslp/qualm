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
  public Collection<CueChange> cueNotifiers = new ArrayList<CueChange>();
  public Collection<PatchChange> patchNotifiers = new ArrayList<PatchChange>();
  public Collection<EventMapActivation> mapNotifiers = new ArrayList<EventMapActivation>();
  
  public Collection<CueChange> getCueNotifiers() {
    return cueNotifiers;
  }
  
  public Collection<PatchChange> getPatchNotifiers() {
    return patchNotifiers;
  }
  
  public Collection<EventMapActivation> getMapNotifiers() {
    return mapNotifiers;
  }
  
  private void addCueNotifier(CueChange notifier) {
    cueNotifiers.add(notifier);
  }
  
  private void addPatchNotifier(PatchChange notifier) {
    patchNotifiers.add(notifier);
  }
  
  private void addMapNotifier(EventMapActivation notifier) {
    mapNotifiers.add(notifier);
  }

  public void handleCueChanges(MasterController masterController) {
    for (CueChange change : getCueNotifiers()) {
      change.cueChange(masterController);
    }
  }
  
  public void handlePatchChanges(int ch, String name, Patch p) {
    for (PatchChange change : getPatchNotifiers()) {
      change.patchChange(ch,name,p);
    }
  }
  
  public void handleMapActivations(MasterController masterController) {
    for (EventMapActivation activation : getMapNotifiers()) {
      activation.activeEventMapper(masterController);
    }
  }
  
  public void addNotification(String name) {
    // should be a class spec; try instantiating an object for this
    Class<?> cls;
    try {
      cls = Class.forName(name);
      if (QualmNotification.class.isAssignableFrom(cls)) {
        boolean added = addNotification((QualmNotification)cls.newInstance());
        if (added) {
          return;
        }
      }
    } catch (Exception e) { }
    
    throw new IllegalArgumentException("Could not start plugin '" + name + "'");
  }

  public boolean addNotification(QualmNotification qp) {
    Class<? extends QualmNotification> cls = qp.getClass();
    qp.initialize();
    boolean added = false;
    if (CueChange.class.isAssignableFrom(cls)) {
      addCueNotifier( (CueChange) qp );
      added = true;
    }
    if (PatchChange.class.isAssignableFrom(cls)) {
      addPatchNotifier( (PatchChange) qp );
      added = true;
    }
    if (EventMapActivation.class.isAssignableFrom(cls)) {
      addMapNotifier( (EventMapActivation) qp );
      added = true;
    }
    return added;
  }

  public Set<QualmNotification> removeNotification(String name) {
    Set<QualmNotification> removed = new HashSet<QualmNotification>();
  
    Iterator<CueChange> cueNotificationIter = getCueNotifiers().iterator();
    while(cueNotificationIter.hasNext()) {
      CueChange obj = cueNotificationIter.next();
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        cueNotificationIter.remove();
      }
    }
    Iterator<PatchChange> patchNotificationIter = getPatchNotifiers().iterator();
    while (patchNotificationIter.hasNext()) {
      PatchChange obj = patchNotificationIter.next();
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        patchNotificationIter.remove();
      }
    }
    Iterator<EventMapActivation> mapNotificationIter = getMapNotifiers().iterator();
    while(mapNotificationIter.hasNext()) {
      EventMapActivation obj = mapNotificationIter.next();
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        mapNotificationIter.remove();
      }
    }

    // shutdown all the removed notifications
    for (QualmNotification qp : removed) 
      qp.shutdown();
  
    return removed;
  }

}