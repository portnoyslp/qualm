package qualm;

import java.lang.Thread;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import java.io.*;

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

  boolean dontInterrupt = false;
  public void updateCue() {
    // signal new cue...If we could interrupt the readline call, that
    // would be best, but instead we'll just print the new prompt
    // string.
    if (!dontInterrupt) {
      System.out.print( "\n" + promptString() );
      System.out.flush();
    }
  }

  public void processLine( String line ) {
    dontInterrupt = true;
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
    dontInterrupt = false;
  }

}
