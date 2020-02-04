package eu.kartoffelquadrat.asyncrestlib;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HashedUpdateTest {

    private String defaultContentString = "27225ea03d26abf31a83b3cae6d78489";
    private String initialHash = DigestUtils.md5Hex(new Gson().toJson(defaultContentString));
    private BroadcastContentManager bcm;
    int timeout = 100;


    @Before
    public void prepareTest() {
        bcm = new BroadcastContentManager(new StringBroadcastContent(defaultContentString));
    }

    /**
     * Verify a direct updates is triggered if the initial hash does not match
     */
    @Test(expected = NullPointerException.class)
    public void updateOnNoHashCollision() {
        // register a client to the responseGenerator and make sure none of the fired updates is registered. Even the
        // initial hash matches, so absolutely no updated should be received. This leads to a nullpointer exception,
        // because the client directly tries to extract the body of the yet empty deferred result.
        StringResponseCollectingClient stringResponseCollectingClientClient =
                new StringResponseCollectingClient(timeout, bcm, "notTheINitHash", new ZapperTransformer(), "irrelevantIgnoredTag");
        assertTrue(stringResponseCollectingClientClient.getBufferedJsonStringResponseEntities().size() ==1);
    }

    /**
     * Verify no update is triggered, if the initial hash matches.
     */
    @Test(expected = NullPointerException.class)
    public void noUpdateOnHashCollision() {
        // register a client to the responseGenerator and make sure none of the fired updates is registered. Even the
        // initial hash matches, so absolutely no updated should be received. This leads to a nullpointer exception,
        // because the client directly treis to extract the body of the yet empty deferred result.
        StringResponseCollectingClient stringResponseCollectingClientClient =
                new StringResponseCollectingClient(timeout, bcm, initialHash, new ZapperTransformer(), "irrelevantIgnoredTag");
    }

}
