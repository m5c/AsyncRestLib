package eu.kartoffelquadrat.asyncrestlib;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the ARLs timeout behavior (return code) and burst messages.
 *
 * @author Maximilian Schiedermeier on 2019/09/02 maximilian.schiedermeier@mail.mcgill.ca
 */
public class ContentUpdateResponseGeneratorTest {

    private String defaultContentString;
    private BroadcastContentManager bcm;
    private final int timeout = 100;

    @Before
    public void prepareTest() {
        defaultContentString = "27225ea03d26abf31a83b3cae6d78489";
        bcm = new BroadcastContentManager(new StringBroadcastContent(defaultContentString));
    }

    /**
     * If we do not provide a hash, the library must defer the result until a content update has occurred on server side
     * (or a timeout happened). If we directly check for a result, it should not yet be set.
     */
    @Test
    public void testMissingImmediateResultWithoutHash() {

        // we expect that no result is set, for we provided no hash (so we are only interested in future updates) and
        // there were no updates on server side.
        DeferredResult<ResponseEntity<String>> deferredResult = ResponseGenerator.getAsyncUpdate(timeout, bcm);
        assertFalse(deferredResult.hasResult());
        assertFalse(deferredResult.isSetOrExpired());
    }

    /**
     * DEACTIVATED - Very unreliable to test due to required timers.
     * If a custom transformer is provided that turns the init message into a zero length string or only whitespace
     * string, no update must be emitted. Expected result is therefore a timeout and NOT an empty string.
     */
//    @Test
//    public void testNoMessageForInitMessageOnTransformationToEmptyString() {
//
//        AtomicBoolean threadwasactive = new AtomicBoolean(false);
//
//        // we simulate an internal server status change that occurs before timeout
//        new Thread(() -> {
//            try {
//                Thread.sleep(30);
//            } catch (InterruptedException e) {
//                throw new RuntimeException("Thread sleep failed");
//            }
//            threadwasactive.set(true);
//
//            // we update by something containing an x
//            bcm.updateBroadcastContent(new StringBroadcastContent("xyz"));
//        }).start();
//
//
//        // we expect that the init message is NOT returned, but that the update of above thread is returned (contains an "x")
//        // this means that the next line blocks until above thread updated the content.
//        assertFalse(threadwasactive.get());
//        DeferredResult<ResponseEntity<String>> deferredResult = ResponseGenerator.getTransformedUpdate(timeout, bcm, "someDummyHash", new EraserTransformer(), "x");
//        assertTrue(threadwasactive.get());
//
//        // We expect that there is a result set and that it contains the string "xyz"
//        assertTrue(deferredResult.hasResult());
//        assertTrue(deferredResult.isSetOrExpired());
//        assertTrue(((ResponseEntity<String>) deferredResult.getResult()).getStatusCode().value() == 200);
//        assertTrue((((ResponseEntity<String>) deferredResult.getResult()).getBody().contains("xyz")));
//    }

    /**
     * Create a new ResponseGenerator with 50ms timeout, trigger timeout and check the return code in the HTTP header is
     * correct. Note that the timeout can ONLY OCCUR if the hash of the default value is provided.
     */
    @Test
    public void testTimeoutReturnCode() throws InterruptedException {

        // Wait for the next update (or timeout), whatever the current content
        DeferredResult<ResponseEntity<String>> deferredResult =
                ResponseGenerator.getAsyncUpdate(timeout, bcm);

        assertFalse(deferredResult.hasResult());
        assertFalse(deferredResult.isSetOrExpired());

        // Usually I would now use a thread sleep to provoke an invokation of the timeout method. However the call to
        // the timeout is handled by a spring controller (happens outside this lib) - so we can not test it.
        //Thread.sleep(500); // TIMEOUT IS NOT INVOKED IF THERE IS NO ACTUAL SPRING CONTROLLER INVOLVED
        // Therefore we invoke the code of the timeout callback manually, as if it had been done by spring
        deferredResult.setErrorResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Request timeout " +
                "occurred."));

        assertTrue(deferredResult.isSetOrExpired());
        assertTrue(((ResponseEntity<BroadcastContent>) deferredResult.getResult()).getStatusCode().value() == 408);
    }

    /**
     * A client who does not provide a hash mast be notified about all status updates
     */
    @Test
    public void testHashlessTransformerlessUpdates() {
        // Create some state that will be observed by the remote client
        BroadcastContentManager bcm = new BroadcastContentManager(new StringBroadcastContent("A"));

        // now register a client to the responseGenerator and make sure all fired updates (including the initial
        // state "A" are registered) -> we provide an empty string as hash to get the initial state (synchronized).
        StringResponseCollectingClient stringResponseCollectingClientClient =
                new StringResponseCollectingClient(timeout, bcm, "");
        assertTrue(stringResponseCollectingClientClient.getBufferedJsonStringResponseEntities().size() == 1);
    }
}
