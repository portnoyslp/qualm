package qualm;

import javax.sound.midi.MidiMessage;

/**
 * MapEvent.java
 *
 *
 * Created: Wed Jan 28 23:26:08 2004
 *
 */


public class MapEvent {
  int fromType = -1;
  int fromChannel = -1;
  int fromExtra1 = -1;
  int fromExtra2 = -1;

  int toType = -1;
  int toChannel = -1;
  int toExtra1 = -1;
  int toExtra2 = -1;
  
  public MapEvent() { } 

  public void setFromType(int t) { fromType = t; }
  public void setFromChannel(int t) { fromChannel = t; }
  public void setFromExtra1(int t) { fromExtra1 = t; }
  public void setFromExtra2(int t) { fromExtra2 = t; }

 public void setToType(int t) { toType = t; }
  public void setToChannel(int t) { toChannel = t; }
  public void setToExtra1(int t) { toExtra1 = t; }
  public void setToExtra2(int t) { toExtra2 = t; }

  public MidiMessage transformMessage( MidiMessage mm ) {
    MidiMessage ret = null;
    return ret;
  }
  
} // MapEvent
