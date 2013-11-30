package qualm.utils;

import static qualm.MidiCommand.NOTE_ON;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import qualm.JavaMidiReceiverFactory;
import qualm.MidiCommand;
import qualm.Patch;
import qualm.PatchChanger;
import qualm.ProgramChangeEvent;
import qualm.QData;
import qualm.QDataLoader;
import qualm.QReceiver;

public class AuditionPatches {

  static QReceiver midiOut = null;
  static Patch defaultPatch = null;
  static boolean playSingle = false;
  static boolean ignoreAliases = false;

  public static void playArpeggiatedChord(int holdTime) {
    // play a Cmaj chord with increasing strength from p (30) to ff
    // (96) [ c4, e4, g4, c5 (60,64,67,72)]

    try {
      // slight delay to give synth time to process patch change
      Thread.sleep(500);

      if (playSingle) {
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 60, 70 ));
      } else {
	
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 60, 30 ));
	Thread.sleep(500);
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 64, 52 ));
	Thread.sleep(500);
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 67, 74 ));
	Thread.sleep(500);
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 72, 96 ));
      }

      // let it ring before silencing.
      Thread.sleep(holdTime);

      if (playSingle) {
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 60, 0 ));

      } else {
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 60, 0 ));
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 64, 0 ));
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 67, 0 ));
	midiOut.handleMidiCommand(new MidiCommand( NOTE_ON, 0, 72, 0 ));
      }
      
      // delay another 1s
      Thread.sleep(800);
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
    PatchChanger.patchChange( new ProgramChangeEvent( 0, null, p ),
			      midiOut );
    playArpeggiatedChord( 1000 );
    PatchChanger.patchChange( new ProgramChangeEvent( 0, null, defaultPatch ),
			      midiOut );
    playArpeggiatedChord( 1000 );
    PatchChanger.patchChange( new ProgramChangeEvent( 0, null, p ),
			      midiOut );
    playArpeggiatedChord( 1500 );
    
  }

  public static void prompt(Patch p) {
    System.out.print("Auditioning patch " + p + "> ");
    System.out.flush();
  }

  private static TreeSet<Patch> setupPatches() {
    TreeSet<Patch> patches = new TreeSet<Patch>( new Comparator<Patch>() { 
	public int compare(Patch a, Patch b) {
	  return a.getID().compareTo( b.getID() );
	}	  
      });
    patches.addAll(data.getPatches());
    return patches;
  }

  public static void loopThroughPatches() {
    TreeSet<Patch> patches = setupPatches(); 

    BufferedReader reader = 
      new BufferedReader( new InputStreamReader( System.in ));

    Iterator<Patch> iter = patches.iterator();
    boolean startRun = true;
    while(iter.hasNext()) {
      Patch p = iter.next();
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

  private static Iterator<Patch> iteratorForPatch(Patch target, TreeSet<Patch> patches) {
    // the insanity of having to replace an iterator on the
    // fly.  Here we go through to find the new patch, but
    // we really need to find the *preceding* patch so we
    // know when to stop.
    Patch preceding = null;
    Iterator<Patch> iter2 = patches.iterator();
    while (iter2.hasNext()) {
      Patch q = iter2.next();
      if (q.equals(target)) break;
      preceding = q;
    }
    iter2 = patches.iterator();
    if (preceding != null) {
      while (iter2.hasNext()) {
	if ( iter2.next().equals(preceding) )
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
    try {
      data = qdl.load(inputFilename);
    } catch (IOException e) {
      throw new RuntimeException("Unable to load file: " + inputFilename, e);
    }
  }

  public static void main(String[] args) {

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
    
    // Set up MIDI ports
    Properties props = new Properties();
    props.setProperty("outputPort", outputPort);
    midiOut = new JavaMidiReceiverFactory().buildFromProperties(props);

    loopThroughPatches();
    System.exit(0);
  }

}

