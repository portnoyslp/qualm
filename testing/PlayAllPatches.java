package qualm.testing;

import qualm.*;
import gnu.getopt.*;
import javax.sound.midi.*;
import java.util.*;
import qualm.Patch;

public class PlayAllPatches {

  static Transmitter midiIn = null;
  static Receiver midiOut = null;

  public static void loopThroughPatches(QData data) {
    TreeSet patches = new TreeSet( new Comparator() { 
	public int compare(Object a, Object b) {
	  return ((Patch)a).getID().compareTo( ((Patch)b).getID() );
	}	  
      });
    patches.addAll(data.getPatches());

    Iterator iter = patches.iterator();
    while(iter.hasNext()) {
      Patch p = (Patch)iter.next();
      System.out.println("Switching to patch " + p);
      PatchChanger.patchChange( new ProgramChangeEvent( 0, p ),
				midiOut );
      // play a chord: c4, e4, g4, c5 (60,64,67,72)
      
      try {
	ShortMessage out = new ShortMessage();
	out.setMessage( ShortMessage.NOTE_ON, 0, 60, 64 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 64, 64 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 67, 64 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 72, 64 );
	midiOut.send(out, -1);
      
	// delay 1s
	Thread.currentThread().sleep(1000);

	out.setMessage( ShortMessage.NOTE_ON, 0, 60, 0 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 64, 0 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 67, 0 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 72, 0 );
	midiOut.send(out, -1);

	// delay another 1s
	Thread.currentThread().sleep(1000);
      } catch (Exception e) {
	e.printStackTrace();
	return;
      }
    }
  }


  public static void main(String[] args) {

    String inputPort = null;
    String outputPort = null;

    int i = 0;
    LongOpt[] longopts = new LongOpt[6];
    longopts[i++] = new LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o');
    longopts[i++] = new LongOpt("input", LongOpt.REQUIRED_ARGUMENT, null, 'i');
    Getopt g = new Getopt("Qualm", args, "o:i:", longopts);

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
	}
    }


    if (g.getOptind() == args.length) {
      System.out.println("No filename given.");
      System.exit(0);
    }
    String inputFilename = args[g.getOptind()];

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


    // set ports
    if (inputPort == null && outputPort == null) 
      inputPort="UM-1";
    if (outputPort == null) 
      outputPort = inputPort;
    if (inputPort == null)
      inputPort = outputPort;

    MidiDevice.Info[] ports = Qualm.getMidiPorts(inputPort, outputPort, false, false);
    MidiDevice.Info inputInfo = ports[0];
    MidiDevice.Info outputInfo = ports[1];
    if (inputInfo == null ) {
      System.out.println("Unable to load input port named " + inputPort);
      System.exit(1);
    }
    if (outputInfo == null) {
      System.out.println("Unable to load output port named " + outputPort);
      System.exit(1);
    }

    // get the transmitter and receiver
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


    loopThroughPatches(data);
    System.exit(0);
  }

}

