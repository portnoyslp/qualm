package qualm;

import java.lang.Thread;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;

public class QualmREPL extends Thread {
  QController qc;
  BufferedReader reader;

  public QualmREPL( QController qc ) {
    this.qc = qc; 
    qc.setREPL(this);
    reader = new BufferedReader( new InputStreamReader( System.in ));
  }

  public String promptString() {
    Cue curQ = qc.getCurrentCue();
    Cue pendingQ = qc.getPendingCue();
    String prompt;
    if (curQ == null) 
      prompt="START";
    else prompt=curQ.getCueNumber();
    prompt += " [";
    if (pendingQ == null) 
      prompt+= "END";
    else prompt+= pendingQ.getCueNumber();
    return prompt + "]> ";
  }

  public void run() {
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
    QData qd = qc.getQData();
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

  public void processLine( String line ) {
    readlineHandlesPrompt = true;

    if (line == null || line.trim().equals("") ||
	line.trim().startsWith("\\") ||
	line.trim().startsWith("]")) {
      qc.advancePatch();
    } else {

      if (line.toLowerCase().equals("quit")) {
	System.exit(0);
      }

      if (line.toLowerCase().equals("dump")) {
	qc.getQData().dump();
      
      } else if (line.toLowerCase().equals("reset")) {
	// go back to the first cue
	qc.switchToCue( "0.0" );
	
      } else {
	// go to the cue number named in the line
	qc.switchToCue( line );
      }
    }

    readlineHandlesPrompt = false;
  }

}
