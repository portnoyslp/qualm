package qualm;

import javax.sound.midi.*;
import java.util.*;
import java.io.*;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

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
    System.out.println("Usage: java qualm.Qualm <options> <filename>");
    System.out.println("  --output <out>");
    System.out.println("  -o <out>        Sets output ALSA port.");
    System.out.println("  --input <in>");
    System.out.println("  -i <in>         Sets input ALSA port.");
    System.out.println("  --nomidi | -n   Ignore MIDI ports");
    System.out.println("  --list | -l     List all ALSA MIDI ports and exit.");
    System.out.println("  --debugmidi     Print all received MIDI events.");
    System.out.println("  --help | -h     Prints this message.");
    System.out.println("  <filename>      The Qualm filename to execute.");
    System.out.println("\n If only one port is specified, an attempt will be made to open the\nport for both input and output.  If no port is specified, 'UM-1' will\nbe used.");
  }
    
  public static void main(String[] args) {

    String inputPort = null;
    String outputPort = null;
    boolean listPorts = false;
    boolean debugMIDI = false;
    boolean skipMIDI = false;

    // handle argument list
    int i = 0;
    LongOpt[] longopts = new LongOpt[6];
    longopts[i++] = new LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o');
    longopts[i++] = new LongOpt("input", LongOpt.REQUIRED_ARGUMENT, null, 'i');
    longopts[i++] = new LongOpt("list", LongOpt.NO_ARGUMENT, null, 'l');
    longopts[i++] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
    longopts[i++] = new LongOpt("nomidi", LongOpt.NO_ARGUMENT, null, 'n');
    longopts[i++] = new LongOpt("debugmidi", LongOpt.NO_ARGUMENT, null, 0);

    Getopt g = new Getopt("Qualm", args, "o:i:hln", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c)
	{
	case 0:
	  // debugmidi option
	  debugMIDI = true;
	  break;
	case 'n':
	  skipMIDI = true;
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

    MidiDevice.Info inputInfo = null;
    MidiDevice.Info outputInfo = null;
   
    if (!skipMIDI) {
      MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
      if (infos.length == 0) {
	System.out.println( "No MIDI devices found.  Exiting." );
	System.exit(1);
      }
      
      boolean useAlsa = true;
      
      /* Find ALSA MIDI ports */
      List midiports = new ArrayList();
      for(i = 0; i<infos.length; i++) {
	if (infos[i].getName().startsWith("ALSA MIDI")) {
	  midiports.add(infos[i]);
	}
      }
      
      /* If we found no ALSA ports, then we'll use others */
      if (midiports.size() == 0) {
	useAlsa = false;
	for(i = 0; i<infos.length; i++) 
	  midiports.add(infos[i]);
      }
      
      if (midiports.size() == 0) {
	System.out.println("No MIDI ports found.  Exiting." );
	System.exit(1);
      }
      
      Map clientMap = null;
      if (useAlsa) clientMap = Qualm.parseALSAClients();

      if (listPorts) 
	System.out.println("MIDI ports:");
      
      Iterator iter = midiports.iterator();
      while (iter.hasNext()) {
	MidiDevice.Info info = (MidiDevice.Info)iter.next();
	String dev = info.getName();
	String cName = info.getDescription();
	if (useAlsa) {
	  dev = dev.substring(dev.indexOf('(')+1);
	  dev = dev.substring(0, dev.lastIndexOf( ':' ));
	  Integer cNum = new Integer(dev);
	  cName = (String)clientMap.get(cNum);
	}
	
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
      
      if (inputInfo == null ) {
	System.out.println("Unable to load input port named " + inputPort);
	System.exit(1);
      }
      if (outputInfo == null) {
	System.out.println("Unable to load output port named " + outputPort);
	System.exit(1);
      }
    } // !skipMIDI

    // get the transmitter and receiver
    Transmitter midiIn = null;
    Receiver midiOut = null;

    // Load the qualm file
    System.out.println("loading " + inputFilename + "...");
    QDataLoader qdl = new QDataLoader();
    QData data = qdl.readFile( new java.io.File( inputFilename ));

    if ( data == null && !debugMIDI ) {
      System.out.println("No readable files; exiting");
      System.exit(1);
    }

    try {
      if (!skipMIDI) {
	MidiDevice inDevice = MidiSystem.getMidiDevice( inputInfo );
	MidiDevice outDevice = MidiSystem.getMidiDevice( outputInfo );
	inDevice.open();
	outDevice.open();
	
	midiIn = inDevice.getTransmitter();
	midiOut = outDevice.getReceiver();
      } // !skipMIDI

      // open a receiver with the right data file.
      QController qc = new QController( midiOut, data );
      if (debugMIDI)
	qc.setDebugMIDI(true);

      // Start a read-eval-print loop as well.  The REPL will do a
      // System.exit when all is done.

      new QualmREPL( qc ).start();
      
      // connect the transmitter to the receiver.
      if (!skipMIDI) 
	midiIn.setReceiver( qc );

    } catch (MidiUnavailableException mue) {
      System.out.println(mue); 
    }
  }

}
