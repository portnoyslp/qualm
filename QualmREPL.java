package qualm;

import java.lang.Thread;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import java.io.*;
import java.util.Collection;
import java.util.Iterator;

public class QualmREPL extends Thread {
  QController qc;
  public QualmREPL( QController qc ) {
    this.qc = qc; 
    qc.setREPL(this);

    try { 
      Readline.load( ReadlineLibrary.GnuReadline );
    } catch (UnsatisfiedLinkError ignore_me) { }
    
    Readline.initReadline("myapp");
    
    Runtime.getRuntime()
      .addShutdownHook(new Thread() {
	  public void run() {
	    Readline.cleanup();
	  }
	});
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
	String line = Readline.readline( promptString() );
	if (line != null)
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
      int patch = pce.getPatch();
      System.out.println( qd.getMidiChannels()[ch] + " -> " +
			  qd.getPatches()[patch] );
    }
      // redo prompt
    if (!readlineHandlesPrompt) {
      System.out.print( promptString() );
      System.out.flush();
    }
  }

  public void processLine( String line ) {
    readlineHandlesPrompt = true;
    if (line.toLowerCase().equals("quit")) {
      System.exit(0);
    }

    if (line.toLowerCase().equals("reset")) {
      // go back to the first cue
      qc.switchToCue( "0.0" );
    } else {
      // go to the cue number named in the line
      qc.switchToCue( line );
    }
    readlineHandlesPrompt = false;
  }

}
