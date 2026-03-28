package qualm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class QualmXMLToYAMLTest {

  @Test
  public void xmlRoundTripsViaYAML() throws Exception {
    QData fromXml = new QDataLoader().load("tests/qualm/qdl-1.xml");

    StringWriter sw = new StringWriter();
    QDataYAMLWriter.outputYAML(fromXml, sw);

    byte[] yamlBytes = sw.toString().getBytes(StandardCharsets.UTF_8);
    QData roundTripped = new YAMLDataLoader()
        .readStream(new ByteArrayInputStream(yamlBytes));

    assertEquals(fromXml, roundTripped);
  }
}
