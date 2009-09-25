/* Network-based SWT plugin.  Used to create a lightweight Maemo
 * display, but can also be used elsewhere. */

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.net.*;
import java.io.*;
import java.util.*;

import qualm.plugins.NetworkNotificationProtocol;

public class ExampleNetworkReader extends Thread {

  public static void main(String[] args) {
    ExampleNetworkReader net = new ExampleNetworkReader();
    String hostName = net.parseArgs(args);
    net.buildDisplay();
    net.connectToServer(hostName);

    net.run();
  }

  public ExampleNetworkReader() {
    cueLabels = new TreeMap();
    patchLabels = new TreeMap();
    eventMappers = new HashMap();
  }

  public String parseArgs(String[] args) {
    // args are [filter] [hostname].

    if (args.length < 2) { 
      System.out.println("Incorrect arguments: " + getClass().getName() + " [filter-string] [hostname]");
      System.exit(1);
    }
    keyboard = args[0];
    String hostName = args[1];

    return hostName;
  }
 
  public void connectToServer(String hostName) {
    // connect to the server, and get ready to read from the socket.
    try {
      inputSocket = new Socket(hostName, NetworkNotificationProtocol.PORT);
      inputReader = 
	new BufferedReader(new InputStreamReader(inputSocket.getInputStream()));
    } catch (Exception e) {
      System.err.println("Unable to open connection to " + hostName + ": " +
			 e.getMessage());
      System.exit(1);
    }
  }

  public void buildDisplay() {
    display = Display.getDefault();
    shell = new Shell(display);
    shell.setText("Qualm Net Reader :: " + keyboard);
    shell.setSize(800,480);
    setStyle(shell);
    
    font = new Font(display,"Arial",30,SWT.BOLD);
    bigFont = new Font(display, "Arial", 80, SWT.BOLD);

    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    shell.setLayout(layout);

    curQ = new Label(shell, SWT.NONE);
    setStyle(curQ);
    curQ.setFont(bigFont);
    GridData gridData = new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false);
    gridData.widthHint = 100;

    nextQ = new Label(shell, SWT.RIGHT);
    setStyle(nextQ);
    gridData = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
    gridData.widthHint = 400;
    nextQ.setLayoutData(gridData);

    // and one big text widget.

    patches = new Text(shell, SWT.LEFT | SWT.MULTI);
    setStyle(patches);
    patches.setEditable(false);
    patches.setText("Waiting for patch info...");

    gridData = new GridData(GridData.FILL, GridData.BEGINNING, true, true);
    gridData.horizontalSpan = 3;
    gridData.widthHint = 800;
    gridData.heightHint = 300;
    patches.setLayoutData(gridData);
   
    shell.pack();
    shell.open();
  }

  public void run() {
    while (true) {
      // read from socket
      boolean updated = false;
      try {
	if (inputReader.ready()) {
          String inputLine = inputReader.readLine();
	  NetworkNotificationProtocol nnp = 
	    NetworkNotificationProtocol.receiveInput(inputLine);
	  if (nnp.type == NetworkNotificationProtocol.CUE) {
	    if (nnp.controllerName.contains(keyboard)) {
	      cueLabels.put(nnp.controllerName,nnp);
              // we wipe out the mappers when we get a new cue; we'll
              // populate them later when we receive them next.
              eventMappers.clear();
              updated = true;
            }
            
	  } else if (nnp.type == NetworkNotificationProtocol.PATCH) {
	    if (nnp.channelName.contains(keyboard)) {
	      patchLabels.put(new Integer(nnp.channelNum),nnp);
              updated = true;
            }
	  } else if (nnp.type == NetworkNotificationProtocol.MAPPER) {
            // OK, do we have a map for the destination channel?
            Integer toChan = new Integer(nnp.toChannel);
            if (patchLabels.containsKey(toChan)) {
              eventMappers.put(toChan, nnp);
              updated = true;
            }
          }
	  
	  updateLabels();
	}
      } catch (IOException ioe) {
	System.err.println("Unable to read data from host: " + ioe.getMessage());
      }

      if (updated) { shell.layout(); }
     
      // check to see if we have any updates.
      if (shell.isDisposed())
	break;
      display.readAndDispatch();
    }
  }

  private void updateLabels() {
    // Use cueLabels and patchLabels to update
    String text = "";
    Iterator iter = cueLabels.values().iterator();
    while (iter.hasNext()) {
      NetworkNotificationProtocol nnp = 
	(NetworkNotificationProtocol) iter.next();
    
      curQ.setText(nnp.currentCue);
      nextQ.setText("  -> " + (nnp.pendingCue.equals("") ? "END" : nnp.pendingCue));
    }

    text = "";
    iter = patchLabels.values().iterator();
    while (iter.hasNext()) {
      NetworkNotificationProtocol nnp = 
	(NetworkNotificationProtocol) iter.next();
    
      String labelStr = nnp.patchDescription;

      if (patchLabels.size() > 1)
	labelStr = nnp.channelName + ": " + labelStr;

      // only show if we have a matching mapper.
      NetworkNotificationProtocol mapper = 
        (NetworkNotificationProtocol) eventMappers.get(new Integer(nnp.channelNum));
      if (mapper != null) {
        if (!text.equals("")) 
          text = text + "\n";
        
        text = text + labelStr;
        
        if (!mapper.mapDescription.equals("")) {
          text = text + " [" + mapper.mapDescription + "]";
        }
      }
    }
    patches.setText(text);
  }

  private void setStyle(Control ctrl) {
    ctrl.setFont(font);
    ctrl.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
    ctrl.setForeground(display.getSystemColor(SWT.COLOR_WHITE)); 
  }

  Display display;
  Shell shell;
  Font font, bigFont;
  TreeMap cueLabels, patchLabels;
  HashMap eventMappers;
  Text patches;
  Label curQ, nextQ;
  String keyboard; // either K1 or K2; used as a filter for both cues and patches.
  Socket inputSocket;
  BufferedReader inputReader;
}