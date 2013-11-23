package qualm;

import java.util.Properties;

public class NullQReceiverFactory implements AbstractQReceiverFactory {

  @Override
  public AbstractQReceiver buildFromProperties(Properties properties) {
    return new NullQReceiver();
  }

  // create a simple AbstractQReceiver that doesn't do anything special.
  private static class NullQReceiver extends AbstractQReceiver {
    @Override
    public void setTarget(QReceiver t) {
    }

    @Override
    public void handleMidiCommand(MidiCommand midi) {
    }
  }
}
