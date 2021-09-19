package eu.kartoffelquadrat.asyncrestlib;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

/**
 * Tests the JsonProcessingException in BroadcastContentHasher by feeding an object with cyclical dependencies.
 */
public class UnserializableTest {

    /**
     * Verifies that hashing of unserializable content (implicit serialization) triggers runtime exception.
     */
    @Test(expected = java.lang.RuntimeException.class)
    public void hashUnserializable()
    {
        UnserializableBroadcastContent unserializable = new UnserializableBroadcastContent();
        BroadcastContentHasher.hash(new ObjectMapper().writer(), unserializable);
    }

    @Test(expected = java.lang.RuntimeException.class)
    public void serializeUnserializable()
    {
        UnserializableBroadcastContent unserializable = new UnserializableBroadcastContent();
        BroadcastContentManager<UnserializableBroadcastContent> unserializableBroadcastContentBroadcastContentManager = new BroadcastContentManager(new ObjectMapper(), unserializable);
        unserializableBroadcastContentBroadcastContentManager.serializeCustomContentUsingAssociatedSerializer(unserializable);
    }
}
