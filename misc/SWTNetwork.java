/* Network-based SWT plugin.  Used to create a lightweight Maemo display. */

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import java.net.*;
import java.io.*;
import java.util.*;

import qualm.plugins.NetworkNotificationProtocol;

import gnu.getopt.*;

public class SWTNetwork extends Thread {

  public static void main(String[] args) {
    SWTNetwork net = new SWTNetwork();
    net.buildDisplay();
    net.connectToServer(args);

    net.run();
  }


  public SWTNetwork() {
    cueLabels = new TreeMap();
    patchLabels = new TreeMap();
  }

  public void connectToServer(String[] args) {
    // handling arguments
    LongOpt[] longopts = new LongOpt[2];
    int i = 0;
    longopts[i++] = new LongOpt("cues", LongOpt.REQUIRED_ARGUMENT,
				null,'c');
    longopts[i++] = new LongOpt("patches", LongOpt.REQUIRED_ARGUMENT,
				null,'p');
    
    Getopt g = new Getopt("SWTNetwork", args, "c:p:", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch (c)
	{
	case 'c':
	  cueFilter = g.getOptarg(); break;
	case 'p':
	  patchFilter = g.getOptarg(); break;
	}
    }
    if (g.getOptind() == args.length) {
      System.out.println("No hostname given.\n");
      System.exit(0);
    }
      
    // connect to the server, and get ready to read from the socket.
    String hostName = args[g.getOptind()];
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
    shell.setLayout(new FillLayout(SWT.VERTICAL));
    font = new Font(display,"Arial",30,SWT.BOLD);

    setStyle(shell);
    
    cues = new Text(shell, SWT.CENTER | SWT.MULTI);
    setStyle(cues);
    cues.setEditable(false);
    cues.setText("Qualm Network Reader");

    patches = new Text(shell, SWT.LEFT | SWT.MULTI);
    setStyle(patches);
    patches.setEditable(false);
    patches.setText("");
   
    shell.open();
  }

  public void run() {
    while (true) {
      // read from socket
      try {
	if (inputReader.ready()) {
	  NetworkNotificationProtocol nnp = 
	    NetworkNotificationProtocol.receiveInput(inputReader.readLine());
	  if (nnp.type == NetworkNotificationProtocol.CUE) {
	    if (cueFilter == null  ||
		nnp.controllerName.matches(cueFilter))
	      cueLabels.put(nnp.controllerName,nnp);
	  } else if (nnp.type == NetworkNotificationProtocol.PATCH) {
	    if (patchFilter == null || 
		nnp.channelName.matches(patchFilter))
	      patchLabels.put(new Integer(nnp.channelNum),nnp);
	  }
	  
	  updateLabels();
	}
      } catch (IOException ioe) {
	System.err.println("Unable to read data from host: " + ioe.getMessage());
      }
      
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
    
      String labelStr = nnp.currentCue + " - " +
	(nnp.pendingCue.equals("") ? "END" : nnp.pendingCue);
          
      if (cueLabels.size() > 1)
	labelStr = nnp.controllerName + ": " + labelStr;

      if (!text.equals("")) 
	text += "\n";
      text += labelStr;
    }
    cues.setText(text);

    text = "";
    iter = patchLabels.values().iterator();
    while (iter.hasNext()) {
      NetworkNotificationProtocol nnp = 
	(NetworkNotificationProtocol) iter.next();
    
      String labelStr = nnp.patchDescription;
          
      if (patchLabels.size() > 1)
	labelStr = nnp.channelName + ": " + labelStr;

      if (!text.equals("")) 
	text = text + "\n";
      text = text + labelStr;
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
  Font font;
  TreeMap cueLabels, patchLabels;
  Text cues, patches;
  String cueFilter,patchFilter;
  Socket inputSocket;
  BufferedReader inputReader;
}