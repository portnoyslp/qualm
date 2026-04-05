package qualm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

import qualm.notification.CueChange;
import qualm.notification.EventMapActivation;
import qualm.notification.NoteWindowChange;
import qualm.notification.PatchChange;
import qualm.notification.QualmNotifier;

public class NotificationManager {
  private Collection<CueChange> cueNotifiers = new ArrayList<>();
  private Collection<PatchChange> patchNotifiers = new ArrayList<>();
  private Collection<EventMapActivation> mapNotifiers = new ArrayList<>();
  private Collection<NoteWindowChange> noteWindowNotifiers = new ArrayList<>();

  public Collection<CueChange> getCueNotifiers() {
    return cueNotifiers;
  }

  public Collection<PatchChange> getPatchNotifiers() {
    return patchNotifiers;
  }

  public Collection<EventMapActivation> getMapNotifiers() {
    return mapNotifiers;
  }

  public Collection<NoteWindowChange> getNoteWindowNotifiers() {
    return noteWindowNotifiers;
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

  private void addNoteWindowNotifier(NoteWindowChange notifier) {
    noteWindowNotifiers.add(notifier);
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

  public void handleNoteWindowChanges(int ch, String channelName, Integer bottomNote, Integer topNote) {
    for (NoteWindowChange change : getNoteWindowNotifiers()) {
      change.noteWindowChange(ch, channelName, bottomNote, topNote);
    }
  }
  
  public void addNotification(String name) {
    Optional<QualmNotifier> found = ServiceLoader.load(QualmNotifier.class)
        .stream()
        .filter(p -> p.type().getName().equals(name) || p.type().getSimpleName().equals(name))
        .map(ServiceLoader.Provider::get)
        .findFirst();

    if (found.isEmpty() || !addNotifier(found.get())) {
      throw new IllegalArgumentException("Could not start plugin '" + name + "'");
    }
  }

  public boolean addNotifier(QualmNotifier qp) {
    Class<? extends QualmNotifier> cls = qp.getClass();
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
    if (NoteWindowChange.class.isAssignableFrom(cls)) {
      addNoteWindowNotifier( (NoteWindowChange) qp );
      added = true;
    }
    return added;
  }

  public Set<QualmNotifier> removeNotification(String name) {
    Set<QualmNotifier> removed = new HashSet<>();
  
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
    Iterator<NoteWindowChange> noteWindowNotificationIter = getNoteWindowNotifiers().iterator();
    while(noteWindowNotificationIter.hasNext()) {
      NoteWindowChange obj = noteWindowNotificationIter.next();
      if (obj.getClass().getName().equals( name )) {
        removed.add(obj);
        noteWindowNotificationIter.remove();
      }
    }
    return removed;
  }

  public void startNotifications() {
    for (QualmNotifier qn: getAllNotifiers()) {
      qn.beginNotifications();
    }
  }

  public void endNotifications() {
    for (QualmNotifier qn: getAllNotifiers()) {
      qn.endNotifications();
    }
  }

  private Set<QualmNotifier> getAllNotifiers() {
    Set<QualmNotifier> allNotifiers = new HashSet<>();
    allNotifiers.addAll(cueNotifiers);
    allNotifiers.addAll(patchNotifiers);
    allNotifiers.addAll(mapNotifiers);
    allNotifiers.addAll(noteWindowNotifiers);
    return allNotifiers;
  }

}