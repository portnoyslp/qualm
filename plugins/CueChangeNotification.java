package qualm.plugins;

import java.util.Collection;

public abstract class CueChangeNotification extends QualmPlugin { 
  public abstract void cueChange(Collection qControllers);
}
