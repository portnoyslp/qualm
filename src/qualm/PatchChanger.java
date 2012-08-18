package qualm;

import java.util.regex.Pattern;

public class PatchChanger {
  // instance methods
  private PatchChanger() { }
  private void setChangeDelegate(ChangeDelegate cd) {
    changeDelegate = cd;
  }
  private ChangeDelegate getChangeDelegate() { return changeDelegate; }
  private void setRequestedDevice(String deviceName) {
    this.requestedDevice = deviceName;
  }
  private String getRequestedDevice() { return requestedDevice; }
  
  private static String delegatePrefix = "qualm.delegates.";

  public static void installDelegateForChannel( int ch,
						ChangeDelegate cd,
						String deviceType ) {
    if (changer[ch] == null)
      changer[ch] = new PatchChanger();

    if (deviceType != null)
      changer[ch].setRequestedDevice(deviceType);

    changer[ch].setChangeDelegate(cd);
  }

  public static void addPatchChanger( int ch, String deviceType ) {
    installDelegateForChannel( ch, lookupDelegateFromType(deviceType), deviceType );
  }

  private static ChangeDelegate lookupDelegateFromType( String deviceType ) {
    // for now, we default to "Standard" as the device type
    
    if (deviceType == null) 
      deviceType = "Standard";

    // find the ChangeDelegate based on the deviceType name.
    Class<?> delegate = null;
    String delegateName = deviceType;
    Pattern pattern = Pattern.compile( "\\W" );
    delegateName = pattern.matcher(delegateName).replaceAll("");

    try {
      delegate = Class.forName( delegatePrefix + delegateName + "Delegate" );
      if (!ChangeDelegate.class.isAssignableFrom(delegate))
	delegate = null;
    } catch (ClassNotFoundException cnfe) { }

    // if not found yet, just try the first word in the device type.
    if (delegate == null) {
      delegateName = deviceType.substring(0,deviceType.indexOf(' '));
      delegateName = pattern.matcher(delegateName).replaceAll("");
      try {
	delegate = Class.forName( delegatePrefix + delegateName + "Delegate" );
	if (!ChangeDelegate.class.isAssignableFrom(delegate)) 
	  delegate = null;
      } catch (ClassNotFoundException cnfe) { }
    }

    if (delegate == null)
      throw new RuntimeException("Could not locate patch changer for device type '" 
				 + deviceType + "'");

    try {
      return (ChangeDelegate) delegate.newInstance();
    } catch (Exception ie) {
      throw new RuntimeException("Could not create patch changer " + delegate.getName());
    }
  }

  public static synchronized void patchChange( ProgramChangeEvent pce,
					       QReceiver midiOut ) {
    int ch = pce.getChannel();
    if (changer[ch] != null) {
      changer[ch].getChangeDelegate().patchChange(pce, midiOut);
    } else {
      throw new RuntimeException("Could not execute program change " + pce
				 + " on unknown channel " + ch);
    }
  }

  public static synchronized void noteWindowChange( NoteWindowChangeEvent nwce,
						    QReceiver midiOut ) {
    int ch = nwce.getChannel();
    if (changer[ch] != null) {
      changer[ch].getChangeDelegate().noteWindowChange(nwce, midiOut);
    } else {
      throw new RuntimeException("Could not execute note-window change " + nwce
				 + " on unknown channel " + ch);
    }
  }

  public static String getRequestedDeviceForChannel(int ch) {
    return changer[ch].getRequestedDevice();
  }
  
  private ChangeDelegate changeDelegate;
  private String requestedDevice;

  private static PatchChanger[] changer = new PatchChanger[16];

} 

