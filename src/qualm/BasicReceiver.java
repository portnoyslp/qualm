package qualm;

public class BasicReceiver implements QReceiver {

  QReceiver target;
  
  @Override
  public QReceiver getForwarder() {
    return target;
  }

  @Override
  public void handleMidiCommand(MidiCommand midi) {
    target.handleMidiCommand(midi);
  }

  @Override
  public void setForwarder(QReceiver qr) {
    this.target = qr;
  }

}
