package qualm;

import javax.sound.midi.*;
import java.util.*;

public class Qualm {
  
  public static void main(String[] args) {
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    if (infos.length == 0) {
      System.out.println( "No MIDI devices found.  Exiting." );
      System.exit(1);
    }

    /* Find ALSA MIDI ports */
    List midiports = new ArrayList();
    for(int i = 0; i<infos.length; i++) {
      if (infos[i].getName().startsWith("ALSA MIDI")) {
	midiports.add(infos[i]);
      }
    }

    if (midiports.size() == 0) {
      System.out.println("No (ALSA) MIDI ports found.  Exiting." );
      System.exit(1);
    }

    System.out.println("ALSA MIDI ports:");
    Iterator i = midiports.iterator();
    while (i.hasNext()) {
      System.out.println ("  " + (MidiDevice.Info)i.next());
    }

    System.exit(0);
  }

}
