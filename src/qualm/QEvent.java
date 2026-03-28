package qualm;

//A simple interface for Qualm events.  All Qualm events have a cue attached to them.
//We also store a channel, which is used by most QEvents.

public abstract sealed class QEvent implements Comparable<QEvent>
    permits ChannelEvent, StreamAdvance {

  public Cue getCue() { return cue; }
  public void setCue(Cue cue) {this.cue = cue; }

  public int compareTo(QEvent qe) {
    return this.getCue().compareTo(qe.getCue());
  }

  protected Cue cue;
}
