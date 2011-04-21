package qualm.plugins;

import qualm.MasterController;

public interface CueChangeNotification extends QualmPlugin { 
  public void cueChange(MasterController mc);
}
