package qualm;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import java.util.*;

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
      e.printStackTrace();
      System.out.println("Couldn't parse " + f + ": " + e);
      return null;
    }
  }
  
  /* DefaultHandler overrides */
  
  public void startDocument() {
    qdata = new QData();
  }
  
  String[] auxValue = new String[2];
  List eventSet = new ArrayList();
  Trigger trigger;
  String content;
  Cue curQ;
  
  public void startElement(String uri, String localName, 
			   String qName, Attributes attributes) {
    if (qName.equals("channel")) {
      // dealing with a midi-channel definition
      auxValue[0] = attributes.getValue("num");
      
    } else if (qName.equals("patch")) {
      // patch name
      auxValue[0] = attributes.getValue("num");
      auxValue[1] = attributes.getValue("channel");

    } else if (qName.equals("cue")) {
      eventSet = new ArrayList();
      curQ = new Cue( attributes.getValue("song"),
		      attributes.getValue("measure"));

    } else if (qName.equals("program-change")) {
      int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
      int p = Integer.parseInt(attributes.getValue("patch")) - 1;
      eventSet.add(new ProgramChangeEvent(ch,p));

      // TRIGGERS
    } else if (qName.equals("note-on")) {
      int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
      int n = Integer.parseInt(attributes.getValue("note")) - 1;
      trigger = Trigger.createNoteOnTrigger( ch, n );
    } else if (qName.equals("note-off")) {
      int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
      int n = Integer.parseInt(attributes.getValue("note")) - 1;
      trigger = Trigger.createNoteOffTrigger( ch, n );
    } else if (qName.equals("foot")) {
      int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
      trigger = Trigger.createFootTrigger( ch );
    } else if (qName.equals("note-off")) {
      int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
      int d = Integer.parseInt(attributes.getValue("duration")) - 1;
      trigger = Trigger.createClearTrigger( ch, d );
    }
  }
  
  public void endElement(String uri, String localName, String qName) {
    if (qName.equals("title")) {
      qdata.setTitle(content);

    } else if (qName.equals("channel")) {
      // dealing with a midi-channel definition
      qdata.addMidiChannel( Integer.parseInt(auxValue[0])-1, content );
      
    } else if (qName.equals("patch")) {
      // patch name; ignoring channel
      qdata.addPatch( Integer.parseInt(auxValue[0])-1, content );
      
    } else if (qName.equals("setup-events")) {
      qdata.setSetupEvents(eventSet);
      eventSet = new ArrayList();

    } else if (qName.equals( "cue" )) {
      if (trigger!=null) 
	curQ.setTrigger(trigger);
      curQ.setEvents(eventSet);
      eventSet = new ArrayList();
      qdata.addCue( curQ );

    } else if (qName.equals("default-trigger")) {
      qdata.setDefaultTrigger(trigger);
      trigger = null;
    } else if (qName.equals("reverse-trigger")) {
      qdata.setReverseTrigger(trigger);
      trigger = null;
    }

  }

  public void characters(char[] ch, int start, int length) {
    content = new String(ch,start,length);
  }
  
  

  public static void main(String[] args) {
    String file = "batboy-k2.xml";
    if (args.length > 0)
      file = args[0];

    QDataLoader qdl = new QDataLoader();
    QData data = qdl.readFile( new java.io.File( "batboy-k2.xml" ));
    data.dump();

  }

}
