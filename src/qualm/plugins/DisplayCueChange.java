package qualm.plugins;

import qualm.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;

import java.util.Collection;
import java.util.Iterator;

public class DisplayCueChange extends CueChangeNotification {

  public DisplayCueChange() {
    showDisplay();
  }

  void showDisplay() {
    frame = new JFrame("Current Cue");
    java.awt.Container panel = frame.getContentPane();
    panel.setLayout(new BorderLayout());
    
    cDisp = new JLabel("--.--");
    cDisp.setFont( cDisp.getFont().deriveFont((float)100) );
    panel.add(cDisp, BorderLayout.NORTH);
    
    frame.pack();
    frame.setVisible(true);
  }

  public void cueChange(MasterController master) {
    // get all the current and pending cues
    String text = "<html><body>";
    Iterator iter = master.getControllers().iterator();
    boolean init = true;
    while (iter.hasNext()) {
      if (!init) text +="<br>";
      init = false;

      QController qc = (QController)iter.next();
      Cue curQ = qc.getCurrentCue();
      Cue pendingQ = qc.getPendingCue();

      text += qc.getTitle() + ": ";
      text += curQ.getCueNumber();
      text += " - ";

      if (pendingQ == null) 
	text += "END";
      else text += pendingQ.getCueNumber();
    }

    // display the new cue info in big 'ol letters
    cDisp.setText(text + "</body></html>");
  }

  public void shutdown() { frame.dispose(); frame = null; }

  JFrame frame;
  JLabel cDisp;

}
