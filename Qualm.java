package qualm;

import javax.sound.midi.*;
import java.util.*;
import java.io.*;
import gnu.getopt.Getopt;

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

  private static void usage() {
    System.out.println("Usage: java qualm.Qualm [-o <output>] [-i <input>] [-l] [-h] <filename>");
    System.out.println("   -o <output>  Sets output ALSA port.");
    System.out.println("   -i <input>   Sets input ALSA port.");
    System.out.println("   -l           List all ALSA MIDI ports and exit.");
    System.out.println("   -h           Prints this message.");
    System.out.println("   <filename>   The Qualm filename to execute.");
    System.out.println("\n If only one port is specified, an attempt will be made to open the\nport for both input and output.  If no port is specified, 'UM-1' will\nbe used.");
  }
    
  public static void main(String[] args) {

    String inputPort = null;
    String outputPort = null;
    boolean listPorts = false;

    // handle argument list
    Getopt g = new Getopt("Qualm", args, "o:i:hl");
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c)
	{
	case 'i':
	  inputPort = g.getOptarg();
	  break;
	case 'o':
	  outputPort = g.getOptarg();
	  break;
	case 'l':
	  listPorts = true;
	  break;
	case 'h':
	  usage();
	  System.exit(0);
	}
    }

    if (g.getOptind() == args.length && !listPorts) {
      System.out.println("No filename given.");
      System.exit(0);
    }
    String inputFilename = null;
    if (!listPorts)
      inputFilename = args[g.getOptind()];

    // set ports
    if (inputPort == null && outputPort == null) 
      inputPort="UM-1";
    if (outputPort == null) 
      outputPort = inputPort;
    if (inputPort == null)
      inputPort = outputPort;
    
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
    MidiDevice.Info inputInfo = null;
    MidiDevice.Info outputInfo = null;

    if (listPorts) 
      System.out.println("ALSA MIDI ports:");

    Iterator i = midiports.iterator();
    while (i.hasNext()) {
      MidiDevice.Info info = (MidiDevice.Info)i.next();
      String dev = info.getName();
      dev = dev.substring(dev.indexOf('(')+1);
      dev = dev.substring(0, dev.lastIndexOf( ':' ));
      Integer cNum = new Integer(dev);
      String cName = (String)clientMap.get(cNum);

      if (listPorts)
	System.out.println ("  " + info.getName() + " [" + cName +"]");
	
      // Is this a port we want?
      if (cName.indexOf(inputPort) != -1 ||
	  info.getName().indexOf(inputPort) != -1) 
	inputInfo = info;
      
      if (cName.indexOf(outputPort) != -1 ||
	  info.getName().indexOf(outputPort) != -1) 
	outputInfo = info;
      
    }
    
    if (listPorts) 
      System.exit(0);

    if (inputInfo == null) {
      System.out.println("Unable to load input port named " + inputPort);
      System.exit(1);
    }
    if (outputInfo == null) {
      System.out.println("Unable to load output port named " + outputPort);
      System.exit(1);
    }

    // get the transmitter and receiver
    Transmitter midiIn = null;
    Receiver midiOut = null;

    // Load the qualm file
    System.out.println("loading " + inputFilename + "...");
    QDataLoader qdl = new QDataLoader();
    QData data = qdl.readFile( new java.io.File( inputFilename ));

    try {
      MidiDevice inDevice = MidiSystem.getMidiDevice( inputInfo );
      MidiDevice outDevice = MidiSystem.getMidiDevice( outputInfo );
      inDevice.open();
      outDevice.open();
      
      midiIn = inDevice.getTransmitter();
      midiOut = outDevice.getReceiver();

      // open a receiver with the right data file.
      QController qc = new QController( midiOut, data );

      // Start a read-eval-print loop as well.  The REPL will do a
      // System.exit when all is done.

      new QualmREPL( qc ).start();
      
      // connect the transmitter to the receiver.
      midiIn.setReceiver( qc );

    } catch (MidiUnavailableException mue) {
      System.out.println(mue); 
    }
  }

}
