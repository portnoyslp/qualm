package qualm.notification;

import qualm.MasterController;

public interface CueChange extends QualmNotifier { 
  public void cueChange(MasterController mc);
}
