package qualm.notification;

import qualm.Patch;

public interface PatchChangeNote extends QualmNotification { 
  public void patchChange(int channel, String channelName, Patch patch);
}
