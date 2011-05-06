package qualm.testing;

import qualm.*;
import gnu.getopt.*;
import javax.sound.midi.*;
import java.util.*;

import qualm.Patch;

public class PlayAllPatches {

  static QReceiver midiIn = null;
  static QReceiver midiOut = null;

  public static void loopThroughPatches(QData data) {
    TreeSet<Patch> patches = new TreeSet<Patch>( new Comparator<Patch>() { 
	public int compare(Patch a, Patch b) {
	  return a.getID().compareTo( b.getID() );
	}	  
      });
    patches.addAll(data.getPatches());

    Iterator<Patch> iter = patches.iterator();
    while(iter.hasNext()) {
      Patch p = iter.next();
      System.out.println("Switching to patch " + p);
      PatchChanger.patchChange( new ProgramChangeEvent( 0, null, p ),
				midiOut );
      // play a chord: c4, e4, g4, c5 (60,64,67,72)
      
      try {
	MidiCommand out = new MidiCommand();
	out.setMessage( MidiCommand.NOTE_ON, 0, 60, 64 );
	midiOut.handleMidiCommand(out);
	out.setMessage( MidiCommand.NOTE_ON, 0, 64, 64 );
	midiOut.handleMidiCommand(out);
	out.setMessage( MidiCommand.NOTE_ON, 0, 67, 64 );
	midiOut.handleMidiCommand(out);
	out.setMessage( MidiCommand.NOTE_ON, 0, 72, 64 );
	midiOut.handleMidiCommand(out);
      
	// delay 1s
	Thread.sleep(1000);

	out.setMessage( MidiCommand.NOTE_ON, 0, 60, 0 );
	midiOut.handleMidiCommand(out);
	out.setMessage( MidiCommand.NOTE_ON, 0, 64, 0 );
	midiOut.handleMidiCommand(out);
	out.setMessage( MidiCommand.NOTE_ON, 0, 67, 0 );
	midiOut.handleMidiCommand(out);
	out.setMessage( MidiCommand.NOTE_ON, 0, 72, 0 );
	midiOut.handleMidiCommand(out);

	// delay another 1s
	Thread.sleep(1000);
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
    
    // Set up MIDI ports
    Properties props = new Properties();
    props.setProperty("inputPort", inputPort);
    props.setProperty("outputPort", outputPort);
    midiOut = new JavaMidiReceiver(props);
    midiIn = midiOut;

    loopThroughPatches(data);
    System.exit(0);
  }

}

