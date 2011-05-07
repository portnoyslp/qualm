package qualm;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

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

  // TODO: Should we be doing namespaces?
  String nsu = "";  // NamespaceURI
  AttributesImpl atts = new AttributesImpl();
  String rootElement = "qualm-data";
  String indentStr = "\n                ";

  public static void outputXML(QData qd, OutputStream xmlOut) {
    try {
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = factory.newTransformer();
      //Setup input and output
      Source src = new SAXSource(new QDataXMLReader(), new QDataInputSource(qd));

      //Setup output
      StreamResult res = new StreamResult(xmlOut);

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

  private void nl(int indent) throws SAXException {
    handler.ignorableWhitespace(indentStr.toCharArray(), 0, indent+1);
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
      handler.startElement(nsu, rootElement, rootElement, atts);

      nl(2);
      handler.startElement(nsu, "title", "title", null);
      parse(qd.getTitle());
      handler.endElement(nsu, "title", "title");

      // the list of midi channels
      nl(2);
      handler.startElement(nsu, "midi-channels", "midi-channels", null);
      for (int i=0; i < qd.getMidiChannels().length; i++) {
        if (qd.getMidiChannels()[i] != null) {
          // output the channel
          atts.clear();
          atts.addAttribute(nsu, "num", "num", null, Integer.toString(i+1));
          String dev = PatchChanger.getRequestedDeviceForChannel(i);
          if (dev != null)
            atts.addAttribute(nsu, "device", "device", null, dev);
          nl(4);
          handler.startElement(nsu, "channel", "channel", atts);
          parse(qd.channels[i]);
          handler.endElement(nsu, "channel", "channel");
        }
      }
      nl(2);
      handler.endElement(nsu, "midi-channels", "midi-channels");

      // the list of patches
      nl(2);
      handler.startElement(nsu, "patches", "patches", null);
      Iterator<Patch> iter = qd.getPatches().iterator();
      while (iter.hasNext()) {
        parse(iter.next());			
      }
      nl(2);
      handler.endElement(nsu, "patches", "patches");

      // and the streams
      Iterator<QStream> sIter = qd.getCueStreams().iterator();
      while (sIter.hasNext()) {
        parse(sIter.next());			
      }

      nl(0);
      handler.endElement(nsu, rootElement, rootElement);
      handler.endDocument();
    }
  }

  public void parse(String str) throws SAXException {
    if (handler == null) {
      throw new SAXException("No handler defined.");
    }
    handler.characters(str.toCharArray(),0,str.length());
  }

  public void parse(Patch p) throws SAXException {
    if (handler == null) {
      throw new SAXException("No handler defined.");
    }
    atts.clear();
    atts.addAttribute(nsu, "id", "id", null, p.getID());
    if (p.getBank() != null) {
      atts.addAttribute(nsu, "bank", "bank", null, p.getBank());
    }
    atts.addAttribute(nsu, "num", "num", null, Integer.toString(p.getNumber()));
    if (p.getVolume() != null) {
      atts.addAttribute(nsu, "volume", "volume", null, p.getVolume().toString());
    }
    nl(4);
    handler.startElement(nsu, "patch", "patch", atts);
    parse(p.getDescription());
    handler.endElement(nsu, "patch", "patch");
  }

  public void parse(QStream qs) throws SAXException {
    if (handler == null) {
      throw new SAXException("No handler defined.");
    }

    atts.clear();
    atts.addAttribute(nsu, "id", "id", null, qs.getTitle());
    nl(2);
    handler.startElement(nsu, "cue-stream", "cue-stream", atts);
    Iterator<Cue> iter = qs.getCues().iterator();
    while (iter.hasNext()) {
      parse(iter.next());		
    }
    nl(2);
    handler.endElement(nsu, "cue-stream", "cue-stream");
  }

  public void parse(Cue cue) throws SAXException {
    atts.clear();
    atts.addAttribute(nsu, "song", "song", null, cue.getSong());
    atts.addAttribute(nsu, "measure", "measure", null, cue.getMeasure());
    nl(4);
    handler.startElement(nsu, "cue", "cue", atts);

    nl(6);
    handler.startElement(nsu, "events", "events", null);
    Iterator<QEvent> iter = cue.getEvents().iterator();
    while (iter.hasNext()) {
      QEvent obj = iter.next();
      if (obj instanceof ProgramChangeEvent) {
        parse((ProgramChangeEvent)obj);
      }
      if (obj instanceof NoteWindowChangeEvent) {
        parse((NoteWindowChangeEvent)obj);
      }
    }
    nl(6);
    handler.endElement(nsu, "events", "events");

    Iterator<EventMapper> emIter = cue.getEventMaps().iterator();
    while (emIter.hasNext()) {
      parse(emIter.next());
    }

    Iterator<Trigger> tIter = cue.getTriggers().iterator();
    while (tIter.hasNext()) {
      parse(tIter.next());
    }

    nl(4);
    handler.endElement(nsu, "cue", "cue");
  }

  public void parse(ProgramChangeEvent pce) throws SAXException {
    nl(8);
    atts.clear();
    atts.addAttribute(nsu, "channel", "channel", null, Integer.toString(pce.getChannel()+1));
    atts.addAttribute(nsu, "patch", "patch", null, pce.getPatch().getID());
    handler.startElement(nsu, "program-change", "program-change", atts);
    handler.endElement(nsu, "program-change", "program-change");
  }

  public void parse(NoteWindowChangeEvent nwce) throws SAXException {
    nl(8);
    atts.clear();
    atts.addAttribute(nsu, "channel", "channel", null, Integer.toString(nwce.getChannel()+1));
    if (nwce.getTopNote() != null)
      atts.addAttribute(nsu, "top", "top", null, nwce.getTopNote().toString());
    if (nwce.getBottomNote() != null)
      atts.addAttribute(nsu, "bottom", "bottom", null, nwce.getBottomNote().toString());
    handler.startElement(nsu, "note-window-change", "note-window-change", atts);
    handler.endElement(nsu, "note-window-change", "note-window-change");
  }

  public void parse(EventMapper em) throws SAXException {
    nl(6);
    handler.startElement(nsu, "map-events", "map-events", null);
    nl(8);
    handler.startElement(nsu, "map-from", "map-from", null);
    parse(em.getFromTemplate());
    handler.endElement(nsu, "map-from", "map-from");
    Iterator<EventTemplate> iter = em.getToTemplateList().iterator();
    while (iter.hasNext()) {
      nl(8);
      handler.startElement(nsu, "map-to", "map-to", null);
      parse(iter.next());
      handler.endElement(nsu, "map-to", "map-to");
    }
    nl(6);
    handler.endElement(nsu, "map-events", "map-events");
  }

  public void parse(Trigger t) throws SAXException {
    nl(6);
    atts.clear();
    if (t.getDelay() > 0)
      atts.addAttribute(nsu, "delay", "delay", null, Integer.toString(t.getDelay()));
    if (t.getReverse())
      atts.addAttribute(nsu, "reverse", "reverse", null, Boolean.toString(t.getReverse()));
    handler.startElement(nsu, "trigger", "trigger", atts);
    nl(8);
    parse(t.getTemplate());
    nl(6);
    handler.endElement(nsu, "trigger", "trigger");
  }
  
  public void parse(EventTemplate et) throws SAXException {
    String elementName;
    if (et.getTypeDesc().equals("NoteOn"))
      elementName = "note-on";
    else if (et.getTypeDesc().equals("NoteOff"))
      elementName = "note-off";
    else if (et.getTypeDesc().equals("Control"))
      elementName = "control";
    else
      throw new SAXException("Could not process template of type '" + et.getTypeDesc() + "'");
    
    atts.clear();
    atts.addAttribute(nsu, "channel", "channel", null, Integer.toString(et.channel()+1));
    if (elementName.equals("note-on") || elementName.equals("note-off")) {
      if (!et.range1().equals("-1--1")) 
        atts.addAttribute(nsu, "note", "note", null, et.range1());
    } else {
      atts.addAttribute(nsu, "control", "control", null, Integer.toString(et.getExtra1()));
      if (et.getExtra2() < 127)
        atts.addAttribute(nsu, "threshold", "threshold", null, Integer.toString(et.getExtra2()));
    }
    handler.startElement(nsu, elementName, elementName, atts);
    handler.endElement(nsu, elementName, elementName);
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
