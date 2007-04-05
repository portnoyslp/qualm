package qualm;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import java.util.*;

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
  List eventSet = new ArrayList();
  List triggers = new ArrayList();
  List globalTriggers = new ArrayList();
  List globalMaps = new ArrayList();
  boolean reverseTrigger = false;
  String content;
  Cue curQ;
  EventTemplate curTemplate;
  Patch patch;
  EventMapper curMapper = null;
  List eventMaps = new ArrayList();
  QStream qstream;
  
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
	  int vol = -1;
	  if (volStr.endsWith( "%" )) {
	    volStr = volStr.substring(0,volStr.length()-1);
	    vol = (Integer.parseInt( volStr ) * 127) / 100;
	  } else {
	    vol = Integer.parseInt( volStr );
	  }
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
	  int vol = Integer.parseInt( volStr );
	  if (volStr.endsWith( "%" )) 
	    vol = (vol * 127) / 100;
	  patch.setVolume( new Integer(vol) );
	}
	else
	  patch.setVolume( targetPatch.getVolume() );

      } else if (qName.equals("cue-stream")) {
	qstream = new QStream();
	qstream.setTitle(attributes.getValue("id"));
     
      } else if (qName.equals("cue")) {
	eventSet = new ArrayList();
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
	eventSet.add(new ProgramChangeEvent(ch, foundPatch));

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
	  bottomNote = new Integer(_noteNameToMidi(bottom));
	currentAttribute = "top";
	String top = attributes.getValue("top");
	if (top != null)
	  topNote = new Integer(_noteNameToMidi(top));
	if (bottom == null && top == null) {
	  System.err.println("WARNING: found note-window-change specifying neither top nor bottom");
	}
	eventSet.add(new NoteWindowChangeEvent(ch, bottomNote, topNote));

      } else if (qName.equals("advance")) {
	StreamAdvance sa = new StreamAdvance( attributes.getValue("stream"));
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

	// TRIGGERS
      } else if (qName.equals("note-on")) {
	currentAttribute = "channel";
	int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	currentAttribute = "note";
	int n = _noteNameToMidi(attributes.getValue("note"));
	curTemplate = EventTemplate.createNoteOnEventTemplate( ch, n );
      } else if (qName.equals("note-off")) {
	currentAttribute = "channel";
	int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	currentAttribute = "note";
	int n = _noteNameToMidi(attributes.getValue("note"));
	curTemplate = EventTemplate.createNoteOffEventTemplate( ch, n );
      } else if (qName.equals("control-change")) {
	currentAttribute = "channel";
	int ch = Integer.parseInt(attributes.getValue("channel")) - 1;
	String control = attributes.getValue("control");
	String thresh = attributes.getValue("threshold");
	curTemplate = EventTemplate.createControlEventTemplate( ch, control, thresh );
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
      triggers.add(t);

    } else if (qName.equals("map-from")) {
      curMapper.setFromTemplate(curTemplate);
    } else if (qName.equals("map-to")) {
      curMapper.setToTemplate(curTemplate);
    } else if (qName.equals("map-events")) {
      eventMaps.add(curMapper);

    } else if (qName.equals( "cue" )) {
      ArrayList l = new ArrayList();
      l.addAll(globalTriggers);
      l.addAll(triggers);
      curQ.setTriggers ( l );

      ArrayList em = new ArrayList();
      em.addAll(globalMaps);
      em.addAll(eventMaps);
      curQ.setEventMaps(em);

      curQ.setEvents(eventSet);

      triggers = new ArrayList();
      eventMaps = new ArrayList();
      eventSet = new ArrayList();

      qstream.addCue( curQ );

    } else if (qName.equals("global")) {
      globalTriggers = triggers;
      globalMaps = eventMaps;
      triggers = new ArrayList();
      eventMaps = new ArrayList();
      
    } else if (qName.equals("cue-stream")) {
      qdata.addCueStream(qstream);
      qstream = null;
    }

  }

  public void characters(char[] ch, int start, int length) {
    content += new String(ch,start,length);
  }
  
  private static List numList = 
    Arrays.asList( new String[] { 
      "c","c#","d","d#","e","f","f#","g","g#","a","a#","b"
    });

  private static int _noteNameToMidi ( String noteName ) {
    try {
      return Integer.parseInt( noteName );
    } catch (NumberFormatException nfe) {
    }

    // convert to lowercase to make our lives easier.
    noteName = noteName.toLowerCase();
    
    // get the key
    String key = noteName.substring(0,1);
    noteName = noteName.substring(1);
    
    // get the sharp/flat (and convert to just sharps)
    if (noteName.startsWith("#")) {
      key = key + "#";
      noteName = noteName.substring(1);
    } else if (noteName.startsWith("b")) {
      // Use # instead of flat
      char k = key.charAt(0);
      if (k == 'a') {
	key = "g#";
      } else if (k == 'c') {
	key = "b";
      } else if (k == 'f') {
	key = "e";
      } else {
	k--;
	key = new String( new char[] { k } );
	key = key + "#";
      }
      noteName = noteName.substring(1);
    }
    // everything else is the octave number.
    int octave = Integer.parseInt(noteName)+1;

    if (key.equals("e#")) key="f";

    if (key.equals("b#")) {
      key="c";
      octave++;
    }
    
    return (octave*12 + numList.indexOf( key ));
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
      System.out.println("*** ERROR: " 
			 + ", line " + err.getLineNumber()
			 + ", uri " + err.getSystemId());
      System.out.println("   " + err.getMessage());
    }
  }
  public void warning(org.xml.sax.SAXParseException err) {
    if (validateInput) {
      System.out.println("*** WARNING: " 
			 + ", line " + err.getLineNumber()
			 + ", uri " + err.getSystemId());
      System.out.println("   " + err.getMessage());
    }
  }

  public static void main(String[] args) {
    for(int i=0; i<args.length; i++)
      System.out.println(args[i] + "=>" + _noteNameToMidi(args[i]));
  }

}

