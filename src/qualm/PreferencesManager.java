package qualm;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import qualm.notification.CueChangeNotification;
import qualm.notification.PatchChangeNotification;

public class PreferencesManager {
  private Preferences prefs = Preferences.userNodeForPackage(PreferencesManager.class);
  static final String PLUGINS_PREFKEY = "plugins";
  private MasterController controller;
  
  public PreferencesManager() { }

  public void setController(MasterController controller) {
    this.controller = controller;
  }
  
  private MasterController getController() { return controller; }

  private Preferences getPrefs() { return prefs; }

  /**
   * Load all the preferences information, and re-install into the controller.
   * @throws IllegalArgumentException if a plugin cannot be reinstated.
   * @deprecated Use {@link #loadPreferences()} instead
   */
  public void loadPreferences(QualmREPL repl) throws IllegalArgumentException {
    loadPreferences();
  }

  /**
   * Load all the preferences information, and re-install into the controller.
   * @throws IllegalArgumentException if a plugin cannot be reinstated.
   */
  public void loadPreferences() throws IllegalArgumentException {
    // if there's no controller available, don't need to do anything.
    if (controller == null) return;
    
    String pluginNames = getPrefs().get(PreferencesManager.PLUGINS_PREFKEY,"");
    StringTokenizer st = new StringTokenizer(pluginNames,",");
    while (st.hasMoreTokens()) {
      String pluginName = st.nextToken();
      try {
        controller.getPluginManager().addPlugin(pluginName);
      } catch(IllegalArgumentException iae) {
        throw new IllegalArgumentException(pluginName, iae);
      }
    }
  }

  /**
   * Pull all the preferences that should be saved from the controller, and store them.
   */
  public void savePreferences() {
    PluginManager pm = getController().getPluginManager();
  
    // combine cue and patch plugins into one
    Set<String> plugins = new HashSet<String>();
    for (PatchChangeNotification plugin : pm.getPatchPlugins()) {
      plugins.add(plugin.getClass().getName());
    }
    for (CueChangeNotification plugin : pm.getCuePlugins()) {
      plugins.add(plugin.getClass().getName());
    }
  
    // and now we get all the names at once...
    StringBuilder outputNames = new StringBuilder();
    String delim = "";
    for (String name : plugins) {
      outputNames.append(delim);
      outputNames.append(name);
      delim = ",";
    }

    getPrefs().put(PreferencesManager.PLUGINS_PREFKEY, outputNames.toString());
  }
}