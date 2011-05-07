package qualm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

/**
 * Adaptor for translating between javax.sound.midi messages and Qualm messaging.
 * 
 * @author speters
 */
public class JavaMidiReceiver extends AbstractQReceiver implements QReceiver, Receiver {

  QReceiver qr;
  Transmitter midiIn;
  Receiver midiOut;
  
  public JavaMidiReceiver(Properties props) {
    buildMidiHandlers(props);
  }
  
  /* Receives the given MidiCommand from Qualm and sends it out through the MIDI interface.
   * @see qualm.BasicReceiver#handleMidiCommand(qualm.MidiCommand)
   */
  public void handleMidiCommand(MidiCommand mc) {
    ShortMessage sm = new ShortMessage();
    try {
      sm.setMessage(mc.getType(),mc.getChannel(),mc.getData1(),mc.getData2());
      midiOut.send(sm, -1);
    } catch (InvalidMidiDataException ume) {
      // ignore MIDI problems.
    }
  }

  public void close() {
    midiOut.close();
  }

  /* Receives a MidiMessage from the internal Transmitter, and sends it out through the forwarding target.
   * @see javax.sound.midi.Receiver#send(javax.sound.midi.MidiMessage, long)
   */
  public void send(MidiMessage message, long timeStamp) {
    if (message instanceof ShortMessage) {
      ShortMessage sm = (ShortMessage)message;
      MidiCommand mc = new MidiCommand(sm.getChannel(),sm.getCommand(),sm.getData1(),sm.getData2());
      getTarget().handleMidiCommand(mc);
    }
    // TODO: Handle other message types
  }

  public static Map<Integer, String> parseALSAClients() {
    Map<Integer, String> ret = new HashMap<Integer, String>();
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
  
  /**
   * Builds the internal MIDI handlers for this instance, using the "inputPort"
   * and "outputPort" properties, if present.
   * 
   * @param props
   */
  private void buildMidiHandlers(Properties props) {
    String inputPort = props.getProperty("inputPort");
    String outputPort = props.getProperty("outputPort");
    boolean debugMIDI = Boolean.parseBoolean(props.getProperty("debug"));
    
    String DEFAULT_PORT="__default_port__";

    if (inputPort == null && outputPort == null) 
      inputPort=DEFAULT_PORT;
    if (outputPort == null) 
      outputPort = inputPort;
    if (inputPort == null)
      inputPort = outputPort;

    MidiDevice.Info inputInfo = null;
    MidiDevice.Info outputInfo = null;
   
    if (inputPort.equals(DEFAULT_PORT)) {
      // try the various default port names
      for(int i=0; i<defaultPortNames.length; i++) {
        MidiDevice.Info[] ports = getMidiPorts(defaultPortNames[i], defaultPortNames[i], debugMIDI);
        inputInfo = ports[0];
        outputInfo = ports[1];
        
        if (inputInfo != null) break;
      }
    } else {
      MidiDevice.Info[] ports = getMidiPorts(inputPort, outputPort, debugMIDI);
      inputInfo = ports[0];
      outputInfo = ports[1];
    }
          
    if (inputInfo == null ) {
      if (inputPort.equals(DEFAULT_PORT)) {
        throw new RuntimeException("Unable to load default input port");
      } else {
        throw new RuntimeException("Unable to load input port named " + inputPort);
      }
    }
    if (outputInfo == null) {
      throw new RuntimeException("Unable to load output port named " + outputPort);
    }
    
    try {         
      MidiDevice inDevice = MidiSystem.getMidiDevice( inputInfo );
      inDevice.open();
      midiIn = inDevice.getTransmitter();
    } catch (MidiUnavailableException mdu1) {
      throw new RuntimeException("Unable to open device for input:" + mdu1);
    }
    
    try {
      MidiDevice outDevice = MidiSystem.getMidiDevice( outputInfo );
      outDevice.open();
      midiOut = outDevice.getReceiver();
    } catch (MidiUnavailableException mdu2) {
      throw new RuntimeException("Unable to open device for output:" + mdu2);
    }    
  }
  
  private static MidiDevice.Info[] getMidiPorts(String inputPort, String outputPort, boolean debugMIDI) {
    MidiDevice.Info[] out = new MidiDevice.Info[2];
    out[0] = null;
    out[1] = null;

    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    if (infos.length == 0) {
      System.out.println("No MIDI devices found.");
      return out;
    }

    boolean useAlsa = true;

    /* Find ALSA MIDI ports */
    List<MidiDevice.Info> midiports = new ArrayList<MidiDevice.Info>();
    int i;
    for (i = 0; i < infos.length; i++) {
      if (infos[i].getName().startsWith("ALSA MIDI")) {
        midiports.add(infos[i]);
      }
    }

    /* If we found no ALSA ports, then we'll use others */
    if (midiports.size() == 0) {
      useAlsa = false;
      for (i = 0; i < infos.length; i++)
        midiports.add(infos[i]);
    }

    if (midiports.size() == 0) {
      System.out.println("No MIDI ports found.  Exiting.");
      System.exit(1);
    }

    Map<Integer, String> clientMap = null;
    if (useAlsa)
      clientMap = JavaMidiReceiver.parseALSAClients();

    if (debugMIDI)
      System.out.println("MIDI ports:");

    Iterator<MidiDevice.Info> iter = midiports.iterator();
    while (iter.hasNext()) {
      MidiDevice.Info info = iter.next();
      String dev = info.getName();
      String cName = info.getDescription();
      if (useAlsa) {
        dev = dev.substring(dev.indexOf('(') + 1);
        dev = dev.substring(0, dev.lastIndexOf(':'));
        Integer cNum = new Integer(dev);
        cName = (String) clientMap.get(cNum);
      }

      if (debugMIDI)
        System.out.println("  " + info.getName() + " [" + cName + "]");

      // Is this a port we want?
      MidiDevice md = null;
      try {
        md = MidiSystem.getMidiDevice(info);
        if (debugMIDI)
          System.out.println("   [trans:" + md.getMaxTransmitters() + " rec:"
              + md.getMaxReceivers() + "]");
      } catch (MidiUnavailableException mue) {
        System.out.println(info.getName() + " unavailable");
      }

      if (inputPort != null) {
        if ((cName.indexOf(inputPort) != -1 || info.getName()
            .indexOf(inputPort) != -1)
            && md != null && md.getMaxTransmitters() != 0) {
          out[0] = info;
          if (debugMIDI)
            System.out.println("Using " + out[0].getName() + " (" + cName
                + ") for input.");
        }
      }

      if (outputPort != null) {
        if ((cName.indexOf(outputPort) != -1 || info.getName().indexOf(
            outputPort) != -1)
            && md != null && md.getMaxReceivers() != 0) {
          out[1] = info;
          if (debugMIDI)
            System.out.println("Using " + out[1].getName() + " (" + cName
                + ") for output.");
        }
      }
    }

    return out;
  }
  
  private static String[] defaultPortNames = {
    "UM-1",
    "USB Midi"
  };

}
