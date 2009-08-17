package qualm;

import java.io.*;
import java.lang.Thread;
import java.util.*;
import java.util.prefs.Preferences;
import qualm.plugins.QualmPlugin;

public class QualmREPL extends Thread {
  // Preference keys
  private static final String PLUGINS_PREFKEY = "plugins";
  private static Preferences prefs = 
    Preferences.userNodeForPackage(QualmREPL.class);


  MasterController controller = null;
  BufferedReader reader;
  ArrayList cuePlugins = new ArrayList();
  ArrayList patchPlugins = new ArrayList();
  String inputFilename = null;
  boolean isRunning = false;

  public QualmREPL( ) {
    reader = new BufferedReader( new InputStreamReader( System.in ));
    loadPreferences();
  }

  public void setMasterController(MasterController mc) { 
    controller = mc; 
    controller.setREPL(this);
  }

  public void loadFilename( String filename ) {
    inputFilename = filename;

    // remove existing controllers
    controller.removeControllers();

    QData qdata = Qualm.loadQDataFromFilename(filename);

    // For each cue stream, start a controller
    Iterator iter = qdata.getCueStreams().iterator();
    while (iter.hasNext()) {
      QController qc = new QController( controller.getMidiOut(), 
					(QStream)iter.next(),
					qdata );
      addController(qc);
    }

    System.out.println( "Loaded data from " + filename );

    if (isRunning)
      reset();

  }
  
  public void addController(QController qc) {
    controller.addController(qc);
  }

  public void loadPreferences() {
    // load all the preferences available.
    String pluginNames = prefs.get(PLUGINS_PREFKEY,"");
    StringTokenizer st = new StringTokenizer(pluginNames,",");

    while (st.hasMoreTokens()) {
      String pluginName = st.nextToken();
      try {
	addPlugin(pluginName);
      } catch(IllegalArgumentException iae) {
	System.out.println("Preferences: could not create or identify plugin '" + pluginName +
			   "'; ignoring.");
      }
    }
  }

  public void savePreferences() {
    // store all the preferences information
    Iterator iter;
    boolean init;
    String out;

    // combine cue and patch plugins into one
    Set plugins = new HashSet();
    iter = patchPlugins.iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      plugins.add(obj.getClass().getName());
    }
    iter = cuePlugins.iterator();
    while (iter.hasNext()) {
      Object obj = iter.next();
      plugins.add(obj.getClass().getName());
    }

    // and now we get all the names at once...
    init = true;
    out = "";
    iter = plugins.iterator();
    while (iter.hasNext()) {
      if (!init) out+=",";
      out += (String)iter.next();
    }
    prefs.put(PLUGINS_PREFKEY,out);
      
  }

  public String promptString() {
    String prompt="";
    
    boolean init = true;
    Iterator iter = controller.getControllers().iterator();
    while (iter.hasNext()) {
      if (!init) prompt += " | ";
      init = false;

      QController qc = (QController)iter.next();
      Cue curQ = qc.getCurrentCue();
      Cue pendingQ = qc.getPendingCue();

      if (curQ == null) 
	prompt += "START";
      else prompt += curQ.getCueNumber();

      prompt += "-";

      if (pendingQ == null) 
	prompt += "END";
      else prompt += pendingQ.getCueNumber();
    }

    return prompt + "> ";
  }

  public void updatePrompt() {
    handleCuePlugins();
    System.out.print( promptString() );
    System.out.flush();
  }


  public void run() {
    isRunning = true;

    // first, we reset the controllers.
    readlineHandlesPrompt = true;
    reset();
    readlineHandlesPrompt = false;

    while (true) {
      try {
	updatePrompt();

	String line = reader.readLine();
	processLine( line );
      } 
      catch (EOFException e) {
	break;
      } 
      catch (Exception e) {
	System.out.println(e);
      }
    }
  }

  private QController mainQC() {
    return controller.mainQC();
  }

  boolean readlineHandlesPrompt = false;
  public void updateCue( Collection c ) {
    // signal new cue...If we could interrupt the readline call, that
    // would be best, but instead we'll just print the new prompt
    // string.
    if (!readlineHandlesPrompt) {
      // end the current line
      System.out.print( "\n" );
    }

    // print out the cue changes
    QData qd = mainQC().getQData();
    Iterator iter = c.iterator();
    while(iter.hasNext()) {
      Object obj = iter.next();
      if (obj instanceof ProgramChangeEvent) {
	ProgramChangeEvent pce = (ProgramChangeEvent)obj;
	int ch = pce.getChannel();
	Patch patch = pce.getPatch();
	System.out.println( qd.getMidiChannels()[ch] + " -> " +
			    patch.getDescription() );
	
	// update the PatchChange plugins
	handlePatchPlugins( ch, qd.getMidiChannels()[ch], patch);
      }
      else if (obj instanceof NoteWindowChangeEvent) {
	NoteWindowChangeEvent nwce = (NoteWindowChangeEvent)obj;
	int ch = nwce.getChannel();
	System.out.println( qd.getMidiChannels()[ch] + " " + nwce );
      }
    }

    // redo prompt
    if (!readlineHandlesPrompt) {
      updatePrompt();
    }
  }

  public void reset() { 
    gotoCue("0.0");
  }

  public void advanceController (String line) {
    String stream_id = line.substring(line.indexOf(" ")).trim();
    controller.advanceStream(stream_id);
  }

  public void gotoCue(String cueName) { controller.gotoCue(cueName); }

  public void processLine( String line ) {
    readlineHandlesPrompt = true;

    if (line == null || line.trim().equals("") ||
	line.trim().startsWith("\\") ||
	line.trim().startsWith("]")) {
      // advance the "mainline" patch
      mainQC().advancePatch();
    } else {
      String lowerCase = line.toLowerCase();

      if (lowerCase.equals("quit")) {
	System.exit(0);
      }

      if (lowerCase.equals("dump")) {
	mainQC().getQData().dump();
      
      } else if (lowerCase.equals("reset")) {
	reset();

      } else if (lowerCase.equals("showmidi")) {
	controller.setDebugMIDI(true);
      } else if (lowerCase.equals("unshowmidi")) {
	controller.setDebugMIDI(false);

      } else if (lowerCase.startsWith("plugin")) {
	parsePluginLine(line);

      } else if (lowerCase.startsWith("save")) {
	savePreferences();

      } else if (lowerCase.startsWith("adv")) {
	advanceController(line);

      } else if (lowerCase.startsWith("load")) {
	StringTokenizer st = new StringTokenizer(line);
	st.nextToken();
	String filename = st.nextToken();
	loadFilename( filename );

      } else if (lowerCase.startsWith("reload")) {
	loadFilename( inputFilename );

      } else if (lowerCase.startsWith("version")) {
	System.out.println( Qualm.versionString() );

      } else if (lowerCase.startsWith("version")) {
	System.out.println( Qualm.versionString() );

      } else {
	gotoCue(line);
      }
    }

    readlineHandlesPrompt = false;
  }

  private void addPlugin(String name) {
    // should be a class spec; try instantiating an object for this
    Class cls;
    try {
      cls = Class.forName(name);
      QualmPlugin qp;
      if (Class.forName("qualm.plugins.QualmPlugin").isAssignableFrom(cls)) {
	qp = (QualmPlugin)cls.newInstance();
	qp.initialize();
	boolean added = false;
	if (Class.forName("qualm.plugins.CueChangeNotification").isAssignableFrom(cls)) {
	  cuePlugins.add( qp );
	  added = true;
	}
	if (Class.forName("qualm.plugins.PatchChangeNotification").isAssignableFrom(cls)) {
	  patchPlugins.add( qp );
	  added = true;
	}
	if (added) {
	  return;
	}
      }
    } catch (Exception e) { }
    
    throw new IllegalArgumentException("Could not start plugin '" + name + "'");
  }

  private void removePlugin(String name) {
    Iterator iter;
    boolean found = false;

    iter = cuePlugins.iterator();
    while (iter.hasNext()) {
      // remove plugins that match the name.
      Object obj = iter.next();
      if (obj.getClass().getName().equals( name )) {
	((QualmPlugin)obj).shutdown();
	iter.remove();
	System.out.println("Removed cue plugin " + obj.getClass().getName());
	found = true;
      }
    }
    iter = patchPlugins.iterator();
    while (iter.hasNext()) {
      // remove plugins that match the name.
      Object obj = iter.next();
      if (obj.getClass().getName().equals( name )) {
	((QualmPlugin)obj).shutdown();
	iter.remove();
	System.out.println("Removed patch plugin " + obj.getClass().getName());
	found = true;
      }
    }
    
    if (!found)
      System.out.println("Unable to find running plugin of type '" + name + "'");
  }


  public void parsePluginLine(String line) {
    // XXX there's probably a better way to handle plugins.  Checkout 
    // http://jpf.sourceforge.net
    StringTokenizer st = new StringTokenizer(line);
    String tok = st.nextToken();
    if (!tok.equals("plugin")) {
      System.out.println("Odd error: plugin spec line did not start with 'plugin'");
      return;
    }

    boolean remove = false;

    tok = st.nextToken();
    if (tok.equals("list")) {
      Iterator iter = cuePlugins.iterator();
      while (iter.hasNext())
	System.out.println("cue " + iter.next().getClass().getName());

      iter = patchPlugins.iterator();
      while (iter.hasNext())
	System.out.println("patch " + iter.next().getClass().getName());
      return;

    } else if (tok.equals("remove")) {
      remove = true;
      tok = st.nextToken();
    } 

    try {
      if (remove)
	removePlugin(tok);
      else
	addPlugin(tok);
    } catch (IllegalArgumentException iae) {
      System.out.println("Unable to create or identify requested plugin '" + tok + "; ignoring request.");
    }
  }

  public void handleCuePlugins() {
    // Tell any CueChangeNotification plugins
    Iterator iter = cuePlugins.iterator();
    while (iter.hasNext()) {
      ((qualm.plugins.CueChangeNotification)iter.next()).cueChange(controller);
    }
  }

  public void handlePatchPlugins(int ch, String name, Patch p) {
    // Tell any CueChangeNotification plugins
    Iterator iter = patchPlugins.iterator();
    while (iter.hasNext()) {
      ((qualm.plugins.PatchChangeNotification)iter.next()).patchChange(ch,name,p);
    }
  }


}
