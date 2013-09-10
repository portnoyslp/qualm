package qualm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

public class JavaMidiReceiverFactory implements AbstractQReceiverFactory {
  private static final String ALSA_CLIENTS_FILE = "/proc/asound/seq/clients";
  private static final String DEFAULT_PORT = "__default_port__";

  private static List<MidiDevice.Info> midiDeviceCache = null;
  private static Map<MidiDevice.Info, String> deviceDescriptionMap = null;

  @Override
  public JavaMidiReceiver buildFromProperties(Properties props) {
    return buildMidiHandlers(props);
  }

  /**
   * Builds the internal MIDI handlers for this instance, using the "inputPort"
   * and "outputPort" properties, if present.
   */
  private static JavaMidiReceiver buildMidiHandlers(Properties props) {
    Receiver rec;
    Transmitter trans;

    MidiDevice.Info[] devices = fetchDeviceInformation(
        props.getProperty("inputPort"), props.getProperty("outputPort"));

    MidiDevice.Info inputInfo = devices[0];
    MidiDevice.Info outputInfo = devices[1];

    try {
      MidiDevice inDevice = MidiSystem.getMidiDevice(inputInfo);
      inDevice.open();
      trans = inDevice.getTransmitter();
    } catch (MidiUnavailableException mdu1) {
      throw new RuntimeException("Unable to open device for input:" + mdu1);
    }

    try {
      MidiDevice outDevice = MidiSystem.getMidiDevice(outputInfo);
      outDevice.open();
      rec = outDevice.getReceiver();
    } catch (MidiUnavailableException mdu2) {
      throw new RuntimeException("Unable to open device for output:" + mdu2);
    }

    JavaMidiReceiver jmr = new JavaMidiReceiver(trans, rec);
    
    if (props.getProperty("sysexDelay") != null) {
      try {
        jmr.setSysexDelay(Integer.valueOf(props.getProperty("sysexDelayMillis")));
      } catch (NumberFormatException nfe) {
        throw new RuntimeException("Sysex delay value is not a number: " + nfe);
      }
    }
    
    trans.setReceiver(jmr);
    return jmr;
  }

  private static MidiDevice.Info[] fetchDeviceInformation(String inputPort,
      String outputPort) {
    if (inputPort == null && outputPort == null)
      inputPort = DEFAULT_PORT;
    if (outputPort == null)
      outputPort = inputPort;
    if (inputPort == null)
      inputPort = outputPort;

    MidiDevice.Info[] ports = { null, null };

    if (inputPort.equals(DEFAULT_PORT)) {
      // try the various default port names
      for (String portName : defaultPortNames) {
        ports = getMidiPorts(portName, portName);
        if (ports[0] != null)
          break;
      }
    } else {
      ports = getMidiPorts(inputPort, outputPort);
    }

    if (ports[0] == null) {
      if (inputPort.equals(DEFAULT_PORT)) {
        throw new RuntimeException("Unable to load default input port");
      } else {
        throw new RuntimeException("Unable to load input port named "
            + inputPort);
      }
    }
    if (ports[1] == null) {
      throw new RuntimeException("Unable to load output port named "
          + outputPort);
    }
    return ports;
  }

  private static MidiDevice.Info[] getMidiPorts(String inputPort,
      String outputPort) {
    MidiDevice.Info[] out = new MidiDevice.Info[2];
    for (MidiDevice.Info info : fetchAllMidiPorts()) {
      populateDeviceInfo(inputPort, outputPort, out, info);
    }
    return out;
  }

  private static List<MidiDevice.Info> fetchAllMidiPorts() {
    if (midiDeviceCache != null)
      return midiDeviceCache;

    List<MidiDevice.Info> midiports = new ArrayList<MidiDevice.Info>();
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

    if (infos.length == 0) {
      Qualm.LOG.warning("No MIDI devices found.");
      return midiports;
    }

    boolean useAlsaPorts = true;

    // Find ALSA MIDI ports
    for (MidiDevice.Info info : infos) {
      if (info.getName().startsWith("ALSA MIDI"))
        midiports.add(info);
    }

    // If we found no ALSA ports, then we'll use others
    if (midiports.isEmpty()) {
      useAlsaPorts = false;
      for (MidiDevice.Info info : infos)
        midiports.add(info);
    }

    if (midiports.isEmpty()) {
      Qualm.LOG.severe("No MIDI ports found.  Exiting.");
      System.exit(1);
    }

    midiDeviceCache = midiports;
    setupDeviceMap(useAlsaPorts);

    return midiDeviceCache;
  }

  private static void setupDeviceMap(boolean useAlsaPorts) {
    // assumes that cache is already built
    if (midiDeviceCache == null)
      throw new RuntimeException("MIDI device cache not yet built.");

    deviceDescriptionMap = new HashMap<MidiDevice.Info, String>();

    Map<Integer, String> alsaClientMap = 
      (useAlsaPorts ? parseAlsaClients() : null);

    List<String> portDescriptions = new ArrayList<String>();
    for (MidiDevice.Info info : midiDeviceCache) {
      String dev = info.getName();
      String cName = info.getDescription();

      if (useAlsaPorts) {
        // pull the ALSA device number out of the device description, and use
        // the Alsa client map to get the true description.
        dev = dev.substring(dev.indexOf('(') + 1);
        dev = dev.substring(0, dev.lastIndexOf(':'));
        cName = (String) alsaClientMap.get(new Integer(dev));
        deviceDescriptionMap.put(info, cName);
      }

      portDescriptions.add(info.getName() + " (" + cName + ") "
          + capacityDescription(info));
    }
    Qualm.LOG.fine("found ports: " + portDescriptions);
  }

  private static String capacityDescription(MidiDevice.Info info) {
    String devInfo;
    try {
      MidiDevice md = MidiSystem.getMidiDevice(info);
      devInfo = "[";
      if (md.getMaxTransmitters() != 0)
        devInfo += "trans:"
            + (md.getMaxTransmitters() >= 0 ? md.getMaxTransmitters()
                : "unlimited") + " ";
      if (md.getMaxReceivers() != 0)
        devInfo += "rec:"
            + (md.getMaxReceivers() >= 0 ? md.getMaxReceivers() : "unlimited");
      devInfo += "]";
      if (devInfo.equals("[ ]"))
        devInfo = "[unavailable]";
    } catch (MidiUnavailableException mue) {
      devInfo = "[unavailable]";
    }
    return devInfo;
  }

  private static void populateDeviceInfo(String inputPort, String outputPort,
      MidiDevice.Info[] out, MidiDevice.Info info) {
    String cName = info.getDescription();
    if (deviceDescriptionMap.get(info) != null)
      cName = deviceDescriptionMap.get(info);

    // Is this a port we want?
    MidiDevice md = null;
    try {
      md = MidiSystem.getMidiDevice(info);
    } catch (MidiUnavailableException mue) {
    }

    if (inputPort != null) {
      if ((cName.indexOf(inputPort) != -1 || info.getName().indexOf(inputPort) != -1)
          && md != null && md.getMaxTransmitters() != 0) {
        out[0] = info;
        Qualm.LOG.info("Using " + out[0].getName() + " (" + cName
            + ") for input.");
      }
    }

    if (outputPort != null) {
      if ((cName.indexOf(outputPort) != -1 || info.getName().indexOf(outputPort) != -1)
          && md != null && md.getMaxReceivers() != 0) {
        out[1] = info;
        Qualm.LOG.info("Using " + out[1].getName() + " (" + cName
            + ") for output.");
      }
    }
  }

  /**
   * Builds and returns a map that translates an ALSA device number to its
   * description
   */
  private static Map<Integer, String> parseAlsaClients() {
    Map<Integer, String> ret = new HashMap<Integer, String>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(ALSA_CLIENTS_FILE));
      String lin = br.readLine();
      while (lin != null) {
        if (lin.startsWith("Client") && !lin.startsWith("Client info")) {
          // get the client number and name
          Integer clientNum = new Integer(lin.substring(6, 10).trim());
          String clientName = lin.substring(14);
          clientName = clientName.substring(0, clientName.lastIndexOf('"'));
          ret.put(clientNum, clientName);
        }
        lin = br.readLine();
      }
      br.close();
    } catch (IOException ioe) {
      Qualm.LOG.info("Couldn't read " + ALSA_CLIENTS_FILE + ": " + ioe);
      return null;
    }
    return ret;
  }

  private static String[] defaultPortNames = { "UM-1", "USB Midi" };
}