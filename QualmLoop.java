package qualm;

import java.lang.Thread;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import java.io.*;

public class QualmLoop extends Thread {
  public QualmLoop() {
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

  public void run() {
    while (true) {
      try {
	String line = Readline.readline("QUALM> ");
	if (line == null)
	  System.out.println("no input");
	else
	  processLine();
      } 
      catch (EOFException e) {
	break;
      } 
      catch (Exception e) {
	System.out.println(e);
      }
    }
  }

  public void processLine() {
    System.out.println("...");
  }


  public static void main(String[] args) {
    new QualmLoop().start();
  }
  
}
