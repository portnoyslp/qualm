package qualm;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

/**
 *  Unit tests for class {@link JavaMidiReceiverFactory}.
 */

public class JavaMidiReceiverFactoryTest {
  Properties props;

  @Before
  public void setUp() throws Exception {
    props = new Properties();
  }

  // TODO: expectedexception rule
  @Test(expected=RuntimeException.class)
  public void unknownDevice() throws Exception {
    props.setProperty("inputPort", "FOOBAR");
    JavaMidiReceiver jmr = JavaMidiReceiverFactory.buildFromProperties(props);    
  }

}