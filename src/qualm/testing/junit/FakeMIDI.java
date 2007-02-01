package qualm.testing.junit;

import qualm.*;
import java.util.*;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import junit.framework.Assert;

/*
 * A "fake" midi transmitter/receiver which can be given a set of
 * commands to send, and will store the responses for later analysis.
 */

public class FakeMIDI implements Receiver,Transmitter {

  Receiver receiver;
  ArrayList incomingMessages;
  ArrayList outgoingMessages;
  
  public FakeMIDI( ) {
    incomingMessages = new ArrayList();
    outgoingMessages = new ArrayList();
  }

  public void addOutgoing(long ts, ShortMessage sm) {
    outgoingMessages.add( new TimestampedMsg(ts,sm) );
  }

  public void addOutgoing(long ts, int a) {
    ShortMessage sm = new ShortMessage();
    try { sm.setMessage(a); } 
    catch (javax.sound.midi.InvalidMidiDataException mde) {
      throw new RuntimeException(mde);
    }
    addOutgoing(ts,sm);
  }
  public void addOutgoing(long ts, int a, int b, int c) {
    ShortMessage sm = new ShortMessage();
    try { sm.setMessage(a, b, c); } 
    catch (javax.sound.midi.InvalidMidiDataException mde) {
      throw new RuntimeException(mde);
    }
    addOutgoing(ts,sm);
  }
  public void addOutgoing(long ts, int a, int b, int c, int d) {
    ShortMessage sm = new ShortMessage();
    try { sm.setMessage(a,b,c,d); } 
    catch (javax.sound.midi.InvalidMidiDataException mde) {
      throw new RuntimeException(mde);
    }
    addOutgoing(ts,sm);
  }

  public ArrayList receivedMessages() {
    return incomingMessages;
  }

  public void run() {
    long baseTime = ((TimestampedMsg)outgoingMessages.get(0)).timestamp ;
    baseTime = System.currentTimeMillis() - baseTime;
    
    // loop through outgoing messages, delaying when necessary.
    for(int i=0; i<outgoingMessages.size(); i++) {
      TimestampedMsg tm = (TimestampedMsg)outgoingMessages.get(i);
      long targetTime = baseTime + tm.timestamp;
      // wait for target time
      while (System.currentTimeMillis() < targetTime ) { 
	try { Thread.sleep(50); } catch (Exception e) { } 
      }
      // send message
      receiver.send(tm.msg,-1);
    }
  }

  public static FakeMIDI prepareTest(String xmlString) {
    FakeMIDI fm = new FakeMIDI();

    QData qd = (new QDataLoader()).
      readSource(new org.xml.sax.InputSource(new java.io.StringReader(xmlString)));
    MasterController mc = new MasterController ( fm );

    Iterator iter = qd.getCueStreams().iterator();
    while (iter.hasNext()) 
      mc.addController (new QController( fm, (QStream)iter.next(), qd ));
    fm.setReceiver( mc );
    mc.setDebugMIDI(true);
    mc.gotoCue("0.0");
   
    return fm;
  }

  // assertion test
  public static void assertMIDI(Object msg, int command, int channel, int data1, int data2) {
    ShortMessage m = (ShortMessage)msg;
    Assert.assertEquals("Command for " + MidiMessageParser.messageToString(m) + 
			" not expected value " + command,
			m.getCommand(), command);
    Assert.assertEquals("Channel for " + MidiMessageParser.messageToString(m) + 
			" not expected value " + channel,
			m.getChannel(), channel);
    Assert.assertEquals("Data1 for " + MidiMessageParser.messageToString(m) + 
			" not expected value " + data1,
			m.getData1(), data1);
    Assert.assertEquals("Data2 for " + MidiMessageParser.messageToString(m) + 
			" not expected value " + data2,
			m.getData2(), data2);
  }
  

  // Implementation of javax.sound.midi.Receiver

  public void close() {
    receiver = null;
  }

  public void send(MidiMessage midiMessage, long ts) {
    // store midi messages.
    incomingMessages.add(midiMessage);
  }

  // Implementation of javax.sound.midi.Transmitter

  public Receiver getReceiver() { return receiver; }
  public void setReceiver(Receiver receiver) { this.receiver = receiver; }

  // private structures for binding timestamps and messages
  private class TimestampedMsg { 
    public long timestamp;
    public ShortMessage msg;
    TimestampedMsg( long ts, ShortMessage sm ) {
      timestamp = ts;
      msg = sm;
    }
  }

} // QController
