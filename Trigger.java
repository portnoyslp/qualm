package qualm;

public class Trigger {

  public Trigger () { } 

  public static Trigger createNoteOnTrigger( int ch, int note ) { 
    Trigger t = new Trigger();
    t.type = NOTE_ON;
    t.channel = ch;
    t.extra = note;
    return t;
  }
  public static Trigger createNoteOffTrigger( int ch, int note ) {  
    Trigger t = new Trigger();
    t.type = NOTE_OFF;
    t.channel = ch;
    t.extra = note;
    return t;
  }
  public static Trigger createFootTrigger( int ch ) {  
    Trigger t = new Trigger();
    t.type = FOOT;
    t.channel = ch;
    return t;
  }
  public static Trigger createClearTrigger( int ch, int duration ) {  
    Trigger t = new Trigger();
    t.type = CLEAR;
    t.channel = ch;
    t.extra = duration;
    return t;
  }

  public void setDelay(int d) { delay=d; }
  public int getDelay() { return delay; }

  public static int NOTE_ON = 1;
  public static int NOTE_OFF = 2;
  public static int FOOT = 3;
  public static int CLEAR = 4;

  protected int delay;
  protected int type;
  protected int channel;
  protected int extra;
  
}
