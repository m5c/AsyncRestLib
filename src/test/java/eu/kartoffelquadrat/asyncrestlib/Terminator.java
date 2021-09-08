package eu.kartoffelquadrat.asyncrestlib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Duration;

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
    public void prepareTest() throws JsonProcessingException {
        defaultContentString = "27225ea03d26abf31a83b3cae6d78489";

        // Note that the hash is ALWAYS computed based on the JSON_String representation of the BroadcastContent,
        // and not just the payload.
        initialHash = DigestUtils.md5Hex(new ObjectMapper().writeValueAsString(new StringBroadcastContent(defaultContentString)));

        // Set timeout high enough so a terminator thread can kill the BCM while the client is waiting for updates.
        timeout = 5000;

        bcm = new BroadcastContentManager(new StringBroadcastContent(defaultContentString));
    }

    @Test
    public void terminateBcmWhileActiveHashlessSubscription() throws InterruptedException {

        // Pretend a client that instantly subscribes to the next bcm modification. The hashes match, so the initial
        // content is considered an update. This line will NOT block until the terminator went active, but the content of the returned deferred result will eventually change.
        DeferredResult<ResponseEntity<String>> response = ResponseGenerator.getAsyncUpdate(timeout, bcm);

        // since the above line is NOT blocking the result-body should be null.
        Assert.assertNull(response.getResult());

        // not terminate the BCM and verify that the response payload has changed.
        bcm.terminate();
        Assert.assertTrue(bcm.isTerminated());

        // The next line will lead to a awaitility timeout (and junit fail) after max 500 milli-seconds. Can only be unblocked if the deferred result payload is updated earlier.
        await().atMost(Duration.ofMillis(500)).until(() ->
        {
            return response.getResult() != null;
        });
    }

    @Test
    public void terminateBcmWhileActiveHashedSubscription() throws InterruptedException {
        // Pretend a client that instantly subscribes to the next bcm modification. The hashes match, so the initial
        // content is considered an update. This line will NOT block until the terminator went active, but the content of the returned deferred result will eventually change.
        DeferredResult<ResponseEntity<String>> response = ResponseGenerator.getHashBasedUpdate(timeout, bcm, initialHash);

        // since the above line is NOT blocking the result-body should be null.
        Assert.assertNull(response.getResult());

        // not terminate the BCM and verify that the response payload has changed.
        bcm.terminate();

        // The next line will lead to a awaitility timeout (and junit fail) after max 500 milli-seconds. Can only be unblocked if the deferred result payload is updated earlier.
        await().atMost(Duration.ofMillis(500)).until(() ->
        {
            return response.getResult() != null;
        });

        Assert.assertNotNull(response.getResult());
    }

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
