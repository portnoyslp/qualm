package qualm;

import java.io.*;
import java.util.*;

// Useful utility procedures that can be used from various places.
public class Utilities { 
    private static List numList = 
    Arrays.asList( new String[] { 
      "c","c#","d","d#","e","f","f#","g","g#","a","a#","b"
    });

  public static int noteNameToMidi ( String noteName ) {
    try {
      return Integer.parseInt( noteName );
    } catch (NumberFormatException nfe) {
    }

    // convert to lowercase to make our lives easier.
    noteName = noteName.toLowerCase();
    
    // get the key
    String key = noteName.substring(0,1);
    noteName = noteName.substring(1);
    
    // get the sharp/flat (and convert to just sharps)
    if (noteName.startsWith("#")) {
      key = key + "#";
      noteName = noteName.substring(1);
    } else if (noteName.startsWith("b")) {
      // Use # instead of flat
      char k = key.charAt(0);
      if (k == 'a') {
	key = "g#";
      } else if (k == 'c') {
	key = "b";
      } else if (k == 'f') {
	key = "e";
      } else {
	k--;
	key = new String( new char[] { k } );
	key = key + "#";
      }
      noteName = noteName.substring(1);
    }
    // everything else is the octave number.
    int octave = Integer.parseInt(noteName)+1;

    if (key.equals("e#")) key="f";

    if (key.equals("b#")) {
      key="c";
      octave++;
    }
    
    return (octave*12 + numList.indexOf( key ));
  }
  
}
