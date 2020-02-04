package eu.kartoffelquadrat.asyncrestlib;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import static org.awaitility.Awaitility.await;

/**
 * Waiting clients should be unblocks if the BCM is terminated asynchronously.
 */
public class Terminator {

    private String defaultContentString;
    private String initialHash;
    private BroadcastContentManager bcm;
    int timeout;

    @Before
    public void prepareTest() {
        defaultContentString = "27225ea03d26abf31a83b3cae6d78489";
        initialHash = DigestUtils.md5Hex(new Gson().toJson(defaultContentString));

        // Set timeout high enough so a terminator thread can kill the BCM while the client is waiting for updates.
        timeout = 5000;
    }

    @Test
    public void terminateBcmWhileActiveHashlessSubscription() throws InterruptedException {
        bcm = new BroadcastContentManager(new StringBroadcastContent(defaultContentString));

        // Pretend a client that instantly subscribes to the next bcm modification. The hashes match, so the initial
        // content is considered an update. This line will NOT block until the terminator went active, but the content of the returned deferred result will eventually change.
        DeferredResult<ResponseEntity<String>> response = ResponseGenerator.getAsyncUpdate(timeout, bcm);

        // since the above line is NOT blocking the result-body should be null.
        Assert.assertNull(response.getResult());

        // not terminate the BCM and verify that the response payload has changed.
        bcm.terminate();
        Assert.assertTrue(bcm.isTerminated());

        // Give the AsyncNotifyThread a little time, so it can update the payload of the deferred result.
        // WHY DOES THIS FAIL IF I RUN ALL JUNITS AT ONCE???
        await().until(() ->
        {
            return response.getResult() != null;
        });
        Assert.assertNotNull(response.getResult());
    }

//    @Test
//    public void terminateBcmWhileActiveHashedSubscription() throws InterruptedException {
//        // Pretend a client that instantly subscribes to the next bcm modification. The hashes match, so the initial
//        // content is considered an update. This line will NOT block until the terminator went active, but the content of the returned deferred result will eventually change.
//        DeferredResult<ResponseEntity<String>> response = ResponseGenerator.getHashBasedUpdate(timeout, bcm, initialHash);
//
//        // since the above line is NOT blocking the result-body should be null.
//        Assert.assertNull(response.getResult());
//
//        // not terminate the BCM and verify that the response payload has changed.
//        bcm.terminate();
//
//        // Give the AsyncNotifyThread a little time, so it can update the payload of the deferred result.
//        Thread.sleep(100);
//
//        Assert.assertNotNull(response.getResult());
//    }

    /**
     * Verify a BCM can not be touched once it has been terminated
     */
    @Test(expected = RuntimeException.class)
    public void touchTerminatedBcm()
    {
        bcm.terminate();
        bcm.touch();
    }

}
