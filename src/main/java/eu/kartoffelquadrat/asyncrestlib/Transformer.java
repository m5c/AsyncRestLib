package eu.kartoffelquadrat.asyncrestlib;

/**
 * Classes implementing this interface can be used to transform internal server state changed, based on connection
 * specific tags. (The tag is provided by the spring rest controller, based on the connection parameters, e.g. clientid)
 * However the tag does no necessarily have to be connection specific. A client could for instance provide a tag
 * "allcaps" (to a corresponding char-manipulating transformer). If such tag is recognized by the transformer, the
 * ResponseGenerator can then transform all characters within a BroadcastContent to uppercase for customized results.
 *
 * @author Maximilian Schiedermeier
 */
public interface Transformer<T extends BroadcastContent> {

    /**
     * This method gets potentially invoked by the ResponseGenerator. It generates a modified copy of a BroadcastContent
     * instance, based on the algorithm specified in the concrete transformer implementation (code of this method). The
     * transformer tag serves as additional parameter for e.g. connection specific modifications.
     *
     * @param inputBroadcastContent as the original broadcastContent that the transformed copy is based on.
     * @param transformerTag        as a lib-user provided tag that can be used e.g. as a filter
     * @return the transformed copy of the input broadcastContent
     */
    T transform(T inputBroadcastContent, String transformerTag);
}

