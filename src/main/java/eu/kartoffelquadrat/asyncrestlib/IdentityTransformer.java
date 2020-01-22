package eu.kartoffelquadrat.asyncrestlib;

/**
 * Implementation of the TagSpecificTransformerInterface that returns BroadcastContent as-is, ignoring the received tag.
 * This is the default transformer that is internally used if no transformer is provided by the library user.
 *
 * @author Maximilian Schiedermeier
 */
public class IdentityTransformer<T extends BroadcastContent> implements Transformer<T> {

    @Override
    public T transform(T inputBroadcastContent, String transformerTag) {
        return inputBroadcastContent;
    }
}
