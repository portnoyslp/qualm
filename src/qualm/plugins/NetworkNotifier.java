package qualm.plugins;

import qualm.*;
import java.net.*;

import java.util.Collection;

/*
 * Sends patch update information over the network to listeners.
 */

public class NetworkNotifier 
  implements CueChangeNotification,PatchChangeNotification {

  public NetworkNotifier() {
    sockets = new ArrayList();
    try {
      serverSocket = new ServerSocket(NetworkNotificationProtocol.PORT);
    } catch (IOException e) {
      System.out.println( logHeader() + "Could not start; unable to listen on port" );
    }

    /* Launch a thread that listens for client connections, and sets
     * up sockets for dealing with those clients. */
    Thread serverThread = (new Thread() {
        public void run() {
          while (true) {
            try {
              Socket clientSocket = serverSocket.accept();
            } catch (IOException e) {
              System.out.println( logHeader() + "Failed to accept connection" );
            }
            
            sockets.add(clientSocket);
          }
        }
      }).start();
  }

  
  private String logHeader() { 
    return "NetworkNotifier plugin[" + NetworkNotificationProtocol.PORT + "]: "; 
  }

  public void patchChange(int channel, String channelName, Patch patch) { 
    broadcast(NetworkNotifier.sendPatch(channel, 
                                        channelName, 
                                        patch.getDescription()));
  }
  public void cueChange(MasterController master) { 
    Iterator iter = master.getControllers().iterator();
    while (iter.hasNext()) {
      QController qc = (QController)iter.next();
      Cue curQ = qc.getCurrentCue();
      Cue pendingQ = qc.getPendingCue();

      broadcast(curQ.getCueNumber(),
                (pendingQ == null ? pendingQ.getCueNumber() : ""),
                qc.getTitle());
    }
  }

  private void broadcast(String output) {
    Iterator sockIter = sockets.iterator();
    while (sockIter.hasNext()) {
      Socket sock = (Socket)sockIter.next();
      PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
      out.println(output);
    }
  }

  public void shutdown() { 
    // close all the sockets.
    serverSocket.close();
    Iterator sockIter = sockets.iterator();
    while (sockIter.hasNext()) {
      ((Socket)sockIter.next()).close();
    }
  }

  ServerSocket serverSocket = null;
  ArrayList sockets = null;

}
