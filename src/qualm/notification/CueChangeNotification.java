package qualm.notification;

import qualm.MasterController;

public interface CueChangeNotification extends QualmPlugin { 
  public void cueChange(MasterController mc);
}
