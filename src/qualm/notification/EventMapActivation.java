package qualm.notification;

import qualm.MasterController;

public interface EventMapActivation extends QualmNotifier { 
  public void activeEventMapper(MasterController em);
}
