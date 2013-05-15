package qualm.notification;

import qualm.MasterController;

public interface EventMapActivation extends QualmNotification { 
  public void activeEventMapper(MasterController em);
}
