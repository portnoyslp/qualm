package qualm;

import java.util.regex.Pattern;

public class PatchChanger {

  private ChangeDelegate[] delegates = new ChangeDelegate[16];
  private String[] requestedDevices = new String[16];

  private static String delegatePrefix = "qualm.delegates.";

  public void installDelegateForChannel( int ch, ChangeDelegate cd, String deviceType ) {
    if (deviceType != null)
      requestedDevices[ch] = deviceType;
    delegates[ch] = cd;
  }

  public void addPatchChanger( int ch, String deviceType ) {
    installDelegateForChannel( ch, lookupDelegateFromType(deviceType), deviceType );
  }

  private static ChangeDelegate lookupDelegateFromType( String deviceType ) {
    if (deviceType == null)
      deviceType = "Standard";

    Class<?> delegate = null;
    String delegateName = deviceType;
    Pattern pattern = Pattern.compile( "\\W" );
    delegateName = pattern.matcher(delegateName).replaceAll("");

    try {
      delegate = Class.forName( delegatePrefix + delegateName + "Delegate" );
      if (!ChangeDelegate.class.isAssignableFrom(delegate))
	delegate = null;
    } catch (ClassNotFoundException cnfe) { }

    if (delegate == null) {
      delegateName = deviceType.substring(0, deviceType.indexOf(' '));
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

  public synchronized void patchChange( ProgramChangeEvent pce, QReceiver midiOut ) {
    int ch = pce.getChannel();
    if (delegates[ch] != null) {
      delegates[ch].patchChange(pce, midiOut);
    } else {
      throw new RuntimeException("Could not execute program change " + pce
				 + " on unknown channel " + ch);
    }
  }

  public synchronized void noteWindowChange( NoteWindowChangeEvent nwce, QReceiver midiOut ) {
    int ch = nwce.getChannel();
    if (delegates[ch] != null) {
      delegates[ch].noteWindowChange(nwce, midiOut);
    } else {
      throw new RuntimeException("Could not execute note-window change " + nwce
				 + " on unknown channel " + ch);
    }
  }

  public String getRequestedDeviceForChannel( int ch ) {
    return requestedDevices[ch];
  }

  // visible for testing
  ChangeDelegate delegateForChannel( int ch ) {
    return delegates[ch];
  }

  /** Constructs a PatchChanger configured from the channel info stored in qdata. */
  public static PatchChanger fromQData( QData qdata ) {
    PatchChanger pc = new PatchChanger();
    String[] channels = qdata.getMidiChannels();
    String[] deviceTypes = qdata.getMidiChannelDeviceTypes();
    for (int i = 0; i < channels.length; i++) {
      if (channels[i] != null)
	pc.addPatchChanger(i, deviceTypes[i]); // null deviceType defaults to Standard
    }
    return pc;
  }

}
