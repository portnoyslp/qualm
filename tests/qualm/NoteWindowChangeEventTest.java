package qualm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class NoteWindowChangeEventTest {

  @Test
  public void priorWhenNoPrevious() {
    NoteWindowChangeEvent nwce = new NoteWindowChangeEvent( 0, null, new Integer(30), new Integer(60) );
    NoteWindowChangeEvent prior = nwce.getPriorStateEvent();
    assertEquals(0, prior.getChannel());
    assertNull(prior.getBottomNote());
    assertNull(prior.getTopNote());
  }

  @Test
  public void undoWhenNoPrevious() {
    NoteWindowChangeEvent nwce = new NoteWindowChangeEvent( 0, null, new Integer(30), new Integer(60) );
    NoteWindowChangeEvent prior = nwce.getUndoEvent();
    assertEquals(0, prior.getChannel());
    assertNull(prior.getBottomNote());
    assertNull(prior.getTopNote());
  }

  @Test
  public void undoToPreviousWindow() {
    NoteWindowChangeEvent first = new NoteWindowChangeEvent( 0, null, new Integer(30), new Integer(60) );
    NoteWindowChangeEvent second = new NoteWindowChangeEvent( 0, null, null, new Integer(50) );
    second.setPrevious(first);
    NoteWindowChangeEvent undone = second.getUndoEvent();
    assertNull(undone.getBottomNote());
    assertEquals(undone.getTopNote(), new Integer(60));
  }

  @Test
  public void priorStateOfPreviousWindow() {
    NoteWindowChangeEvent first = new NoteWindowChangeEvent( 0, null, new Integer(30), new Integer(60) );
    NoteWindowChangeEvent second = new NoteWindowChangeEvent( 0, null, null, new Integer(50) );
    second.setPrevious(first);
    NoteWindowChangeEvent prior = second.getPriorStateEvent();
    assertEquals(prior.getBottomNote(), new Integer(30));
    assertEquals(prior.getTopNote(), new Integer(60));
  }

  @Test
  public void postStateOfChainedWindow() {
    NoteWindowChangeEvent first = new NoteWindowChangeEvent( 0, null, new Integer(30), new Integer(60) );
    NoteWindowChangeEvent second = new NoteWindowChangeEvent( 0, null, null, new Integer(50) );
    second.setPrevious(first);
    NoteWindowChangeEvent post = second.getPostStateEvent();
    assertEquals(post.getBottomNote(), new Integer(30));
    assertEquals(post.getTopNote(), new Integer(50));
  }

}
