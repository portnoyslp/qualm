package qualm;

import javax.sound.midi.Receiver;

public abstract class ChangeDelegate {
  /**
   * Tell the MIDI receiver to change patches, using the information
   * in the supplied ProgramChangeEvent.
   * 
   * Subclasses of ChangeDelegate must implement this message in order
   * for the code to function.
   *
   * @param pce the program change
   * @param midiOut the destination receiver
   */
  public abstract void patchChange( ProgramChangeEvent pce,
				    Receiver midiOut );
  
  /**
   * Tell the MIDI receiver to change the note window for a part,
   * using the information in the supplied NoteWindowChangeEvent.
   * 
   * Subclasses of ChangeDelegate may implement this at their
   * discretion in order to support Qualm control files containing
   * <note-window-change> events.  The default implementation prints a
   * message to stdout stating that note window changes are not
   * supported by this device.
   *
   * @param nwce the note window change
   * @param midiOut the destination receiver
   */
  public void noteWindowChange( NoteWindowChangeEvent nwce,
				Receiver midiOut ) {
    System.out.println( "WARNING: this device does not support <note-window-change> events." );
  }
  
  /**
   * Determines whether a given ProgramChangeEvent is valid for this
   * device.  Can be used to determine if the bank description and
   * patch number have valid values.
   *
   * Subclasses of ChangeDelegate may implment this at their
   * discretion; the default implementation is to mark all events as
   * valid.
   *
   * @param pce the event under consideration.
   * @return a <code>boolean</code> value indicating whether or not
   * <code>pce</code> is a valid event.
   */
  public boolean isValid( ProgramChangeEvent pce ) { return true; }
}
