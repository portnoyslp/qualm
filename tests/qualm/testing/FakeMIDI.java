package qualm.testing;

import java.util.ArrayList;
import java.util.Iterator;

import qualm.MasterController;
import qualm.MidiCommand;
import qualm.QController;
import qualm.QData;
import qualm.QDataLoader;
import qualm.QReceiver;
import qualm.QStream;

import junit.framework.Assert;

/*
 * A "fake" midi transmitter/receiver which can be given a set of
 * commands to send, and will store the responses for later analysis.
 */

public class FakeMIDI implements QReceiver {

  QReceiver receiver;
  ArrayList<Object> incomingMessages;
  ArrayList<Object> outgoingMessages;
  long baseTime;
  
  public FakeMIDI( ) {
    // set the base time for the run.
    baseTime = System.currentTimeMillis();
    
    incomingMessages = new ArrayList<Object>();
    outgoingMessages = new ArrayList<Object>();
  }

  public void setReceiver (QReceiver rec) {
    this.receiver = rec;
  }
  
  public void addOutgoing(long ts, MidiCommand cmd) {
    outgoingMessages.add( new TimestampedMsg(ts,cmd) );
  }

  public void addOutgoing(long ts, int a, int ch, int c) {
    MidiCommand sm = new MidiCommand();
    sm.setParams(ch, a, (byte)c); 
    addOutgoing(ts,sm);
  }
  public void addOutgoing(long ts, int a, int ch, int c, int d) {
    MidiCommand sm = new MidiCommand();
    sm.setParams(ch,a,(byte)c,(byte)d); 
    addOutgoing(ts,sm);
  }

  public ArrayList<Object> receivedMessages() {
    return incomingMessages;
  }
  
  public void printOutMessages() {
    System.out.println("Number of msgs received == " + incomingMessages.size());
    java.util.Iterator<Object> iter = incomingMessages.iterator();
    while (iter.hasNext()) {
      TimestampedMsg tsm = (TimestampedMsg)iter.next();
      System.out.println("   " + tsm.timestamp + "ms: " + tsm.msg );
    }
  }
    

  public void run() {
    long firstStamp = ((TimestampedMsg)outgoingMessages.get(0)).timestamp ;
    long runStartTime = System.currentTimeMillis() - firstStamp;
    
    // loop through outgoing messages, delaying when necessary.
    for(int i=0; i<outgoingMessages.size(); i++) {
      TimestampedMsg tm = (TimestampedMsg)outgoingMessages.get(i);
      long targetTime = runStartTime + tm.timestamp;
      // wait for target time
      while (System.currentTimeMillis() < targetTime ) { 
	try { Thread.sleep(50); } catch (Exception e) { } 
      }
      // send message
      receiver.handleMidiCommand(tm.msg);
    }
  }

  public static FakeMIDI prepareTest(String xmlString) {
    FakeMIDI fm = new FakeMIDI();

    QData qd = (new QDataLoader()).
      readSource(new org.xml.sax.InputSource(new java.io.StringReader(xmlString)));
    MasterController mc = new MasterController ( fm );
    mc.setSilentErrorHandling(false);

    Iterator<QStream> iter = qd.getCueStreams().iterator();
    while (iter.hasNext()) 
      mc.addController (new QController( fm, iter.next(), qd ));
    fm.setReceiver( mc );
    mc.setDebugMIDI(true);

    mc.gotoCue("0.0");
   
    return fm;
  }

  // assertion test
  public static void assertMIDI(Object msg, int command, int channel, int data1, int data2) {
    MidiCommand m = (MidiCommand) (((TimestampedMsg)msg).msg);
    Assert.assertEquals("Command for " + m + 
			" not expected value " + command,
			m.getType(), command);
    Assert.assertEquals("Channel for " + m + 
			" not expected value " + channel,
			m.getChannel(), channel);
    Assert.assertEquals("Data1 for " + m + 
			" not expected value " + data1,
			m.getData1(), data1);
    Assert.assertEquals("Data2 for " + m + 
			" not expected value " + data2,
			m.getData2(), data2);
  }

  // assertion: ensure that a timestamp is past a certain minimum value
  public static void assertTS(Object msg, long minValue) {
    long ts = ((TimestampedMsg)msg).timestamp;
    MidiCommand m = ((TimestampedMsg)msg).msg;
    Assert.assertTrue( "Timestamp for " + m +
                       " not after " + minValue,
                       ts > minValue );
  }
  

  // Implementation of QReceiver

  public void handleMidiCommand(MidiCommand midiMessage) {
    // Store midi messages with a timestamp based on the unit's creation.
    long curTime = System.currentTimeMillis() - baseTime;
    incomingMessages.add(new TimestampedMsg(curTime, midiMessage));
  }

  // private structures for binding timestamps and messages
  private class TimestampedMsg { 
    public long timestamp;
    public MidiCommand msg;
    TimestampedMsg( long ts, MidiCommand mm ) {
      timestamp = ts;
      msg = mm;
    }
  }

} // QController
