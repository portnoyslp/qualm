package qualm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class QDataTest {

  QData testData;

  @Before
  public void setup() {
    testData = new QDataBuilder()
      .addMidiChannel( 0, null, "Ch1")
      .addStream(new QStreamBuilder()
                 .addCue(new Cue.Builder()
                         .setCueNumber( "1.1" )
                         .addProgramChangeEvent( 0, new Patch( "P1", 1 ))
                         .build())
                 .addCue(new Cue.Builder()
                         .setCueNumber( "2.1" )
                         .addProgramChangeEvent( 0, new Patch( "P2", 2 ))
                         .build())
                 .build())
      .build();
  }

  @Test
  public void addingMidiChannelUpdatesPatchChanger() {
    QData qd = new QData();
    qd.addMidiChannel( 2, null, "Test");
    assertEquals(PatchChanger.getRequestedDeviceForChannel(2), null);
  }

  @Test	public void dumpWorks() {
    StringWriter sw = new StringWriter();
    testData.dump(new PrintWriter(sw));
    Assert.assertTrue(sw.toString().startsWith("Data dump for null"));
  }

  @Test
  public void trivialEquality() {
    QData qd1 = new QDataBuilder().build();
    QData qd2 = new QDataBuilder().build();
    assertEquals(qd1, qd2);
  }

  @Test
  public void titleEquality() {
    QData qd1 = new QDataBuilder().withTitle("Title").build();
    QData qd2 = new QDataBuilder().withTitle("Title").build();
    QData qd3 = new QDataBuilder().withTitle("Different Title").build();
    assertEquals(qd1, qd2);
    assertFalse(qd1.equals(qd3));
  }

  @Test
  public void channelEquality() {
    QData qd1 = new QDataBuilder().addMidiChannel(2, "Roland", "K2").build();
    QData qd2 = new QDataBuilder().addMidiChannel(2, "Roland", "K2").build();
    QData qd3 = new QDataBuilder().addMidiChannel(1, "Roland", "K1").build();
    assertEquals(qd1, qd2);
    assertFalse(qd1.equals(qd3));
  }

  @Test
  public void patchEquality() {
    QData qd1 = new QDataBuilder().addPatch( new Patch( "P1", 1 )).build();
    QData qd2 = new QDataBuilder().addPatch( new Patch( "P1", 1 )).build();
    QData qd3 = new QDataBuilder().addPatch( new Patch( "P2", 1 )).build();
    assertEquals(qd1, qd2);
    assertFalse(qd1.equals(qd3));
  }

  @Test
  public void cueStreamEquality() {
    QData qd1 = new QDataBuilder().addStream( new QStreamBuilder().withTitle("S1").build() ).build();
    QData qd2 = new QDataBuilder().addStream( new QStreamBuilder().withTitle("S1").build() ).build();
    QData qd3 = new QDataBuilder().addStream( new QStreamBuilder().withTitle("S2").build() ).build();
    assertEquals(qd1, qd2);
    assertFalse(qd1.equals(qd3));
  }
  @Test
  public void multipleCueStreamsEquality() {
    QData qd1 = new QDataBuilder().addStream( new QStreamBuilder().build() )
      .addStream( new QStreamBuilder().build() ).build();
    QData qd2 = new QDataBuilder().addStream( new QStreamBuilder().build() )
      .addStream( new QStreamBuilder().build() ).build();
    QData qd3 = new QDataBuilder().addStream( new QStreamBuilder().build() ).build();
    assertEquals(qd1, qd2);
    assertFalse(qd1.equals(qd3));
  }

  @Test
  public void variedEquality() {
    QData qd2 = new QDataBuilder()
      .addMidiChannel( 0, null, "Ch1")
      .addStream(new QStreamBuilder()
                 .addCue(new Cue.Builder()
                         .setCueNumber( "1.1" )
                         .addProgramChangeEvent( 0, new Patch( "P1", 1 ))
                         .build())
                 .addCue(new Cue.Builder()
                         .setCueNumber( "2.1" )
                         .addProgramChangeEvent( 0, new Patch( "P2", 2 ))
                         .build())
                 .build())
      .build();
    assertEquals(testData,qd2);
  }

}
