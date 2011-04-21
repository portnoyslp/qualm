package qualm;

// a simple interface for cued events (events that have a cue attached
// to them); handy for writing Comparators.

public interface CuedEvent extends QEvent {
  public int getChannel();
  public Cue getCue();
}
