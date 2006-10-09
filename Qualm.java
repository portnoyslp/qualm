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
    
  public static void main(String[] args) throws Exception {

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
	MidiDevice md = null;
	try {
	  md = MidiSystem.getMidiDevice(info);
	  if (listPorts && debugMIDI)
	    System.out.println("   [trans:" + md.getMaxTransmitters() + " rec:" + md.getMaxReceivers() + "]");
	} catch (MidiUnavailableException mue) { System.out.println(info.getName() + " unavailable"); }

	if ((cName.indexOf(inputPort) != -1 ||
	     info.getName().indexOf(inputPort) != -1) &&
	    md!=null && md.getMaxTransmitters() != 0) {
	  inputInfo = info;
	  if (debugMIDI)
	    System.out.println("Using " + inputInfo.getName() + " (" + cName + ") for input.");
	}
	
	if ((cName.indexOf(outputPort) != -1 ||
	     info.getName().indexOf(outputPort) != -1) &&
	    md!=null && md.getMaxReceivers() != 0) { 
	  outputInfo = info;
	  if (debugMIDI)
	    System.out.println("Using " + outputInfo.getName() + " (" + cName + ") for output.");
	}
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
    QData data;

    if (inputFilename.startsWith("http:") ||
	inputFilename.startsWith("ftp:") ||
	inputFilename.startsWith("file:")) {
      // assume we have a URL
      data = qdl.readSource( new org.xml.sax.InputSource(inputFilename));
    } else {
      data = qdl.readFile( new java.io.File( inputFilename ));
    }

    if ( data == null && !debugMIDI ) {
      System.out.println("No readable files; exiting");
      System.exit(1);
    }

    if (!skipMIDI) {
      try {      	
	MidiDevice inDevice = MidiSystem.getMidiDevice( inputInfo );
	inDevice.open();
	midiIn = inDevice.getTransmitter();
      } catch (MidiUnavailableException mdu1) {
	System.out.println("Unable to open device for input:" + mdu1);
      }
      
      try {
	MidiDevice outDevice = MidiSystem.getMidiDevice( outputInfo );
	outDevice.open();
	midiOut = outDevice.getReceiver();
      } catch (MidiUnavailableException mdu2) {
	System.out.println("Unable to open device for output:" + mdu2);
      }
    } // !skipMIDI


    // prepare patches properly
    data.prepareCueStreams();

    MultiplexReceiver mrec = new MultiplexReceiver();
    if (debugMIDI) mrec.setDebugMIDI(true);

    QualmREPL repl = new QualmREPL();
    repl.setMultiplexReceiver( mrec );

    // For each cue stream, start a controller
    boolean first = true;
    Iterator iter = data.getCueStreams().iterator();
    while (iter.hasNext()) {
      QController qc = new QController( midiOut, 
					(QStream)iter.next(),
					data );

      repl.addController(qc);
      mrec.addReceiver(qc);
    }

    // connect the transmitter to the receiver.
    if (!skipMIDI)
      midiIn.setReceiver( mrec );
  
    // start the REPL
    repl.start();
      
  }

}
