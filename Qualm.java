package qualm;

import javax.sound.midi.*;

public class Qualm {
  
  public static void main(String[] args) {
    MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
    if (infos.length == 0) {
      System.out.println( "No Midi devices found???" );
      System.exit(1);
    }
    
    for(int i = 0; i<infos.length; i++) {
      try { 
	MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
	System.out.println( "Device[" + i + "]: " +
			    infos[i].getName() );
      } catch (MidiUnavailableException mue) {
	mue.printStackTrace();
      }
    }

    System.exit(0);
  }

}
