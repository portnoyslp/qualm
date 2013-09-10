package qualm;

import java.util.Properties;

public interface AbstractQReceiverFactory {

  /**
   * Builds and returns an {@link AbstractQReceiver} using any information in the 
   * given {@link Properties}.
   */
  public AbstractQReceiver buildFromProperties(Properties properties);
  
}