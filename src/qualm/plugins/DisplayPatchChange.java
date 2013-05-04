package qualm.plugins;

import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JLabel;

import qualm.Patch;

public class DisplayPatchChange extends BaseQualmPlugin implements PatchChangeNotification {

  TreeMap<Integer,String> channels = new TreeMap<Integer,String>();

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
  }

  public void patchChange(int channel, String channelName, Patch patch) {
    // Store the info
    Integer channelNum = new Integer(channel);

    // Generate text for description
    channels.put(channelNum, channelName + ": " + patch.getDescription());

    // get all the current and pending cues
    String text = "<html><body>";
    Iterator<Integer> iter = channels.keySet().iterator();
    boolean init = true;
    while (iter.hasNext()) {
      if (!init) text +="<br>";
      init = false;

      channelNum = iter.next();
      text += channels.get(channelNum);
    }

    // display the new cue info in big 'ol letters
    cDisp.setText(text + "</body></html>");
  }

  public void shutdown() { frame.dispose(); frame = null; }

  JFrame frame;
  JLabel cDisp;

}
