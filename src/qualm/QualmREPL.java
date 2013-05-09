package qualm;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import qualm.plugins.CueChangeNotification;
import qualm.plugins.EventMapperNotification;
import qualm.plugins.PatchChangeNotification;
import qualm.plugins.QualmPlugin;

public class QualmREPL extends Thread {
  // Preference keys
  private static final String PLUGINS_PREFKEY = "plugins";
  private static Preferences prefs = 
    Preferences.userNodeForPackage(QualmREPL.class);

  private MasterController controller = null;
  private final List<CueChangeNotification> cuePlugins = new ArrayList<CueChangeNotification>();
  private final List<PatchChangeNotification> patchPlugins = new ArrayList<PatchChangeNotification>();
  private final List<EventMapperNotification> mapperPlugins = new ArrayList<EventMapperNotification>();
  private final BufferedReader reader;
  private final PrintWriter output;
  private String inputFilename = null;
  private boolean isRunning = false;
  private boolean readlineHandlesPrompt = false;

  public QualmREPL( ) {
    this(new InputStreamReader( System.in ), new PrintWriter( System.out ));
  }

  QualmREPL(Reader reader, Writer output) {
    this.reader = new BufferedReader(reader);
    this.output = new PrintWriter(output);
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
    for(QStream qs : qdata.getCueStreams()) {
      QController qc = new QController( controller.getMidiOut(), 
					qs, qdata );
      addController(qc);
    }

    output.println( "Loaded data from " + filename );

    if (isRunning)
      reset();
  }
  
  private void addController(QController qc) {
    controller.addController(qc);
  }

  // visible for testing
  void loadPreferences() {
    // load all the preferences available.
    String pluginNames = prefs.get(PLUGINS_PREFKEY,"");
    StringTokenizer st = new StringTokenizer(pluginNames,",");
    while (st.hasMoreTokens()) {
      String pluginName = st.nextToken();
      try {
	addPlugin(pluginName);
      } catch(IllegalArgumentException iae) {
	output.println("Preferences: could not create or identify plugin '" + pluginName +
			   "'; ignoring.");
      }
    }
  }

  private void savePreferences() {
    // store all the preferences information
    boolean init;
    String out;

    // combine cue and patch plugins into one
    Set<String> plugins = new HashSet<String>();
    for (PatchChangeNotification plugin : patchPlugins) {
      plugins.add(plugin.getClass().getName());
    }
    for (CueChangeNotification plugin : cuePlugins) {
      plugins.add(plugin.getClass().getName());
    }

    // and now we get all the names at once...
    init = true;
    out = "";
    for (String name : plugins) {
      out += (init ? "" : ",") + name;
      init = false;
    }
    prefs.put(PLUGINS_PREFKEY,out);
  }

  private String promptString() {
    String prompt="";
    
    boolean init = true;
    for (QController qc : controller.getControllers()) {
      if (!init) prompt += " | ";
      init = false;

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
    handleMapperPlugins();
    output.print( promptString() );
    output.flush();
  }

  @Override
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
	output.println(e);
      }
    }
  }

  private QController mainQC() {
    return controller.mainQC();
  }

  
  public void updateCue( Collection<QEvent> c ) {
    // signal new cue...If we could interrupt the readline call, that
    // would be best, but instead we'll just print the new prompt
    // string.
    if (!readlineHandlesPrompt) {
      // end the current line
      output.print( "\n" );
    }

    // print out the cue changes
    QData qd = mainQC().getQData();
    for (QEvent obj : c) {
      if (obj instanceof ProgramChangeEvent) {
	ProgramChangeEvent pce = (ProgramChangeEvent)obj;
	int ch = pce.getChannel();
	Patch patch = pce.getPatch();
	output.println( qd.getMidiChannels()[ch] + " -> " +
			    patch.getDescription() );
	
	// update the PatchChange plugins
	handlePatchPlugins( ch, qd.getMidiChannels()[ch], patch);
      }
      else if (obj instanceof NoteWindowChangeEvent) {
	NoteWindowChangeEvent nwce = (NoteWindowChangeEvent)obj;
	int ch = nwce.getChannel();
	output.println( qd.getMidiChannels()[ch] + " " + nwce );
      }
    }

    // redo prompt
    if (!readlineHandlesPrompt) {
      updatePrompt();
    }
  }

  private void reset() { 
    gotoCue("0.0");
  }

  private void advanceController (String line) {
    String stream_id = line.substring(line.indexOf(" ")).trim();
    controller.advanceStream(stream_id);
  }

  private void reverseController (String line) {
    String stream_id = line.substring(line.indexOf(" ")).trim();
    controller.reverseStream(stream_id);
  }

  private void gotoCue(String cueName) { controller.gotoCue(cueName); }

  /* visible for unit testing */
  void processLine( String line ) {
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
	mainQC().getQData().dump(output);
      
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

      } else if (lowerCase.startsWith("rev")) {
	reverseController(line);

      } else if (lowerCase.startsWith("load")) {
	StringTokenizer st = new StringTokenizer(line);
	st.nextToken();
	String filename = st.nextToken();
	loadFilename( filename );

      } else if (lowerCase.startsWith("showxml")) {
    	QDataXMLReader.outputXML(mainQC().getQData(), output);
    	output.println("");
    	  
      } else if (lowerCase.startsWith("reload")) {
	loadFilename( inputFilename );

      } else if (lowerCase.startsWith("version")) {
	output.println( Qualm.versionString() );

      } else {
	gotoCue(line);
      }
    }

    readlineHandlesPrompt = false;
  }

  private void addPlugin(String name) {
    // should be a class spec; try instantiating an object for this
    Class<?> cls;
    try {
      cls = Class.forName(name);
      QualmPlugin qp;
      if (Class.forName("qualm.plugins.QualmPlugin").isAssignableFrom(cls)) {
	qp = (QualmPlugin)cls.newInstance();
	qp.initialize();
	boolean added = false;
	if (Class.forName("qualm.plugins.CueChangeNotification").isAssignableFrom(cls)) {
	  cuePlugins.add( (CueChangeNotification) qp );
	  added = true;
	}
	if (Class.forName("qualm.plugins.PatchChangeNotification").isAssignableFrom(cls)) {
	  patchPlugins.add( (PatchChangeNotification) qp );
	  added = true;
	}
	if (Class.forName("qualm.plugins.EventMapperNotification").isAssignableFrom(cls)) {
	  mapperPlugins.add( (EventMapperNotification) qp );
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
    boolean found = false;

    Set<QualmPlugin> removed = new HashSet<QualmPlugin>();

    Iterator<CueChangeNotification> cuePluginIter = cuePlugins.iterator();
    while(cuePluginIter.hasNext()) {
      CueChangeNotification obj = cuePluginIter.next();
      // remove plugins that match the name.
      if (obj.getClass().getName().equals( name )) {
	removed.add(obj);
	cuePluginIter.remove();
	output.println("Removed cue plugin " + obj.getClass().getName());
	found = true;
      }
    }
    Iterator<PatchChangeNotification> patchPluginIter = patchPlugins.iterator();
    while (patchPluginIter.hasNext()) {
      // remove plugins that match the name.
      PatchChangeNotification obj = patchPluginIter.next();
      if (obj.getClass().getName().equals( name )) {
	removed.add(obj);
	patchPluginIter.remove();
	output.println("Removed patch plugin " + obj.getClass().getName());
	found = true;
      }
    }

    // shutdown all the removed plugins
    for (QualmPlugin qp : removed) 
      qp.shutdown();

    if (!found)
      output.println("Unable to find running plugin of type '" + name + "'");
  }


  private void parsePluginLine(String line) {
    // XXX there's probably a better way to handle plugins.  Checkout 
    // http://jpf.sourceforge.net
    StringTokenizer st = new StringTokenizer(line);
    String tok = st.nextToken();
    if (!tok.equals("plugin")) {
      output.println("Odd error: plugin spec line did not start with 'plugin'");
      return;
    }

    boolean remove = false;

    tok = st.nextToken();
    if (tok.equals("list")) {
      for (CueChangeNotification ccn : cuePlugins)
	output.println("cue " + ccn.getClass().getName());

      for (PatchChangeNotification pcn : patchPlugins)
	output.println("patch " + pcn.getClass().getName());

      for (EventMapperNotification emn : mapperPlugins)
	output.println("mapper " + emn.getClass().getName());
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
      output.println("Unable to create or identify requested plugin '" + tok + "; ignoring request.");
    }
  }

  private void handleCuePlugins() {
    for (QualmPlugin plugin : cuePlugins) {
      ((qualm.plugins.CueChangeNotification)plugin).cueChange(controller);
    }
  }

  private void handlePatchPlugins(int ch, String name, Patch p) {
    for (QualmPlugin plugin : patchPlugins) {
      ((qualm.plugins.PatchChangeNotification)plugin).patchChange(ch,name,p);
    }
  }

  private void handleMapperPlugins() {
    for (QualmPlugin plugin : mapperPlugins) {
      ((qualm.plugins.EventMapperNotification)plugin).activeEventMapper(controller);
    }
  }


}
