package qualm;

/**
 * Holder for simple MIDI commands that will be sent on cue trigger.
 */

public class MidiEvent extends QEvent {
  MidiCommand cmd;
  
  public MidiEvent(MidiCommand cmd) {
    this.cmd = cmd;
  }
  public MidiEvent(EventTemplate et) {
    // use ET to build the right command.
    MidiCommand cmd = null;
    switch (et.getType()) {
    case MidiCommand.NOTE_ON:
      cmd = new MidiCommand(et.channel(),et.getType(),(byte)et.getExtra1(),(byte)100);
      break;
    case MidiCommand.NOTE_OFF:
      cmd = new MidiCommand(et.channel(),et.getType(),(byte)et.getExtra1(),(byte)0);
      break;
    case MidiCommand.CONTROL_CHANGE:
      cmd = new MidiCommand(et.channel(),et.getType(),(byte)et.getExtra1(),(byte)et.getExtra2());
      break;
    }
    if (cmd == null)
      throw new RuntimeException("Could not recognize template type " + et.getTypeDesc());
    this.cmd = cmd;
  }
  
  public MidiCommand getMidiCommand() { return cmd; }

}
