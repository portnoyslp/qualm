package qualm.plugins;

import qualm.Patch;

public interface PatchChangeNotification extends QualmPlugin { 
  public void patchChange(int channel, String channelName, Patch patch);
}
