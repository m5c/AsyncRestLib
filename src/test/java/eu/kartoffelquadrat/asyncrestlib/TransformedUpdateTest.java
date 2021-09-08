package eu.kartoffelquadrat.asyncrestlib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;

public class TransformedUpdateTest {

    private String defaultContentString = "27225ea03d26abf31a83b3cae6d78489";
    private String initialHash = DigestUtils.md5Hex(new ObjectMapper().writeValueAsString(defaultContentString));
    private BroadcastContentManager bcm;
    int timeout = 100;

    public TransformedUpdateTest() throws JsonProcessingException {
    }


    @Before
    public void prepareTest() {
        bcm = new BroadcastContentManager(new StringBroadcastContent(defaultContentString));
    }


    /**
     * Verify transformed update requests are rejected if no hash is provided
     */
    @Test(expected = NullPointerException.class)
    public void verifyHashRequired() {
        ResponseGenerator.getTransformedUpdate(timeout, bcm, null, null, null);
    }
}
