package qualm;

public class StreamAdvance {
  String streamID;
  String song;
  String measure;
  
  public StreamAdvance (String stream) {
    setStreamID(stream);
  }

  public void setStreamID( String sid ) { streamID = sid; }
  public String getStreamID() { return streamID; }

  public void setSong( String song ) { this.song = song; }
  public String getSong() { return song; }

  public void setMeasure( String measure ) { this.measure = measure; }
  public String getMeasure() { return measure; }

  public String getCueNumber() { return song + "." + measure; }

} 