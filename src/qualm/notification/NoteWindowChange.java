package qualm.notification;

public interface NoteWindowChange extends QualmNotifier {
  void noteWindowChange(int channel, String channelName, Integer bottomNote, Integer topNote);
}
