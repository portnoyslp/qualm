package qualm;

import org.xml.sax.InputSource;

public class QDataInputSource extends InputSource {
	// Creates an InputSource for QData elements.  Used to create XML documents from QData.
	
	QData qdata;
	
	public QDataInputSource(QData qd) {
		setQData(qd);
	}
	public void setQData(QData qd) {
		qdata = qd;
	}
	public QData getQData() {
		return qdata;
	}
}
