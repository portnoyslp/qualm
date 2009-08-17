package qualm.plugins;

import qualm.MasterController;

public interface EventMapperNotification extends QualmPlugin { 
  public void activeEventMapper(MasterController em);
}
