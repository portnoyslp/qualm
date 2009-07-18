package qualm.plugins;

import qualm.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/*
 * Sends patch update information over the network to listeners.
 */

public class NetworkNotifier extends BaseQualmPlugin
  implements CueChangeNotification,PatchChangeNotification {

  public NetworkNotifier() {
    sockets = new ArrayList();
    try {
      serverSocket = new ServerSocket(NetworkNotificationProtocol.PORT);
    } catch (IOException e) {
      System.out.println( logHeader() + "Could not start; unable to listen on port" );
    }
  }

  public void initialize() {
    /* Launch a thread that listens for client connections, and sets
     * up sockets for dealing with those clients. */
    serverThread = new Thread() {
        public void run() {
          while (true) {
            try {
              Socket clientSocket = serverSocket.accept();
	      sockets.add(clientSocket);
            } catch (IOException e) {
              System.out.println( logHeader() + "Failed to accept connection" );
            }
          }
        }
      };
    serverThread.start();
  }

  
  private String logHeader() { 
    return "NetworkNotifier plugin[" + NetworkNotificationProtocol.PORT + "]: "; 
  }

  public void patchChange(int channel, String channelName, Patch patch) { 
    broadcast(NetworkNotificationProtocol.sendPatch(channel, 
						    channelName, 
						    patch.getDescription()));
  }
  public void cueChange(MasterController master) { 
    Iterator iter = master.getControllers().iterator();
    while (iter.hasNext()) {
      QController qc = (QController)iter.next();
      Cue curQ = qc.getCurrentCue();
      Cue pendingQ = qc.getPendingCue();

      broadcast(NetworkNotificationProtocol.sendCue(curQ.getCueNumber(),
						    (pendingQ == null ? pendingQ.getCueNumber() : ""),
						    qc.getTitle()));
    }
  }

  private void broadcast(String output) {
    Iterator sockIter = sockets.iterator();
    while (sockIter.hasNext()) {
      try {
	Socket sock = (Socket)sockIter.next();
	PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
	out.println(output);
      } catch (IOException ioe) { 
	System.out.println("Couldn't send output '" + output + "': " + ioe);
      }
    }
  }

  public void shutdown() { 
    // close all the sockets.
    try { 
      serverSocket.close();
      Iterator sockIter = sockets.iterator();
      while (sockIter.hasNext()) {
	((Socket)sockIter.next()).close();
      }
    } catch (IOException ioe) {
      System.out.println("Couldn't shut down sockets: " + ioe);
    }
  }

  ServerSocket serverSocket = null;
  ArrayList sockets = null;
  Thread serverThread = null;

}
