package qualm;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads in a qualm file and builds a QData structure that holds the
 * info.
 */

public class QDataLoader extends DefaultHandler {

  boolean validateInput = false;
  boolean ignorePatchAliases = false;
  
  SAXParser parser;
  QData qdata;
  
  public QDataLoader() { this( false ); }
  
  public QDataLoader(boolean validateInput) {
    this.validateInput = validateInput;
    try {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setValidating(validateInput);
      parser = spf.newSAXParser();
    } catch (ParserConfigurationException pce) {
      System.out.println("Could not configure parser: " + pce);
    } catch (org.xml.sax.SAXException se) {
      System.out.println("Could not build parser: " + se);
    }
  }

  public QData readFile( File f ) {
    try {
      parser.parse( f, this );
      qdata.prepareCueStreams();
      return qdata;
    } catch (Exception e) {
      System.out.println("Couldn't parse " + f + ": " + e);
      return null;
    }
  }
  public QData readSource( org.xml.sax.InputSource f ) {
    try {
      parser.parse( f, this );
      qdata.prepareCueStreams();
      return qdata;
    } catch (Exception e) {
      System.out.println("Couldn't parse " + f + ": " + e);
      return null;
    }
  }

  /**
   * If set to true, <patch-alias> tags in the XML data are ignored
   * and no patches are generated for them.  This is handy for
   * auditioning patches.
   */
  public void setIgnorePatchAliases(boolean val)
  {
    this.ignorePatchAliases = val;
  }
  
  /* DefaultHandler overrides */
  
  public void startDocument() {
    qdata = new QData();
  }
  
  String[] auxValue = new String[2];
  List<QEvent> eventSet = new ArrayList<QEvent>();
  List<Trigger> triggers = new ArrayList<Trigger>();
  List<Trigger> globalTriggers = new ArrayList<Trigger>();
  List<EventMapper> globalMaps = new ArrayList<EventMapper>();
  boolean reverseTrigger = false;
  int triggerDelay = 0;
  String content;
  Cue curQ;
  EventTemplate curTemplate;
  Patch patch;
  EventMapper curMapper = null;
  boolean buildingEvents = false;
  List<EventMapper> eventMaps = new ArrayList<EventMapper>();
  QStream qstream;

  private int parseIntOrPercent( String parseStr, int max) 
    throws NumberFormatException {
    if (parseStr.endsWith( "%" )) {
      parseStr = parseStr.substring(0,parseStr.length()-1);
      return (Integer.parseInt( parseStr ) * max) / 100;
    } else {
      return Integer.parseInt( parseStr );
    }
  }

  public void startElement(String uri, String localName, 
			   String qName, Attributes attributes) 
    throws NumberFormatException {

    content = "";
    String currentAttribute = null;
    String currentElement = "`" + qName + "' element";

    try {
      
      if (qName.equals("channel")) {
	// dealing with a midi-channel definition
	auxValue[0] = attributes.getValue("num");
	auxValue[1] = attributes.getValue("device-type");
	
      } else if (qName.equals("patch")) {
	currentElement = "patch `" + attributes.getValue("id") + "'";
	currentAttribute = "num";
	
	patch = new Patch( attributes.getValue("id"),
			   Integer.parseInt( attributes.getValue("num")));
	
	if (attributes.getValue("bank") != null) {
	  currentAttribute = "bank";
	  patch.setBank( attributes.getValue("bank") );
	}

	if (attributes.getValue("volume") != null) {
	  currentAttribute = "volume";
	  String volStr = attributes.getValue("volume");
	  int vol = parseIntOrPercent( volStr , 127 );
	  patch.setVolume( new Integer(vol) );
	}

      } else if (qName.equals("patch-alias") && !ignorePatchAliases ) {
	currentElement = "patch-alias `" + attributes.getValue("id") + "'";
	currentAttribute = "target";

	String targetID =  attributes.getValue("target");
	Patch targetPatch = qdata.lookupPatch( targetID );
	if (targetPatch == null)
	  System.err.println("WARNING: could not find patch with id " + 
			     targetID);
	else {
	  // duplicate bank and number of target patch (but id and
	  // description will be different)
	  patch = new Patch( attributes.getValue("id"),
	      targetPatch.getNumber() );

	  patch.setBank( targetPatch.getBank() );

	  // duplicate volume of target patch unless the patch-alias
	  // sets its own independent volume attribute
	  if (attributes.getValue("volume") != null) {
	    currentAttribute = "volume";
	    String volStr = attributes.getValue("volume");
	    int vol = parseIntOrPercent( volStr, 127 );
	    patch.setVolume( new Integer(vol) );
	  }
	  else
	    patch.setVolume( targetPatch.getVolume() );
	}
      } else if (qName.equals("cue-stream")) {
	qstream = new QStream();
	qstream.setTitle(attributes.getValue("id"));
     
      } else if (qName.equals("cue")) {
	eventSet = new ArrayList<QEvent>();
	curQ = new Cue( attributes.getValue("song"),
			attributes.getValue("measure"));
	
      } else if (qName.equals("program-change")) {
	currentElement = "program-change element";
	if (curQ != null) 
	  currentElement += " [cue " + curQ.getCueNumber() + "]";
	currentAttribute = "channel";
	int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	String patchID = attributes.getValue("patch");
	Patch foundPatch = qdata.lookupPatch(patchID);
	if (foundPatch == null) {
	  System.err.println("WARNING: could not find patch with id " + patchID);
	}
	eventSet.add(new ProgramChangeEvent(ch, curQ, foundPatch));

      } else if (qName.equals("note-window-change")) {
	currentElement = "note-window-change element";
	if (curQ != null) 
	  currentElement += " [cue " + curQ.getCueNumber() + "]";
	currentAttribute = "channel";
	int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	Integer topNote = null, bottomNote = null;
	currentAttribute = "bottom";
	String bottom = attributes.getValue("bottom");
	if (bottom != null)
	  bottomNote = new Integer(Utilities.noteNameToMidi(bottom));
	currentAttribute = "top";
	String top = attributes.getValue("top");
	if (top != null)
	  topNote = new Integer(Utilities.noteNameToMidi(top));
	if (bottom == null && top == null) {
	  System.err.println("WARNING: found note-window-change specifying neither top nor bottom");
	}
	eventSet.add(new NoteWindowChangeEvent(ch, curQ, bottomNote, topNote));

      } else if (qName.equals("advance")) {
	StreamAdvance sa = new StreamAdvance( attributes.getValue("stream"), curQ );
	if (attributes.getValue("song")!=null)
	  sa.setSong(attributes.getValue("song"));
	if (attributes.getValue("measure")!=null)
	  sa.setMeasure(attributes.getValue("measure"));

	eventSet.add(sa);
	
      } else if (qName.equals("trigger")) {
	currentAttribute="reverse";
	String rev = attributes.getValue("reverse");
	reverseTrigger = false;
	if (rev != null && !rev.equals("false"))
	  reverseTrigger = true;

        currentAttribute="delay";
        triggerDelay = 0;
        String delayStr = attributes.getValue("delay");
        if (delayStr != null && !delayStr.equals("")) {
          // multiply by 1000 to get ms.
          triggerDelay = (int) (1000*Float.parseFloat(delayStr));
        }
        
      } else if (qName.equals("events")) {
        buildingEvents = true;

	// TRIGGERS
      } else if (qName.equals("note-on")) {
	currentAttribute = "channel";
	int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	currentAttribute = "note";
	curTemplate = EventTemplate.noteOn( ch, attributes.getValue("note") );
	if (buildingEvents) {
	  eventSet.add(new MidiEvent(curTemplate));
	}
	
      } else if (qName.equals("note-off")) {
	currentAttribute = "channel";
	int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	currentAttribute = "note";
	curTemplate = EventTemplate.noteOff( ch, attributes.getValue("note") );
        if (buildingEvents) {
          eventSet.add(new MidiEvent(curTemplate));
        }
        
      } else if (qName.equals("control-change")) {
	currentAttribute = "channel";
	int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	String control = attributes.getValue("control");
	String value = attributes.getValue("value");
	curTemplate = EventTemplate.control( ch, control, value );
        if (buildingEvents) {
          eventSet.add(new MidiEvent(curTemplate));
        }
        
      } else if (qName.equals("sysex")) {
        // no attributes, but we have hex-coded data, so this gets handled at endElement

	/* } else if (qName.equals("clear")) {
	   currentAttribute = "channel";
	   int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	   currentAttribute = "duration";
	   int d = Integer.parseInt(attributes.getValue("duration")) - 1;
	   curTemplate = EventTemplate.createClearEventTemplate( ch, d ) ); */


      } else if (qName.equals("map-events")) {
	curMapper = new EventMapper();
      }

   
    } catch (NumberFormatException nfe) {
      throw new NumberFormatException( "Could not parse `" + currentAttribute 
				       + "' attribute for " + currentElement);
    } catch (NullPointerException npe) {
      System.out.println("Handling " + currentElement + "/" + currentAttribute);
      npe.printStackTrace();
      throw npe;
    }
  }
  
  public void endElement(String uri, String localName, String qName) {
    if (qName.equals("title")) {
      qdata.setTitle(content);

    } else if (qName.equals("channel")) {
      // dealing with a midi-channel definition
      qdata.addMidiChannel( Integer.parseInt(auxValue[0])-1, auxValue[1], content );
      
    } else if (qName.equals("patch") ||
	       (qName.equals("patch-alias") && !ignorePatchAliases)) {
      // patch name; ignoring channel
      if (content != null && !"".equals(content))
	patch.setDescription(content);
      else
	patch.setDescription(patch.getID());

      qdata.addPatch( patch );
      
    } else if (qName.equals("trigger")) {
      Trigger t = new Trigger(curTemplate,reverseTrigger);
      if (triggerDelay > 0) { t.setDelay(triggerDelay); }
      triggers.add(t);

    } else if (qName.equals("map-from")) {
      curMapper.setFromTemplate(curTemplate);
    } else if (qName.equals("map-to")) {
      curMapper.addToTemplate(curTemplate);
    } else if (qName.equals("map-events")) {
      eventMaps.add(curMapper);
      curMapper = null;

    } else if (qName.equals("events")) {
      buildingEvents = false;
      
    } else if (qName.equals("sysex")) {
      // interpret contents as hex-encoded string, and build an event for it
      // first, get rid of all whitespace
      String hexString = content.replaceAll("\\s+","");
      int len = hexString.length();
      byte[] data = new byte[len/2];
      for (int i=0; i<len; i+=2) {
        data[i/2] = (byte) ((Character.digit(hexString.charAt(i),16) << 4)
                           + Character.digit(hexString.charAt(i+1),16));
      }
      MidiCommand mc = new MidiCommand();
      mc.setSysex(data);
      if (buildingEvents) {
        eventSet.add(new MidiEvent(mc));
      }
      
    } else if (qName.equals( "cue" )) {
      ArrayList<Trigger> l = new ArrayList<Trigger>();
      l.addAll(globalTriggers);
      l.addAll(triggers);
      curQ.setTriggers ( l );

      ArrayList<EventMapper> em = new ArrayList<EventMapper>();
      em.addAll(globalMaps);
      em.addAll(eventMaps);
      curQ.setEventMaps(em);

      curQ.setEvents(eventSet);

      triggers = new ArrayList<Trigger>();
      eventMaps = new ArrayList<EventMapper>();
      eventSet = new ArrayList<QEvent>();

      qstream.addCue( curQ );

    } else if (qName.equals("global")) {
      globalTriggers = triggers;
      globalMaps = eventMaps;
      triggers = new ArrayList<Trigger>();
      eventMaps = new ArrayList<EventMapper>();
      
    } else if (qName.equals("cue-stream")) {
      qdata.addCueStream(qstream);
      qstream = null;
    }

  }

  public void characters(char[] ch, int start, int length) {
    content += new String(ch,start,length);
  }
  
  public InputSource resolveEntity(String publicId, String systemId) 
    throws org.xml.sax.SAXException {
    try { 
      if (validateInput)
	return super.resolveEntity(publicId, systemId);
      else
	// return a null input source so that we don't check things we
	// don't care about.
	return new InputSource(new StringReader(""));
    } catch (java.io.IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  // handle validation errors if appropriate
  public void error(org.xml.sax.SAXParseException err) {
    if (validateInput) {
      Qualm.LOG.severe("Validation Error: " 
			 + ", line " + err.getLineNumber()
			 + ", uri " + err.getSystemId());
      Qualm.LOG.severe("   " + err.getMessage());
    }
  }
  public void warning(org.xml.sax.SAXParseException err) {
    if (validateInput) {
      Qualm.LOG.warning("Validation warning: " 
			 + ", line " + err.getLineNumber()
			 + ", uri " + err.getSystemId());
      Qualm.LOG.warning("   " + err.getMessage());
    }
  }
}

