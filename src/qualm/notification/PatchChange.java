package qualm.notification;

import qualm.Patch;

public interface PatchChange extends QualmNotifier { 
  public void patchChange(int channel, String channelName, Patch patch);
}
