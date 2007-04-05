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
  static boolean ignoreAliases = false;

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

  private static TreeSet setupPatches() {
    TreeSet patches = new TreeSet( new Comparator() { 
	public int compare(Object a, Object b) {
	  return ((Patch)a).getID().compareTo( ((Patch)b).getID() );
	}	  
      });
    patches.addAll(data.getPatches());
    return patches;
  }

  public static void loopThroughPatches() {
    TreeSet patches = setupPatches(); 

    BufferedReader reader = 
      new BufferedReader( new InputStreamReader( System.in ));

    Iterator iter = patches.iterator();
    boolean startRun = true;
    while(iter.hasNext()) {
      Patch p = (Patch)iter.next();
      prompt(p);
      
      boolean advancePatch = false;
      while (!advancePatch) {
	if (!startRun)
	  auditionPatch(p);
	startRun = false;

	try { 
	  String line = reader.readLine();
	  line = line.trim();
	  if (line == null || line.equals("")) {
	    // re-audition by continuing in loop...
	    prompt(p);
	  } else if (line.toLowerCase().equals("n") ||
		     line.toLowerCase().equals("next")) {
	    // next patch in sequence.
	    advancePatch = true;
	  } else if (line.toLowerCase().equals("quit")) {
	    System.exit(0);
	  } else if (line.toLowerCase().equals("single")) {
	    playSingle = true;
	  } else if (line.toLowerCase().equals("multiple") ||
		     line.toLowerCase().equals("chord")) {
	    playSingle = false;
	  } else if (line.toLowerCase().equals("reload")) {
	    String targetName = p.getID();
	    loadData();
	    patches = setupPatches();
	    Patch target = data.lookupPatch(targetName);
	    if (target == null) {
	      System.out.println("Could not find current patch '" + target + 
				 "' in reload of data; starting from the beginning.");
	      iter = patches.iterator();
	    } else {
	      iter = iteratorForPatch(target, patches);
	      advancePatch = true;
	    }
	  } else {
	    // we might be specifying a patch name; try to move to it if so
	    Patch target = data.lookupPatch(line);
	    if (target == null) {
	      System.out.println("Unable to switch to patch '" + line +
				 "'; ignoring.");
	    } else {
	      iter = iteratorForPatch(target, patches);
	      advancePatch = true;
	    }
	  }
	} catch (Exception e) {
	  e.printStackTrace();
	}
      }
      
    }
  }

  private static Iterator iteratorForPatch(Patch target, TreeSet patches) {
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
    return iter2;
  }

  static String inputFilename;
  static QData data;

  private static void loadData() {
    if (inputFilename == null)
      throw new RuntimeException("loadData called without inputFilename in place.");
    
    QDataLoader qdl = new QDataLoader();
    qdl.setIgnorePatchAliases(ignoreAliases);
    
    if (inputFilename.startsWith("http:") ||
	inputFilename.startsWith("ftp:") ||
	inputFilename.startsWith("file:")) {
      // assume we have a URL
      data = qdl.readSource( new org.xml.sax.InputSource(inputFilename));
    } else {
      data = qdl.readFile( new java.io.File( inputFilename ));
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
    longopts[i++] = new LongOpt("ignore-aliases", LongOpt.NO_ARGUMENT, null, 'i');
    Getopt g = new Getopt("Qualm", args, "o:p:si", longopts);

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
	case 'i':
	  ignoreAliases = true;
	  break;
	}
    }

    if (g.getOptind() == args.length) {
      System.out.println("No filename given.");
      System.exit(0);
    }
    inputFilename = args[g.getOptind()];

    loadData();

    // get the default patch
    defaultPatch = data.lookupPatch( defaultPatchName );
    if (defaultPatch == null) {
      System.out.println("Unable to find default patch with name '" 
			 + defaultPatchName +"'");
      System.out.println("(use \"-p 'patchName'\" to specify a different default patch)");
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

    loopThroughPatches();
    System.exit(0);
  }

}

