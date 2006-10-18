package qualm.plugins;

import qualm.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;

import java.util.TreeMap;
import java.util.Iterator;

public class DisplayPatchChange extends PatchChangeNotification {

  TreeMap patches = new TreeMap();

  public DisplayPatchChange() {
    showDisplay();
  }

  void showDisplay() {
    frame = new JFrame("Current Patch");
    java.awt.Container panel = frame.getContentPane();
    panel.setLayout(new BorderLayout());
    
    cDisp = new JLabel("--.--");
    cDisp.setFont( cDisp.getFont().deriveFont((float)100) );
    panel.add(cDisp, BorderLayout.NORTH);
    
    frame.pack();
    frame.setVisible(true);
    frame.show();
  }

  public void patchChange(int channel, String channelName, Patch patch) {
    // Store the info
    patches.put(channelName, patch.getDescription());

    // get all the current and pending cues
    String text = "<html><body>";
    Iterator iter = patches.keySet().iterator();
    boolean init = true;
    while (iter.hasNext()) {
      if (!init) text +="<br>";
      init = false;

      channelName = (String)iter.next();
      String patchName = (String)patches.get(channelName);

      text += channelName + ": " + patchName;
    }

    // display the new cue info in big 'ol letters
    cDisp.setText(text + "</body></html>");
  }

  public void shutdown() { frame.dispose(); frame = null; }

  JFrame frame;
  JLabel cDisp;

}
