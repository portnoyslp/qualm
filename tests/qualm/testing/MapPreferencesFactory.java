package qualm.testing;

import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class MapPreferencesFactory implements PreferencesFactory {

  @Override
  public Preferences systemRoot() {
    return userRoot();
  }

  @Override
  public Preferences userRoot() {
    return new MapPreferences(null, "");
  }
  
  private class MapPreferences extends AbstractPreferences {
    private Map<String, String> root;
    private Map<String, MapPreferences> children;
    
    MapPreferences(AbstractPreferences parent, String name) {
      super(parent, name);
      root = new TreeMap<String, String>();
      children = new TreeMap<String, MapPreferences>();
    }
    
    @Override
    protected AbstractPreferences childSpi(String name) {
      MapPreferences child = children.get(name);
      if (child == null) {
        child = new MapPreferences(this, name);
        children.put(name, child);
      }
      return child;
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
      return children.keySet().toArray(new String[children.keySet().size()]);
    }

    @Override
    protected void flushSpi() throws BackingStoreException { }

    @Override
    protected String getSpi(String key) {
      return root.get(key);
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
      return root.keySet().toArray(new String[root.keySet().size()]);
    }

    @Override
    protected void putSpi(String key, String value) {
      root.put(key, value);
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException { }

    @Override
    protected void removeSpi(String key) {
      root.remove(key);
    }

    @Override
    protected void syncSpi() throws BackingStoreException { }
  }

}
