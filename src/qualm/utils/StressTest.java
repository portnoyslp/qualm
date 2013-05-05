package qualm.utils;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import qualm.Cue;
import qualm.EventTemplate;
import qualm.JavaMidiReceiver;
import qualm.MidiCommand;
import qualm.QData;
import qualm.QDataLoader;
import qualm.QReceiver;
import qualm.QStream;
import qualm.Trigger;

public class StressTest {

  static QReceiver midiIn = null;
  static QReceiver midiOut = null;
    
  public static void runLoop(QData data) {
    // get all the cues.  Note that we can't use the normal
    // comparator for Cues, we have to use our own which will allow
    // two cues with the same measure number to exist in the set.
    Comparator<Cue> cueCompare = new Comparator<Cue>() {
	public int compare(Cue a, Cue b) {
	  int out = a.compareTo(b);
	  if (out == 0)
	    return a.hashCode()-b.hashCode();
	  else return out;
	}
      };
    TreeSet<Cue> cues = new TreeSet<Cue>(cueCompare);
    Iterator<QStream> iter = data.getCueStreams().iterator();
    while (iter.hasNext()) {
      QStream qs = iter.next();
      cues.addAll(qs.getCues());
    }

    // OK, we now have a list of cues
    Iterator<Cue> cIter = cues.iterator();
    while (cIter.hasNext()) {
      Cue q = cIter.next();
      System.out.println("Executing trigger for " + q);
      
      // for each cue, pick a trigger and execute it.
      Collection<Trigger> coll = q.getTriggers();
      Iterator<Trigger> j = coll.iterator();
      Trigger trig = null;
      while(j.hasNext()) {
	trig = j.next();
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
      try {  Thread.sleep(1500); } catch (InterruptedException ie) { }

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

    // Set up MIDI ports
    Properties props = new Properties();
    props.setProperty("inputPort", inputPort);
    props.setProperty("outputPort", outputPort);
    midiOut = JavaMidiReceiver.buildFromProperties(props);
    midiIn = midiOut;

    runLoop(data);
    System.exit(0);
  }

}

  class InstantiatableTemplate extends EventTemplate {
    public InstantiatableTemplate(EventTemplate et) { super(et); }

    public void sendMessage(QReceiver midiOut) {
      if (type == MidiCommand.NOTE_ON) {
	int ch = channel;
	int note = extra1Min;
	midiOut.handleMidiCommand(new MidiCommand( MidiCommand.NOTE_ON, ch, note, 61 ));
	midiOut.handleMidiCommand(new MidiCommand( MidiCommand.NOTE_ON, ch, note, 0 ));
      }
    }
  }


