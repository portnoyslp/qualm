package qualm;

import javax.sound.midi.Receiver;

public class PatchChanger {
  // instance methods
  private PatchChanger() { }
  private void setChangeDelegate(ChangeDelegate cd) {
    changeDelegate = cd;
  }
  private ChangeDelegate getChangeDelegate() { return changeDelegate; }



  public static void addPatchChanger( int ch,
				      String deviceType ) {
    if (changer[ch] == null)
      changer[ch] = new PatchChanger();

    // for now, we only have the one delegate type
    // XXX this should be dispatched on the deviceType
    changer[ch].setChangeDelegate(new RolandDelegate() );
  }    

  // and now the only important method
  public static synchronized void patchChange( ProgramChangeEvent pce,
					       Receiver midiOut ) {
    int ch = pce.getChannel();
    if (changer[ch] != null) {
      changer[ch].getChangeDelegate().patchChange(pce, midiOut);
    } else {
      throw new RuntimeException("Could not execute program change " + pce
				 + " on unknown channel " + ch);
    }
  }


  private ChangeDelegate changeDelegate;

  private static PatchChanger[] changer = new PatchChanger[16];

} 

