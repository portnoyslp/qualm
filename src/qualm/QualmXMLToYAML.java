package qualm;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Command-line converter: reads a Qualm XML file, writes YAML to stdout.
 *
 * Usage: java qualm.QualmXMLToYAML <input.xml>
 */
public class QualmXMLToYAML {

  private static void usage() {
    System.err.println("Usage: java qualm.QualmXMLToYAML <input.xml>");
    System.err.println("  Reads a Qualm XML file and writes YAML to stdout.");
    System.exit(1);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) usage();

    QData qd;
    try {
      qd = new QDataLoader().load(args[0]);
    } catch (IOException e) {
      System.err.println("Error reading " + args[0] + ": " + e.getMessage());
      System.exit(1);
      return;
    }

    Writer writer = new OutputStreamWriter(System.out, "UTF-8");
    QDataYAMLWriter.outputYAML(qd, writer);
    writer.flush();
  }
}
