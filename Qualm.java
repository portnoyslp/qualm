package qualm;

import javax.sound.midi.*;
import java.util.*;
import java.io.*;

public class Qualm {

  public static Map parseALSAClients() {
    Map ret = new HashMap();
    String filename = "/proc/asound/seq/clients";
    try {
      BufferedReader br = 
	new BufferedReader( new FileReader( filename ));
      String lin = br.readLine();
      while ( lin != null ) {
	if (lin.startsWith("Client") && !lin.startsWith("Client info")) {
	  // get the client number and name
	  Integer clientNum = new Integer(lin.substring(6,10).trim());
	  String clientName = lin.substring(14);
	  clientName = clientName.substring(0,clientName.lastIndexOf('"'));
	  ret.put(clientNum, clientName);
	}
	lin = br.readLine();
      }
    } catch (IOException ioe) {
      System.out.println("Couldn't read " + filename + ": " + ioe);
      return null;
    }
    return ret;
  }

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

    Map clientMap = Qualm.parseALSAClients();

    System.out.println("ALSA MIDI ports:");
    Iterator i = midiports.iterator();
    while (i.hasNext()) {
      MidiDevice.Info info = (MidiDevice.Info)i.next();
      String dev = info.getName();
      dev = dev.substring(dev.indexOf('(')+1);
      dev = dev.substring(0, dev.lastIndexOf( ':' ));
      Integer cNum = new Integer(dev);
      System.out.println ("  " + info.getName() + " [" + clientMap.get(cNum)
			  +"]");
    }

    System.exit(0);
  }

}
