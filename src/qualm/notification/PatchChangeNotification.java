package qualm.notification;

import qualm.Patch;

public interface PatchChangeNotification extends QualmNotification { 
  public void patchChange(int channel, String channelName, Patch patch);
}
