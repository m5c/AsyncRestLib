package eu.kartoffelquadrat.asyncrestlib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.Test;

/**
 * Tests whether the identity transformator correctly handles input (should not modify any fields).
 *
 * @author Maximilian Schiedermeier on 2019/09/02 maximilian.schiedermeier@mail.mcgill.ca
 */
public class IdentityTransformerTest {

    private Transformer identityTransformer;
    private BroadcastContent content;
    private String contentPayload = "abc123";


    public IdentityTransformerTest() {

        identityTransformer = new IdentityTransformer();
        content = new StringBroadcastContent(contentPayload);
    }

    @Test
    public void testIdentityTransformerOutput() {
        ObjectWriter defaultSerializer = new ObjectMapper().writer();
        assert BroadcastContentHasher.hash(defaultSerializer, identityTransformer.transform(content, null)).equals(BroadcastContentHasher.hash(defaultSerializer, new StringBroadcastContent(contentPayload)));
        assert BroadcastContentHasher.hash(defaultSerializer, identityTransformer.transform(content, contentPayload)).equals(BroadcastContentHasher.hash(defaultSerializer, new StringBroadcastContent(contentPayload)));

    }
}
