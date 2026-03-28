package qualm;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *  Unit tests for class {@link JavaMidiReceiverFactory}.
 */

public class JavaMidiReceiverFactoryTest {
  Properties props;

  @BeforeEach
  public void setUp() throws Exception {
    props = new Properties();
  }

  // TODO: Add tests for checking that JMR is correctly built from real data.
  @Test
  public void unknownDevice() {
    props.setProperty("inputPort", "FOOBAR");
    assertThrows(RuntimeException.class, () ->
        new JavaMidiReceiverFactory().buildFromProperties(props));
  }
}
