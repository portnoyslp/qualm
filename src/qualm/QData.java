package qualm;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * Holds all data for the cue handling...
 */

public class QData {
  private String[] channels;
  String[] channelDeviceTypes;
  Map<String, Patch> patches;
  Collection<QStream> cueStreams;
  String title;

  public QData( ) {
    title = null;
    channels = new String[16];
    channelDeviceTypes = new String[16];
    patches = new HashMap<>();
    cueStreams = new ArrayList<>();
  }

  public String getTitle() { return title; }
  public void setTitle(String t) { title=t; }

  public void addMidiChannel( int num, String deviceType, String desc ) {
    channels[num] = desc;
    channelDeviceTypes[num] = deviceType;
  }
  public String[] getMidiChannels() { return channels; }
  public String[] getMidiChannelDeviceTypes() { return channelDeviceTypes; }
  public Collection<Patch> getPatches() { return patches.values(); }

  public void addPatch( Patch p ) {
    patches.put( p.getID(), p );
  }
  public Patch lookupPatch( String id ) { 
    return patches.get(id); 
  }

  public void addCueStream(QStream qs) {
    cueStreams.add(qs);
  }
  public Collection<QStream> getCueStreams() { return cueStreams; }

  public void dump(Writer outWriter) {
    PrintWriter output = new PrintWriter(outWriter);
    output.println("Data dump for " + getTitle());
    // Create lists of patches, channels.
    List<String> out = new ArrayList<String>();
    for (int i=0; i<channels.length;i++) 
      if (channels[i]!=null) out.add("(" + i + ")" + channels[i]);
    output.println("  ch:" + out);
    output.println("  pl:" + patches.values());

    for (QStream qs : cueStreams) 
      qs.dump(output);
  }
  

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;

    QData qd = (QData)obj;
    return (title == null ? qd.getTitle() == null : title.equals(qd.getTitle()))
      && Arrays.equals(qd.getMidiChannels(), this.getMidiChannels())
      && Arrays.equals(qd.getMidiChannelDeviceTypes(), this.getMidiChannelDeviceTypes())
      // convert getPatches() to HashSet so equals() works.
      && (new HashSet<Patch>(getPatches())).equals(new HashSet<Patch>(qd.getPatches()))
      && (cueStreams == null ? qd.getCueStreams() == null : cueStreams.equals(qd.getCueStreams()))
      ;
  }
  @Override
  public int hashCode() {
    final int prime = 73;
    int result = 1;
    result =  prime * result + (title==null ? 0 : title.hashCode());
    result += prime * result + (Arrays.hashCode(channels));
    result += prime * result + (Arrays.hashCode(channelDeviceTypes));
    result += prime * result + (getPatches() == null ? 0 : getPatches().hashCode());
    result += prime * result + (cueStreams == null ? 0 : cueStreams.hashCode());
    
    return result;
  }
} 
