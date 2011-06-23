package qualm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Adaptor for translating between DSMidi messages and Qualm messaging.
 * Basically a straightforward port of the libdsmi_iphone.m file from
 * http://code.google.com/dsmi/
 *
 * @author speters
 */

public class DSMidiReceiver extends AbstractQReceiver implements QReceiver {

  QReceiver qr;
  
  String hostName;
  
  int DEST_PORT = 9000;
  int LOCAL_PORT = 9001;
  int LOCAL_SEND_PORT = 9002;
  
  DatagramSocket sockIn, sockOut;
  
  public DSMidiReceiver(Properties props) {
    try { 
        
      Qualm.LOG.info("Listening on port " + LOCAL_PORT);
      sockIn = new DatagramSocket(LOCAL_PORT);
    
      sockOut = new DatagramSocket(LOCAL_SEND_PORT);
      sockOut.setBroadcast(true);
    
      Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
      InetAddress broadAddr = null;
      while (niEnum.hasMoreElements() && broadAddr==null) {
        NetworkInterface ni = niEnum.nextElement();
        if (ni.isLoopback())
          continue; // no broadcasting to the local interface
        
        for (InterfaceAddress iAddr : ni.getInterfaceAddresses()) {
          broadAddr = iAddr.getBroadcast();
          if (broadAddr != null)
            break;
        }
      }
      
      if (broadAddr == null) {
        throw new RuntimeException("Could not find any network interfaces.");
      }
      
      Qualm.LOG.info("Broadcasting to " + broadAddr + ":" + DEST_PORT);
      sockOut.connect(broadAddr,DEST_PORT);
      
    } catch (SocketException ne) {
      throw new RuntimeException("Could not set up sockets.");
    }
    
    // and now, we start a Thread running that will listen on the input socket and 
    // spew out MidiCommands.
    
    Thread readerThread = new Thread() {
      public void run() {
        while (true) {
          try { 
            byte[] buf = new byte[3];
            DatagramPacket packet = new DatagramPacket(buf,buf.length);
            sockIn.receive(packet);
        
            // not sure if this works, or if I need to reload the data
            int messageType = -1;
            switch((byte)(buf[0] & 0xf0)) {
              case NOTE_ON: messageType = MidiCommand.NOTE_ON;        break;
              case NOTE_OFF: messageType = MidiCommand.NOTE_OFF;       break;
              case CONTROL_CHANGE: messageType = MidiCommand.CONTROL_CHANGE; break;
              case PROGRAM_CHANGE: messageType = MidiCommand.PROGRAM_CHANGE; break;
            }
            byte channel = (byte) (buf[0] & 0x0f);
            byte data1 = buf[1];
            byte data2 = buf[2];
        
            if (messageType != -1) {
              MidiCommand mc = new MidiCommand(channel,messageType,data1,data2);
              getTarget().handleMidiCommand(mc);
            }
          
          } catch (IOException ioe) {
            // ignore any errors
          }
      
        }
      }
    };
    
    readerThread.start();
    
  }
  
  /* Receives the given MidiCommand from Qualm and sends it out through the MIDI interface.
   * @see qualm.BasicReceiver#handleMidiCommand(qualm.MidiCommand)
   */
  public void handleMidiCommand(MidiCommand mc) {
    try {

      if (mc.getType() == MidiCommand.SYSEX) {
        // sorry, we don't support SYSEX, so we silently drop it.
        return;
      }
          
      byte messageType = 0;
      switch (mc.getType()) {
      case MidiCommand.NOTE_ON:        messageType=NOTE_ON;        break;
      case MidiCommand.NOTE_OFF:       messageType=NOTE_OFF;       break;
      case MidiCommand.CONTROL_CHANGE: messageType=CONTROL_CHANGE; break;
      case MidiCommand.PROGRAM_CHANGE: messageType=PROGRAM_CHANGE; break;
      }
      
      byte channel = (byte)(mc.getChannel());

      byte[] sendbuf = new byte[3];          
      sendbuf[0] = (byte) (messageType | channel);
      sendbuf[1] = (byte) mc.getData1();
      sendbuf[2] = (byte) mc.getData2();
      sockOut.send(new DatagramPacket(sendbuf,3,sockOut.getRemoteSocketAddress()));
      
    } catch (IOException ioe) {
      // ignore MIDI problems.
    }
  }
  
  static final byte NOTE_ON = (byte)0x80;
  static final byte NOTE_OFF = (byte)0x90;
  static final byte CONTROL_CHANGE = (byte)0xB0;
  static final byte PITCH_CHANGE = (byte)0xE0;
  static final byte PROGRAM_CHANGE = (byte)0xC0;


}
