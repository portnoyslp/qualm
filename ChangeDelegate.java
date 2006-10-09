package qualm;

import javax.sound.midi.Receiver;

public abstract class ChangeDelegate {
  public abstract void patchChange( ProgramChangeEvent pce,
				    Receiver midiOut );
}