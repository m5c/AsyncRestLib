package eu.kartoffelquadrat.asyncrestlib;

/**
 * Class with inner cyclical dependency (references itself). Can not be serialized with a default ObjectMapper (serializer).
 * The class is used to test JsonProcessingException on BroadcastContentHasher.
 */
public class UnserializableBroadcastContent implements BroadcastContent {

    public UnserializableBroadcastContent cyclicalReference;

    public UnserializableBroadcastContent()
    {
        cyclicalReference = this;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
