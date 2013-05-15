package qualm.notification;

import qualm.Patch;

public interface PatchChange extends QualmNotification { 
  public void patchChange(int channel, String channelName, Patch patch);
}
