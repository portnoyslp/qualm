package qualm;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/**
 * Reads in a qualm file and builds a QData structure that holds the
 * info.
 */

public class QDataLoader extends DefaultHandler {
  
  SAXParser parser;
  QData qdata; 

  public QDataLoader() {
    try {
      parser = SAXParserFactory.newInstance().newSAXParser();
    } catch (ParserConfigurationException pce) {
      System.out.println("Could not configure parser: " + pce);
    } catch (org.xml.sax.SAXException se) {
      System.out.println("Could not build parser: " + se);
    }
  }

  public QData readFile( File f ) {
    try {
      parser.parse( f, this );
      return qdata;
    } catch (Exception e) {
      System.out.println("Couldn't parse " + f + ": " + e);
      return null;
    }
  }

  /* DefaultHandler overrides */

  public void startDocument() {
    qdata = new QData();
  }

  String[] auxValue = new String[2];
  String content;
  
  public void startElement(String uri, String localName, 
			   String qName, Attributes attributes) {
    if (qName.equals("channel")) {
      // dealing with a midi-channel definition
      auxValue[0] = attributes.getValue("num");

    } else if (qName.equals("patch")) {
      // patch name
      auxValue[0] = attributes.getValue("num");
      auxValue[1] = attributes.getValue("channel");
    }			       
  }
  
  public void endElement(String uri, String localName, String qName) {
    if (qName.equals("title") {
      qdata.setTitle(content);
    
    } else if (qName.equals("channel")) {
      // dealing with a midi-channel definition
      qdata.addMidiChannel( Integer.parseInt(auxValue[0])-1, content );

    } else if (qName.equals("patch")) {
      // patch name; ignoring channel
      qdata.addPatch( Integer.parseInt(auxValue[0])-1, content );

    }
  }
  public void characters(char[] ch, int start, int length) {
    content = new String(ch,start,length);
  }

}
