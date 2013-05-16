package qualm.notification;

public interface QualmNotifier {
  /**
   * Informs the notifier that a series of notifications is being sent. When all
   * notifications have been delivered, {@link endNotifications} will then be
   * called.
   */
  public void beginNotifications();

  /**
   * Informs the notifier that the current series of notifications has been
   * delivered.
   */
  public void endNotifications();
}
