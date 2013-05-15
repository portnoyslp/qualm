package qualm.notification;

import qualm.MasterController;

public interface CueChange extends QualmNotification { 
  public void cueChange(MasterController mc);
}
