package qualm.notification;

import qualm.MasterController;

public interface CueChangeNotification extends QualmNotification { 
  public void cueChange(MasterController mc);
}
