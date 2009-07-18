package qualm.plugins;

import java.util.Collection;
import qualm.MasterController;

public interface CueChangeNotification extends QualmPlugin { 
  public void cueChange(MasterController mc);
}
