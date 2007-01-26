package qualm.plugins;

import java.util.Collection;
import qualm.MasterController;

public abstract class CueChangeNotification extends QualmPlugin { 
  public abstract void cueChange(MasterController mc);
}
