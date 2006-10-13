package qualm;

import java.lang.Thread;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

public class QualmREPL extends Thread {
  ArrayList controllers = new ArrayList();
  MultiplexReceiver mrec = null;
  BufferedReader reader;

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

  public void run() {
    // first, we reset the controllers.
    readlineHandlesPrompt = true;
    reset();
    readlineHandlesPrompt = false;

    while (true) {
      try {
	System.out.print( promptString() );
	System.out.flush();

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
    }
      // redo prompt
    if (!readlineHandlesPrompt) {
      System.out.print( promptString() );
      System.out.flush();
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
	
      } else {
	gotoCue(line);
      }
    }

    readlineHandlesPrompt = false;
  }

}
