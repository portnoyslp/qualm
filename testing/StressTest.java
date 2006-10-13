package qualm.testing;

import qualm.*;
import gnu.getopt.*;
import javax.sound.midi.*;
import java.util.*;

public class StressTest {


  static Transmitter midiIn = null;
  static Receiver midiOut = null;
    
  public static void runLoop(QData data) {
    // get all the cues
    TreeSet cues = new TreeSet();
    Iterator iter = data.getCueStreams().iterator();
    while (iter.hasNext()) {
      QStream qs = (QStream)iter.next();
      cues.addAll(qs.getCues());
    }

    // OK, we now have a list of cues
    iter = cues.iterator();
    while (iter.hasNext()) {
      Cue q = (Cue)iter.next();
      System.out.println("Executing trigger for " + q);
      
      // for each cue, pick a trigger and execute it.
      Collection coll = q.getTriggers();
      Iterator j = coll.iterator();
      Trigger trig = null;
      while(j.hasNext()) {
	trig = (Trigger)j.next();
	if (!trig.getReverse())
	  break;
      }
      if (trig == null) {
	System.out.println("Could not find trigger leaving " + q);
	System.exit(1);
      }
      
      // OK, we found a trigger.
      InstantiatableTemplate t = 
	new InstantiatableTemplate(trig.getTemplate());
      t.sendMessage(midiOut);

      // pause for a little while
      try {  Thread.currentThread().sleep(500); } catch (InterruptedException ie) { }

    }
    System.out.println("Done with cues");
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


    runLoop(data);
    System.exit(0);
  }

}

  class InstantiatableTemplate extends EventTemplate {
    public InstantiatableTemplate(EventTemplate et) { super(et); }

    public void sendMessage(Receiver out) {
      if (type == ShortMessage.NOTE_ON) {
	int ch = channel;
	int note = extra1;
	try {
	  ShortMessage msg = new ShortMessage();
	  msg.setMessage( ShortMessage.NOTE_ON, ch, note, 61 );
	  out.send(msg, -1);
	  msg.setMessage( ShortMessage.NOTE_ON, ch, note, 0 );
	  out.send(msg, -1);
	} catch (InvalidMidiDataException imde) {
	  System.out.println("Could not build messages matching " + toString() );
	}
      }
    }
  }


