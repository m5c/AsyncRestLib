package eu.kartoffelquadrat.asyncrestlib;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the BroadcastContentManager, notably the latch for unblocking on asynchronous changes.
 * @author Maximilian Schiedermeier on 2019/09/02 maximilian.schiedermeier@mail.mcgill.ca
 */
public class BroadcastContentManagerTest {

    private BroadcastContent content;
    private BroadcastContentManager manager;
    private String defaultContentString = "abc123";
    private String updateContentString = "bcd234";

    /**
     * Reset manager state so next test starts with clean state
     */
    @Before
    public void resetForNextTest() {
        content = new StringBroadcastContent(defaultContentString);
        manager = new BroadcastContentManager(content);
    }


    /**
     * Test awaiting update without manager termination verify the termination flag is not set and the content has
     * changed
     */
    @Test
    public void testAwaitUpdate() {

        // start extra thread to unlock the blocking method
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            manager.updateBroadcastContent(new StringBroadcastContent(updateContentString));
        }).start();

        // block until update, then check content and termination flag
        assertFalse(manager.awaitUpdate());
        assertFalse(manager.isTerminated());
        assertTrue(BroadcastContentHasher.hash(manager.getCurrentBroadcastContent()).equals(BroadcastContentHasher.hash(new StringBroadcastContent(updateContentString))));
    }


    /**
     * Test awaiting update with manager termination verify the termination flag is set anf the content unchanged
     */
    @Test
    public void testTerminate() {
        // start extra thread to unlock the blocking method
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            manager.terminate();
        }).start();

        // block until update, then check content and termination flag
        assertTrue(manager.awaitUpdate());
        assertTrue(BroadcastContentHasher.hash(manager.getCurrentBroadcastContent()).equals(BroadcastContentHasher.hash(new StringBroadcastContent(defaultContentString))));
    }

    /**
     * Once terminated, a content manager can not be updated any more. Check for a runtime exception
     */
    @Test(expected = RuntimeException.class)
    public void testUpdateOfTerminated() {
        manager.terminate();
        manager.updateBroadcastContent(new StringBroadcastContent(updateContentString));
    }

    @Test
    public void contentRetrieval() {
        assertTrue(BroadcastContentHasher.hash(manager.getCurrentBroadcastContent()).equals(BroadcastContentHasher.hash(new StringBroadcastContent(defaultContentString))));
        manager.updateBroadcastContent(new StringBroadcastContent(updateContentString));
        assertTrue(BroadcastContentHasher.hash(manager.getCurrentBroadcastContent()).equals(BroadcastContentHasher.hash(new StringBroadcastContent(updateContentString))));
    }

    /**
     * Verify the correct has is returned for a given content
     */
    @Test
    public void verifyHash() {
        // The MD5 of the default string abc123 is e99a18c428cb38d5f260853678922e03
        String expectedHash = BroadcastContentHasher.hash(new StringBroadcastContent("abc123"));
        assertTrue(manager.getContentHash().equals(expectedHash));
    }
}
