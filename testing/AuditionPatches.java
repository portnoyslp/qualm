package qualm.testing;

import qualm.*;
import gnu.getopt.*;
import javax.sound.midi.*;
import java.util.*;
import qualm.Patch;

import java.io.*;

public class AuditionPatches {

  static Receiver midiOut = null;
  static Patch defaultPatch = null;
  static boolean playSingle = false;

  public static void playArpeggiatedChord(int holdTime) {
    // play a Cmaj chord with increasing strength from p (30) to ff
    // (96) [ c4, e4, g4, c5 (60,64,67,72)]

    try {
      ShortMessage out = new ShortMessage();

      if (playSingle) {
	out.setMessage( ShortMessage.NOTE_ON, 0, 60, 60 );
	midiOut.send(out, -1);

      } else {
	
	out.setMessage( ShortMessage.NOTE_ON, 0, 60, 30 );
	midiOut.send(out, -1);
	Thread.currentThread().sleep(500);
	out.setMessage( ShortMessage.NOTE_ON, 0, 64, 52 );
	midiOut.send(out, -1);
	Thread.currentThread().sleep(500);
	out.setMessage( ShortMessage.NOTE_ON, 0, 67, 74 );
	midiOut.send(out, -1);
	Thread.currentThread().sleep(500);
	out.setMessage( ShortMessage.NOTE_ON, 0, 72, 96 );
	midiOut.send(out, -1);
      }

      // let it ring before silencing.
      Thread.currentThread().sleep(holdTime);

      if (playSingle) {
	out.setMessage( ShortMessage.NOTE_ON, 0, 60, 0 );
	midiOut.send(out, -1);

      } else {

	out.setMessage( ShortMessage.NOTE_ON, 0, 60, 0 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 64, 0 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 67, 0 );
	midiOut.send(out, -1);
	out.setMessage( ShortMessage.NOTE_ON, 0, 72, 0 );
	midiOut.send(out, -1);
      }
      
      // delay another 1s
      Thread.currentThread().sleep(800);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
  }

  public static void auditionPatch(Patch p) {

    if (midiOut == null) {
      System.out.println("No MIDI output; pretending to audition a " +
			 (playSingle?"note.":"chord."));
      return;
    }

    // for each patch, we play it, then we play the default, then we
    // play the original again with a slightly longer hold time.
    PatchChanger.patchChange( new ProgramChangeEvent( 0, p ),
			      midiOut );
    playArpeggiatedChord( 800 );
    PatchChanger.patchChange( new ProgramChangeEvent( 0, defaultPatch ),
			      midiOut );
    playArpeggiatedChord( 800 );
    PatchChanger.patchChange( new ProgramChangeEvent( 0, p ),
			      midiOut );
    playArpeggiatedChord( 1500 );
    
  }

  public static void prompt(Patch p) {
    System.out.print("Auditioning patch " + p + "> ");
    System.out.flush();
  }

  public static void loopThroughPatches(QData data) {
    TreeSet patches = new TreeSet( new Comparator() { 
	public int compare(Object a, Object b) {
	  return ((Patch)a).getID().compareTo( ((Patch)b).getID() );
	}	  
      });
    patches.addAll(data.getPatches());

    BufferedReader reader = 
      new BufferedReader( new InputStreamReader( System.in ));

    Iterator iter = patches.iterator();
    while(iter.hasNext()) {
      Patch p = (Patch)iter.next();
      prompt(p);
      
      boolean next = false;
      while (!next) {
	auditionPatch(p);

	try { 
	  String line = reader.readLine();
	  if (line == null || line.trim().equals("")) {
	    // re-audition by continuing in loop...
	    prompt(p);
	  } else if (line.toLowerCase().equals("n") ||
		     line.toLowerCase().equals("next")) {
	    // next patch in sequence.
	    next = true;
	  } else {
	    // we might be specifying a patch name; try to move to it if so
	    Patch target = data.lookupPatch(line.trim());
	    if (target == null) {
	      System.out.println("Unable to switch to patch '" + line.trim() +
				 "'; ignoring.");
	    } else {
	      // the insanity of having to replace an iterator on the
	      // fly.  Here we go through to find the new patch, but
	      // we really need to find the *preceding* patch so we
	      // know when to stop.
	      Patch preceding = null;
	      Iterator iter2 = patches.iterator();
	      while (iter2.hasNext()) {
		Patch q = (Patch)iter2.next();
		if (q.equals(target)) break;
		preceding = q;
	      }
	      iter2 = patches.iterator();
	      if (preceding != null) {
		while (iter2.hasNext()) {
		  if ( ((Patch)iter2.next()).equals(preceding) )
		    break;
		}
	      }
	      iter = iter2;
	      next = true;
	    }
	  }
	} catch (Exception e) {
	  e.printStackTrace();
	}
      }
      
    }
  }


  public static void main(String[] args) {

    String inputPort = null;
    String outputPort = null;

    int i = 0;
    LongOpt[] longopts = new LongOpt[6];
    longopts[i++] = new LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o');
    longopts[i++] = new LongOpt("default", LongOpt.REQUIRED_ARGUMENT, null, 'p');
    longopts[i++] = new LongOpt("single", LongOpt.NO_ARGUMENT, null, 's');
    Getopt g = new Getopt("Qualm", args, "o:p:s", longopts);

    int c;
    String defaultPatchName = "Piano";
    while ((c = g.getopt()) != -1) {
      switch(c)
	{
	case 'o':
	  outputPort = g.getOptarg();
	  break;
	case 'p':
	  defaultPatchName = g.getOptarg();
	  break;
	case 's':
	  playSingle = true;
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

    // get the default patch
    defaultPatch = data.lookupPatch( defaultPatchName );
    if (defaultPatch == null) {
      System.out.println("Unable to find default patch with name '" 
			 + defaultPatchName +"'");
      System.exit(1);
    }

    // set ports
    if (outputPort == null) 
      outputPort="UM-1";

    MidiDevice.Info[] ports = Qualm.getMidiPorts(null, outputPort, false, false);
    MidiDevice.Info outputInfo = ports[1];
    if (outputInfo == null) {
      System.out.println("Unable to load output port named " + outputPort);
      System.exit(1);
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

