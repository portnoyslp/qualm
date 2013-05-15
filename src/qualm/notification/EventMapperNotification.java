package qualm.notification;

import qualm.MasterController;

public interface EventMapperNotification extends QualmNotification { 
  public void activeEventMapper(MasterController em);
}
