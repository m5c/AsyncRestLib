package eu.kartoffelquadrat.asyncrestlib;

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
        assert identityTransformer.transform(content, null).serialize().equals(contentPayload);
        assert identityTransformer.transform(content, contentPayload).serialize().equals(contentPayload);

    }
}
