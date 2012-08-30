package qualm;

import java.util.*;

public class QStreamBuilder { 
  String title = null;
  Collection<Cue> cues = new ArrayList<Cue>();

  public QStreamBuilder withTitle(String title) {
    this.title = title;
    return this;
  }

  public QStreamBuilder addCues(Collection<Cue> cues) {
    this.cues.addAll(cues);
    return this;
  }

  public QStreamBuilder addCue(Cue cue) {
    this.cues.add(cue);
    return this;
  }

  public QStream build() {
    QStream qs = new QStream();
    qs.setTitle(title);
    for(Cue c : cues)
      qs.addCue( c );
    return qs;
  }
}
