package qualm;

import java.util.*;
import javax.sound.midi.Receiver;
import javax.sound.midi.MidiMessage;

public class MultiplexReceiver implements Receiver {
  
  Collection receivers;
  boolean debugMIDI = false;

  public MultiplexReceiver() {
    receivers = new ArrayList();
  }

  public void setDebugMIDI(boolean flag) { debugMIDI=flag; }

  public void addReceiver( Receiver qc ) {
    receivers.add( qc );
  }

  public void send(MidiMessage msg, long ts) {
    if (debugMIDI) 
      System.out.println( MidiMessageParser.messageToString(msg) );

    Iterator i = receivers.iterator();
    while (i.hasNext()) 
      ((Receiver)i.next()). send(msg, ts);
  }

  public void close() {
    Iterator i = receivers.iterator();
    while (i.hasNext()) 
      ((Receiver)i.next()). close();
  }
}