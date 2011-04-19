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

	@Override
	public ContentHandler getContentHandler() {
		return handler;
	}

	@Override
	public void setContentHandler(ContentHandler handler) {
		this.handler = handler;
	}

	private void nl(int indent) throws SAXException {
		handler.ignorableWhitespace(indentStr.toCharArray(), 0, indent+1);
	}

	@Override
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
					// TODO: add device-type
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
			Iterator iter = qd.getPatches().iterator();
			while (iter.hasNext()) {
				parse((Patch)(iter.next()));			
			}
			nl(2);
			handler.endElement(nsu, "patches", "patches");

			// and the streams
			iter = qd.getCueStreams().iterator();
			while (iter.hasNext()) {
				parse((QStream)(iter.next()));			
			}

			nl(0);
			handler.endElement(nsu, rootElement, rootElement);
			handler.endDocument();
		}
	}

	public void parse(String str) throws IOException, SAXException {
		if (handler == null) {
			throw new SAXException("No handler defined.");
		}
		handler.characters(str.toCharArray(),0,str.length());
	}

	public void parse(Patch p) throws IOException, SAXException {
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

	public void parse(QStream qs) throws IOException, SAXException {
		if (handler == null) {
			throw new SAXException("No handler defined.");
		}

		atts.clear();
		atts.addAttribute(nsu, "id", "id", null, qs.getTitle());
		nl(2);
		handler.startElement(nsu, "cue-stream", "cue-stream", atts);
		Iterator iter = qs.getCues().iterator();
		while (iter.hasNext()) {
			parse((Cue)(iter.next()));		
		}
		nl(2);
		handler.endElement(nsu, "cue-stream", "cue-stream");
	}

	public void parse(Cue cue) throws IOException, SAXException {
		atts.clear();
		atts.addAttribute(nsu, "song", "song", null, cue.getSong());
		atts.addAttribute(nsu, "measure", "measure", null, cue.getMeasure());
		nl(4);
		handler.startElement(nsu, "cue", "cue", atts);
		nl(6);
		handler.startElement(nsu, "events", "events", null);
		Iterator iter = cue.getEvents().iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			if (obj instanceof ProgramChangeEvent) {
				parse((ProgramChangeEvent)obj);
			}
		}
		nl(6);
		handler.endElement(nsu, "events", "events");
		nl(4);
		handler.endElement(nsu, "cue", "cue");
	}

	public void parse(ProgramChangeEvent pce) throws IOException, SAXException {
		nl(8);
		atts.clear();
		atts.addAttribute(nsu, "channel", "channel", null, Integer.toString(pce.getChannel()+1));
		atts.addAttribute(nsu, "patch", "patch", null, pce.getPatch().getID());
		handler.startElement(nsu, "program-change", "program-change", atts);
		handler.endElement(nsu, "program-change", "program-change");
	}

	@Override
	public DTDHandler getDTDHandler() {
		return null;
	}

	@Override
	public EntityResolver getEntityResolver() {
		return null;
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return null;
	}

	@Override
	public boolean getFeature(String arg0) throws SAXNotRecognizedException,
	SAXNotSupportedException {
		return false;
	}

	@Override
	public Object getProperty(String arg0) throws SAXNotRecognizedException,
		SAXNotSupportedException {
		return null;
	}

	@Override
	public void setDTDHandler(DTDHandler arg0) {
	}

	@Override
	public void setEntityResolver(EntityResolver arg0) {
	}

	@Override
	public void setErrorHandler(ErrorHandler arg0) {
	}

	@Override
	public void setFeature(String arg0, boolean arg1)
	throws SAXNotRecognizedException, SAXNotSupportedException {
	}

	@Override
	public void setProperty(String arg0, Object arg1)
	throws SAXNotRecognizedException, SAXNotSupportedException {
	}

}
