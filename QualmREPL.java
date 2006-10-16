package qualm;

import java.io.*;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

public class QualmREPL extends Thread {
  ArrayList controllers = new ArrayList();
  MultiplexReceiver mrec = null;
  BufferedReader reader;
  ArrayList cuePlugins = new ArrayList();
  ArrayList patchPlugins = new ArrayList();

  public QualmREPL( ) {
    reader = new BufferedReader( new InputStreamReader( System.in ));
  }

  public void setMultiplexReceiver( MultiplexReceiver m ) { mrec = m; }

  public void addController(QController qc) {
    controllers.add(qc);
    qc.setREPL(this);
  }

  public String promptString() {
    String prompt="";
    
    boolean init = true;
    Iterator iter = controllers.iterator();
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
    return (QController) (controllers.get(0));
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
      ProgramChangeEvent pce = (ProgramChangeEvent)iter.next();
      int ch = pce.getChannel();
      Patch patch = pce.getPatch();
      System.out.println( qd.getMidiChannels()[ch] + " -> " +
			  patch.getDescription() );

      // update the PatchChange plugins
      handlePatchPlugins( ch, qd.getMidiChannels()[ch], patch);
    }
      // redo prompt
    if (!readlineHandlesPrompt) {
      updatePrompt();
    }
  }

  public void reset() {
    gotoCue("0.0");
  }

  public void gotoCue(String cueName) {
    // send all controllers to the cue number named in the line
    Iterator iter = controllers.iterator();
    while (iter.hasNext()) {
      QController qc = (QController)iter.next();
      qc.switchToCue( cueName );
    }
  }

  public void processLine( String line ) {
    readlineHandlesPrompt = true;

    if (line == null || line.trim().equals("") ||
	line.trim().startsWith("\\") ||
	line.trim().startsWith("]")) {
      // advance the "mainline" patch
      mainQC().advancePatch();
    } else {

      if (line.toLowerCase().equals("quit")) {
	System.exit(0);
      }

      if (line.toLowerCase().equals("dump")) {
	mainQC().getQData().dump();
      
      } else if (line.toLowerCase().equals("reset")) {
	reset();

      } else if (line.toLowerCase().equals("showmidi")) {
	mrec.setDebugMIDI(true);
      } else if (line.toLowerCase().equals("unshowmidi")) {
	mrec.setDebugMIDI(false);

      } else if (line.toLowerCase().startsWith("plugin")) {
	installPlugin(line);

      } else {
	gotoCue(line);
      }
    }

    readlineHandlesPrompt = false;
  }

  public void installPlugin(String line) {
    // XXX there's probably a better way to handle plugins.  Checkout 
    // http://jpf.sourceforge.net
    StringTokenizer st = new StringTokenizer(line);
    String tok = st.nextToken();
    if (!tok.equals("plugin")) {
      System.out.println("Odd error: plugin spec line did not start with 'plugin'");
      return;
    }

    String pluginType = st.nextToken();
    if (!pluginType.equals("cue") && !pluginType.equals("patch")) {
      System.out.println("Only handling change plugins; type '" + pluginType + "' not supported.");
      return;
    }
    
    tok = st.nextToken();
    // should be a class spec; try instantiating an object for this
    Class cls;
    try {
      cls = Class.forName(tok);
  
      if (pluginType.equals("cue")) {
	if (Class.forName("qualm.plugins.CueChangeNotification").isAssignableFrom(cls))
	  cuePlugins.add( cls.newInstance() );
	else
	  System.out.println("Plugin '" + tok + "' is not a cue notifier; ignoring request.");
      }
      if (pluginType.equals("patch")) {
	if (Class.forName("qualm.plugins.PatchChangeNotification").isAssignableFrom(cls)) 
	  patchPlugins.add( cls.newInstance() );
	else 
	  System.out.println("Plugin '" + tok + "' is not a patch notifier; ignoring request.");
      }
      
    } catch (Exception e) {
      System.out.println("Unable to create plugin of type '" + tok + "'");
    }
 
  }

  public void handleCuePlugins() {
    // Tell any CueChangeNotification plugins
    Iterator iter = cuePlugins.iterator();
    while (iter.hasNext()) {
      ((qualm.plugins.CueChangeNotification)iter.next()).cueChange(controllers);
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
