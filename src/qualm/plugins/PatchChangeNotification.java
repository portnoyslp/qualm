package qualm.plugins;

import qualm.Patch;

public abstract class PatchChangeNotification extends QualmPlugin { 
  public abstract void patchChange(int channel, String channelName, Patch patch);
}
