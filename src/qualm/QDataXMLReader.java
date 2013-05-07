package qualm;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

public class QDataXMLReader implements XMLReader {

  ContentHandler handler;

  static String doctype = "http://portnoyslp.github.com/qualm/qualm.dtd";
  String nsu = "";  // NamespaceURI
  AttributesImpl atts = new AttributesImpl();
  String rootElement = "qualm-data";
  String indentStr = "\n                ";

  public static void outputXML(QData qd, Writer xmlWriter) {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype);
      transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
      
      //Setup input and output
      Source src = new SAXSource(new QDataXMLReader(), new QDataInputSource(qd));

      //Setup output
      StreamResult res = new StreamResult(xmlWriter);

      //Start XSLT transformation
      transformer.transform(src, res);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ContentHandler getContentHandler() {
    return handler;
  }

  public void setContentHandler(ContentHandler handler) {
    this.handler = handler;
  }

  // helper functions
  private void nl(int indent) throws SAXException {
    handler.ignorableWhitespace(indentStr.toCharArray(), 0, indent+1);
  }
  private void startElement(String elementName, AttributesImpl at) throws SAXException {
    handler.startElement(nsu, elementName, elementName, at);
  }
  private void endElement(String elementName) throws SAXException {
    handler.endElement(nsu, elementName, elementName);
  }
  private void addAttribute(String attName, String value) throws SAXException {
    atts.addAttribute(nsu, attName, attName, null, value);
  }

  public void parse(InputSource source) throws IOException, SAXException {
    if (source instanceof QDataInputSource) {
      // start handling data
      if (handler == null) {
        throw new SAXException("No handler defined.");
      }
      QData qd = ((QDataInputSource)source).getQData();

      handler.startDocument();
      nl(0);
      startElement(rootElement, atts);

      nl(2);
      startElement("title", null);
      parse(qd.getTitle());
      endElement("title");

      // the list of midi channels
      nl(2);
      startElement("midi-channels", null);
      for (int i=0; i < qd.getMidiChannels().length; i++) {
        if (qd.getMidiChannels()[i] != null) {
          // output the channel
          atts.clear();
          addAttribute("num", Integer.toString(i+1));
          String dev = PatchChanger.getRequestedDeviceForChannel(i);
          if (dev != null)
            addAttribute("device", dev);
          nl(4);
          startElement("channel", atts);
          parse(qd.channels[i]);
          endElement("channel");
        }
      }
      nl(2);
      endElement("midi-channels");

      // the list of patches
      nl(2);
      startElement("patches", null);
      Iterator<Patch> iter = qd.getPatches().iterator();
      while (iter.hasNext()) {
        parse(iter.next());			
      }
      nl(2);
      endElement("patches");

      // and the streams
      Iterator<QStream> sIter = qd.getCueStreams().iterator();
      while (sIter.hasNext()) {
        parse(sIter.next());			
      }

      nl(0);
      endElement(rootElement);
      handler.endDocument();
    }
  }

  
  public void parse(String str) throws SAXException {
    if (handler == null) {
      throw new SAXException("No handler defined.");
    }
    handler.characters(str.toCharArray(),0,str.length());
  }

  public void parseAsHexData(byte[] data) throws SAXException {
    StringBuilder hex = new StringBuilder();
    for (byte b : data)
      hex.append(String.format("%1$02X", b));
    String hStr = hex.toString();
    handler.characters(hStr.toCharArray(),0,hStr.length());
  }

  public void parse(Patch p) throws SAXException {
    if (handler == null) {
      throw new SAXException("No handler defined.");
    }
    atts.clear();
    addAttribute("id", p.getID());
    if (p.getBank() != null) {
      addAttribute("bank", p.getBank());
    }
    addAttribute("num", Integer.toString(p.getNumber()));
    if (p.getVolume() != null) {
      addAttribute("volume", p.getVolume().toString());
    }
    nl(4);
    startElement("patch", atts);
    parse(p.getDescription());
    endElement("patch");
  }

  public void parse(QStream qs) throws SAXException {
    if (handler == null) {
      throw new SAXException("No handler defined.");
    }

    atts.clear();
    addAttribute("id", qs.getTitle());
    nl(2);
    startElement("cue-stream", atts);
    Iterator<Cue> iter = qs.getCues().iterator();
    while (iter.hasNext()) {
      parse(iter.next());		
    }
    nl(2);
    endElement("cue-stream");
  }

  public void parse(Cue cue) throws SAXException {
    atts.clear();
    addAttribute("song", cue.getSong());
    addAttribute("measure", cue.getMeasure());
    nl(4);
    startElement("cue", atts);

    nl(6);
    startElement("events", null);
    Iterator<QEvent> iter = cue.getEvents().iterator();
    while (iter.hasNext()) {
      QEvent obj = iter.next();
      if (obj instanceof ProgramChangeEvent)
        parse((ProgramChangeEvent)obj);
      if (obj instanceof NoteWindowChangeEvent)
        parse((NoteWindowChangeEvent)obj);
      if (obj instanceof StreamAdvance)
        parse((StreamAdvance)obj);
      if (obj instanceof MidiEvent)
        parse((MidiEvent)obj);
    }
    nl(6);
    endElement("events");

    Iterator<EventMapper> emIter = cue.getEventMaps().iterator();
    while (emIter.hasNext()) {
      parse(emIter.next());
    }

    Iterator<Trigger> tIter = cue.getTriggers().iterator();
    while (tIter.hasNext()) {
      parse(tIter.next());
    }

    nl(4);
    endElement("cue");
  }

  public void parse(ProgramChangeEvent pce) throws SAXException {
    nl(8);
    atts.clear();
    addAttribute("channel", Integer.toString(pce.getChannel()+1));
    addAttribute("patch", pce.getPatch().getID());
    startElement("program-change", atts);
    endElement("program-change");
  }

  public void parse(NoteWindowChangeEvent nwce) throws SAXException {
    nl(8);
    atts.clear();
    addAttribute("channel", Integer.toString(nwce.getChannel()+1));
    if (nwce.getTopNote() != null)
      addAttribute("top", nwce.getTopNote().toString());
    if (nwce.getBottomNote() != null)
      addAttribute("bottom", nwce.getBottomNote().toString());
    startElement("note-window-change", atts);
    endElement("note-window-change");
  }

  public void parse(StreamAdvance sa) throws SAXException {
    nl(8);
    atts.clear();
    addAttribute("stream", sa.getStreamID());
    if (sa.getSong() != null)
      addAttribute("song", sa.getSong());
    if (sa.getMeasure() != null) 
      addAttribute("measure", sa.getMeasure());
    startElement("advance", atts);
    endElement("advance");
  }

  public void parse(MidiEvent me) throws SAXException {
    MidiCommand cmd = me.getMidiCommand();
    String elName = null;
    atts.clear();
    nl(8);
    if (cmd.getType() == MidiCommand.NOTE_ON) {
      elName = "note-on";
      addAttribute("channel", Integer.toString(cmd.getChannel()+1));
      addAttribute("note", Integer.toString(cmd.getData1()));
      addAttribute("value", Integer.toString(cmd.getData2()));
    }
    if (cmd.getType() == MidiCommand.NOTE_OFF) {
      elName = "note-off";
      addAttribute("channel", Integer.toString(cmd.getChannel()+1));
      addAttribute("note", Integer.toString(cmd.getData1()));
    }
    if (cmd.getType() == MidiCommand.CONTROL_CHANGE) {
      elName = "control-change";
      addAttribute("channel", Integer.toString(cmd.getChannel()+1));
      addAttribute("control", Integer.toString(cmd.getData1()));
      addAttribute("value", Integer.toString(cmd.getData2()));
    }
    if (cmd.getType() == MidiCommand.SYSEX) {
      startElement("sysex", null);
      parse(cmd.hexData());
      endElement("sysex");
    }
    if (elName != null) {
      startElement(elName,atts);
      endElement(elName);
    }
  }
  
  
  public void parse(EventMapper em) throws SAXException {
    nl(6);
    startElement("map-events", null);
    nl(8);
    startElement("map-from", null);
    parse(em.getFromTemplate());
    endElement("map-from");
    Iterator<EventTemplate> iter = em.getToTemplateList().iterator();
    while (iter.hasNext()) {
      nl(8);
      startElement("map-to", null);
      parse(iter.next());
      endElement("map-to");
    }
    nl(6);
    endElement("map-events");
  }

  public void parse(Trigger t) throws SAXException {
    nl(6);
    atts.clear();
    if (t.getDelay() > 0)
      addAttribute("delay", Integer.toString(t.getDelay()));
    if (t.getReverse())
      addAttribute("reverse", Boolean.toString(t.getReverse()));
    startElement("trigger", atts);
    nl(8);
    parse(t.getTemplate());
    nl(6);
    endElement("trigger");
  }
  
  public void parse(EventTemplate et) throws SAXException {
    String elementName;
    switch (et.getType()) {
    case MidiCommand.NOTE_ON: 
      elementName = "note-on"; break;
    case MidiCommand.NOTE_OFF:
      elementName = "note-off"; break;
    case MidiCommand.CONTROL_CHANGE:
      elementName = "control-change"; break;
    case MidiCommand.SYSEX:
      elementName = "sysex"; break;
    default:
      throw new SAXException("Could not process template of type '" + et.getTypeDesc() + "'");
    }
    
    atts.clear();
    addAttribute("channel", Integer.toString(et.channel()+1));
    if (elementName.equals("note-on") || elementName.equals("note-off")) {
      if (!et.range1().equals("-1--1")) 
        addAttribute("note", et.range1());
      } else {
        addAttribute("control", Integer.toString(et.getExtra1()));
        if (et.getExtra2() < 127)
          addAttribute("threshold", Integer.toString(et.getExtra2()));
      }
    startElement(elementName, atts);
    endElement(elementName);
  }

  public DTDHandler getDTDHandler() {
    return null;
  }

  public EntityResolver getEntityResolver() {
    return null;
  }

  public ErrorHandler getErrorHandler() {
    return null;
  }

  public boolean getFeature(String arg0) throws SAXNotRecognizedException,
  SAXNotSupportedException {
    return false;
  }

  public Object getProperty(String arg0) throws SAXNotRecognizedException,
  SAXNotSupportedException {
    return null;
  }

  public void setDTDHandler(DTDHandler arg0) {
  }

  public void setEntityResolver(EntityResolver arg0) {
  }

  public void setErrorHandler(ErrorHandler arg0) {
  }

  public void setFeature(String arg0, boolean arg1)
  throws SAXNotRecognizedException, SAXNotSupportedException {
  }

  public void setProperty(String arg0, Object arg1)
  throws SAXNotRecognizedException, SAXNotSupportedException {
  }

}
