package eu.kartoffelquadrat.asyncrestlib;

/**
 * Nonsense transformer that reduces any message to the emptystring, unless it contains the received tag.
 */
public class EraserTransformer implements Transformer<StringBroadcastContent> {

    /**
     * See class description.
     * @param inputBroadcastContent as the original broadcastContent that the transformed copy is based on.
     * @param transformerTag        as a lib-user provided tag that can be used e.g. as a filter
     * @return a new StringStateBroadcastContent, with empty string as payload.
     */
    @Override
    public StringBroadcastContent transform(StringBroadcastContent inputBroadcastContent, String transformerTag) {
        if(inputBroadcastContent.contains(transformerTag))
            return inputBroadcastContent;
        else
            return new StringBroadcastContent("");
    }
}
