package qualm;

import javax.sound.midi.*;
import javax.sound.midi.Receiver;
import javax.sound.midi.MidiMessage;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QReceiver implements Receiver {

  public QReceiver() { 
    triggers = new HashMap();
  }

  // Implementation of javax.sound.midi.Receiver

  public void close() { }

  public void send(MidiMessage midiMessage, long l) {
    // OK, we've received a message.  Check the triggers.
    for (int i=0;i<cachedTriggers.length;i++) {
      Trigger trig = cachedTriggers[i];
      if (trig.match(midiMessage)) {
	// call the method
	Invocation inv = (Invocation)triggers.get(trig);
	inv.call( trig, midiMessage );
	removeTrigger(trig);
      }
    }
    // no match, just ignore the message.
  }

  Trigger cachedTriggers[] = {};

  private void buildTriggerCache() {
    List l = new ArrayList();
    l.addAll(triggers.keySet());
    cachedTriggers = (Trigger[]) l.toArray(cachedTriggers);
  }

  public void addTrigger( Trigger t, Object o, Method m ) {
    triggers.put(t, new Invocation(o,m));
    buildTriggerCache();
  }

  public void removeTrigger( Trigger t ) {
    triggers.remove(t);
    buildTriggerCache();
  }

  class Invocation {
    Object o;
    Method m;
    public Invocation(Object o, Method m) {
      this.o = o; this.m = m;
    }
    public void call( Trigger t, MidiMessage msg ) {
      try {
	m.invoke(o, new Object[] { t, msg });
      } catch (IllegalAccessException iae) {
	System.out.println(iae);
	iae.printStackTrace();
      } catch (InvocationTargetException ite) {
	System.out.println(ite);
	ite.printStackTrace();
      }
    }
  }
      

  Map triggers;
}


