package qualm.notification;

/*
 * The protocol information for sending and receiving queue and patch
 * changes.  This defines the serialization/deserialization of the
 * information.
 *
 * The Cue information is formatted as
 * "Q<cueNum>:<pendingCueNum>:<controller>".  Patch information is
 * formatted as "P<channelNum>:<channelName>:<description>".
 * Note window information is formatted as
 * "N<channelNum>:<channelName>:<bottomNote>:<topNote>" where either
 * note bound may be empty if unset.
 */

public class NetworkNotificationProtocol {

  public static String sendCue (String currentCueNumber, 
                                String pendingCueNumber,
                                String controllerName) {
    return "Q" + currentCueNumber + ":" +
      pendingCueNumber + ":" +
      controllerName;
  }
  public static String sendPatch (int channel, String channelName, 
                                  String patchDesc) {
    return "P" + channel + ":" +
      channelName + ":" + 
      patchDesc;
  }
  public static String sendEventMap (int fromChannel, int toChannel,
                                     String mapperDesc) {
    return "M" + fromChannel + ":" +
      toChannel + ":" + mapperDesc;
  }
  public static String sendNoteWindow(int channel, String channelName,
                                      Integer bottomNote, Integer topNote) {
    return "N" + channel + ":" +
      channelName + ":" +
      (bottomNote != null ? bottomNote : "") + ":" +
      (topNote != null ? topNote : "");
  }

  public static NetworkNotificationProtocol receiveInput( String inputStr ) {
    // receives a string of text and converts it into a handy object.
    NetworkNotificationProtocol nnp = new NetworkNotificationProtocol();

    if (inputStr.startsWith("Q")) {
      // Cue creation
      nnp.type = CUE;
      String[] strs = (inputStr.substring(1)).split(":",3);
      nnp.currentCue = strs[0];
      nnp.pendingCue = strs[1];
      nnp.controllerName = strs[2];
    } else if (inputStr.startsWith("P")) {
      nnp.type = PATCH;
      String[] strs = (inputStr.substring(1)).split(":",3);
      nnp.channelNum = Integer.parseInt(strs[0]);
      nnp.channelName = strs[1];
      nnp.patchDescription = strs[2];
    } else if (inputStr.startsWith("M")) {
      nnp.type = MAPPER;
      String[] strs = (inputStr.substring(1)).split(":",3);
      nnp.fromChannel = Integer.parseInt(strs[0]);
      nnp.toChannel = Integer.parseInt(strs[1]);
      nnp.mapDescription = strs[2];
    } else if (inputStr.startsWith("N")) {
      nnp.type = NOTE_WINDOW;
      String[] strs = (inputStr.substring(1)).split(":",4);
      nnp.channelNum = Integer.parseInt(strs[0]);
      nnp.channelName = strs[1];
      nnp.bottomNote = strs[2].isEmpty() ? null : Integer.parseInt(strs[2]);
      nnp.topNote = strs[3].isEmpty() ? null : Integer.parseInt(strs[3]);
    } else
      throw new IllegalArgumentException("Input '" + inputStr + "' is unrecognized");

    return nnp;
  }

  // The standard port for Qualm network messages.
  public static int PORT = 25687;

  // Enum values for message types.
  public static int PATCH = 1;
  public static int CUE = 2;
  public static int MAPPER = 3;
  public static int NOTE_WINDOW = 4;

  // The instance information used to deserialize information
  public int type;
  // Patch info
  public int channelNum;
  public String channelName;
  public String patchDescription;
  // Cue info
  public String controllerName;
  public String currentCue;
  public String pendingCue;
  // Event Map info
  public int fromChannel;
  public int toChannel;
  public String mapDescription;
  // Note Window info
  public Integer bottomNote;
  public Integer topNote;
  
}
