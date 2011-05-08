package qualm;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public class Qualm {
  
  public static final Logger LOG = Logger.getLogger("Qualm.core");
  
  private static void usage() {
    System.out.println("Usage: java qualm.Qualm <options> <filename>");
    System.out.println("  --output <out>");
    System.out.println("  -o <out>        Sets output ALSA port.");
    System.out.println("  --input <in>");
    System.out.println("  -i <in>         Sets input ALSA port.");
    System.out.println("  --nomidi | -n   Ignore MIDI ports");
    System.out.println("  --debugmidi     Print debug output, including received MIDI events.");
    System.out.println("  --lint          Carefully check the input file for errors.");
    System.out.println("  --version | -v  Give information on the build identifier.");
    System.out.println("  --help | -h     Prints this message.");
    System.out.println("  <filename>      The Qualm filename to execute.");
    System.out.println("\n If only one port is specified, an attempt will be made to open the\nport for both input and output.  If no port is specified, Qualm will\nsearch for common USB-MIDI cable names.");
  }

  public static void loadProperties() {
    String propsFile = "qualm.properties";
    try {
      System.getProperties().load(ClassLoader.getSystemResource(propsFile).openStream());
    } catch (java.io.IOException ioe) {
      System.err.println("Unable to open properties file " + propsFile + ": " + ioe.getMessage());
    } catch (NullPointerException ne) {
      System.err.println("Unable to find properties file " + propsFile);
    }
  }

  public static String versionString() {
    return "Qualm v" + System.getProperty("qualm.version.number") +
      " (build " + System.getProperty("qualm.version.build") + ")";
  }

  public static QData loadQDataFromFilename(String inputFilename) {
    QData data;

    // Load the qualm file
    QDataLoader qdl = new QDataLoader( validateInput );

    if (inputFilename.startsWith("http:") ||
	inputFilename.startsWith("ftp:") ||
	inputFilename.startsWith("file:")) {
      // assume we have a URL
      data = qdl.readSource( new org.xml.sax.InputSource(inputFilename));
    } else {
      data = qdl.readFile( new java.io.File( inputFilename ));
    }

    return data;
  }

  public static void main(String[] args) throws Exception {

    loadProperties();

    String inputPort = null;
    String outputPort = null;
    boolean debugMIDI = false;
    boolean skipMIDI = false;

    // handle argument list
    int i = 0;
    LongOpt[] longopts = new LongOpt[8];
    longopts[i++] = new LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o');
    longopts[i++] = new LongOpt("input", LongOpt.REQUIRED_ARGUMENT, null, 'i');
    longopts[i++] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
    longopts[i++] = new LongOpt("nomidi", LongOpt.NO_ARGUMENT, null, 'n');
    longopts[i++] = new LongOpt("debug", LongOpt.NO_ARGUMENT, null, 0);
    longopts[i++] = new LongOpt("debugmidi", LongOpt.NO_ARGUMENT, null, 0);
    longopts[i++] = new LongOpt("lint", LongOpt.NO_ARGUMENT, null, 1);
    longopts[i++] = new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v');

    Getopt g = new Getopt("Qualm", args, "o:i:hlnv", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c)
	{
	case 0:
	  // debugmidi option
	  debugMIDI = true;
	  break;
	case 1:
	  // lint option
	  validateInput = true;
	case 'n':
	  skipMIDI = true;
	case 'i':
	  inputPort = g.getOptarg();
	  break;
	case 'o':
	  outputPort = g.getOptarg();
	  break;
	case 'h':
	  usage();
	case 'v':
	  System.out.println(versionString());
	  System.exit(0);
	}
    }

    // set up output logging; don't use the parent, so that we can skip the standard logging.
    // TODO redo so that we can actually use the standard logging config files?
    LOG.setUseParentHandlers(false);
    Handler handler = new ConsoleHandler();
    handler.setLevel(Level.FINER);
    LOG.addHandler(handler);
    
    if (debugMIDI) {
      LOG.setLevel(Level.FINER);
    } else {
      LOG.setLevel(Level.INFO);
    }
    
    String inputFilename = null;
    if (g.getOptind() < args.length) {
      inputFilename = args[g.getOptind()];
    }

    // Set up MIDI ports
    Properties props = new Properties();
    if (inputPort != null) props.setProperty("inputPort", inputPort);
    if (outputPort != null) props.setProperty("outputPort", outputPort);
    JavaMidiReceiver jmr = null;
    if (!skipMIDI)
      try {
        jmr = new JavaMidiReceiver(props);       
      } catch (Exception e) {
        LOG.warning(e.getMessage());
        System.exit(0);
      }
    
    MasterController mc = new MasterController( jmr );
    if (debugMIDI) mc.setDebugMIDI(true);

    QualmREPL repl = new QualmREPL();
    repl.setMasterController( mc );

    // connect the transmitter to the receiver.
    if (!skipMIDI)
      jmr.setTarget( mc );

    if (inputFilename == null) {
      System.out.println("No filename given.\n");
      usage();
      System.exit(0);
    }

    repl.loadFilename( inputFilename );
  
    // start the REPL
    repl.start();
  }

  static boolean validateInput = false;

}

