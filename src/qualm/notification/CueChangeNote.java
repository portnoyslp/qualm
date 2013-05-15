package qualm.notification;

import qualm.MasterController;

public interface CueChangeNote extends QualmNotification { 
  public void cueChange(MasterController mc);
}
