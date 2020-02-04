package eu.kartoffelquadrat.asyncrestlib;

/**
 * Erases all messages ans always returns the empty string.
 */
public class ZapperTransformer implements Transformer<StringBroadcastContent> {

    /**
     * See class description.
     * @param inputBroadcastContent as the original broadcastContent that the transformed copy is based on.
     * @param transformerTag        irrelevant, the content is always zapped.
     * @return a new StringStateBroadcastContent, with empty string as payload.
     */
    @Override
    public StringBroadcastContent transform(StringBroadcastContent inputBroadcastContent, String transformerTag) {
            return new StringBroadcastContent("");
    }
}
