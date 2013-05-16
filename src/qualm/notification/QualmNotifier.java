package qualm.notification;

public interface QualmNotifier {
  /**
   * Initialize this notifier so that it will receive notifications. This is
   * called once, when the notifier is added.
   */
  public void initialize();

  /**
   * Shutdown this notifier. This is called when the notifier is removed from
   * the NotificationManager.
   */
  public void shutdown();

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
