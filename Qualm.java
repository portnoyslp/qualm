package qualm;

import javax.sound.midi.*;
import java.util.*;
import java.io.*;

public class Qualm {
  
  Receiver receiver;
  int midiChannel = 0; // MIDI CH1

  public Qualm(Receiver rec) { 
    receiver = rec;
    System.out.println( rec );
  }
  
  public void sendPatchChange() {
    ShortMessage patchChange = null;
    try {
      patchChange = new ShortMessage();
      patchChange.setMessage( ShortMessage.PROGRAM_CHANGE,
			      midiChannel, 5, 0 );

      receiver.send(patchChange, -1);
      receiver.close();
    } catch (InvalidMidiDataException e) {
      System.out.println(e);
    }
  }

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
    MidiDevice.Info selectedInfo = null;

    System.out.println("ALSA MIDI ports:");
    Iterator i = midiports.iterator();
    while (i.hasNext()) {
      MidiDevice.Info info = (MidiDevice.Info)i.next();
      String dev = info.getName();
      dev = dev.substring(dev.indexOf('(')+1);
      dev = dev.substring(0, dev.lastIndexOf( ':' ));
      Integer cNum = new Integer(dev);
      String cName = (String)clientMap.get(cNum);
      System.out.println ("  " + info.getName() + " [" + cName +"]");

      // we like the one connected to UM-1...
      if (cName.indexOf("UM-1") != -1) 
	selectedInfo = info;

    }

    if (selectedInfo == null) {
      System.out.println("Couldn't find UM-1 output.");
      System.exit(1);
    }
    
    System.out.println("Using " + selectedInfo);
    try { 
      MidiDevice dev = MidiSystem.getMidiDevice( selectedInfo );
      Qualm q = new Qualm( dev.getReceiver() );
      q.sendPatchChange();

      System.out.println("Patch change sent.");

      // wait a second before exiting
      Thread.currentThread().sleep(1000);

    } catch (MidiUnavailableException mue) {
      System.out.println(mue); 
    } catch (InterruptedException ie) { }

    System.exit(0);
  }

}
