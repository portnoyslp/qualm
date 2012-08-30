package qualm;

import java.util.*;

public class QDataBuilder { 
  String title = null;
  Collection<Patch> patches = new ArrayList<Patch>();
  Collection<QStream> cueStreams = new ArrayList<QStream>();
  Map<Integer,String> channelTypes = new TreeMap<Integer,String>();
  Map<Integer,String> channelDesc = new TreeMap<Integer,String>();

  public QDataBuilder withTitle(String title) {
    this.title = title;
    return this;
  }

  public QDataBuilder addPatches(Collection<Patch> patches) {
    this.patches.addAll(patches);
    return this;
  }

  public QDataBuilder addPatch(Patch p) {
    this.patches.add(p);
    return this;
  }

  public QDataBuilder addStreams(Collection<QStream> streams) {
    this.cueStreams.addAll(streams);
    return this;
  }

  public QDataBuilder addStream(QStream qs) {
    this.cueStreams.add(qs);
    return this;
  }

  public QDataBuilder addMidiChannel( int ch, String devType, String desc) {
    this.channelTypes.put(new Integer(ch), devType);
    this.channelDesc.put(new Integer(ch), desc);
    return this;
  }

  public QData build() {
    QData qd = new QData();
    qd.setTitle(title);
    for(Integer i : channelTypes.keySet())
      qd.addMidiChannel( i.intValue(), channelTypes.get(i), channelDesc.get(i) );
    for(Patch p : patches) 
      qd.addPatch(p);
    for(QStream qs : cueStreams)
      qd.addCueStream(qs);
    qd.prepareCueStreams();
    return qd;
  }
}
