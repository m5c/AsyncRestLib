package eu.kartoffelquadrat.asyncrestlib;

import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ForkJoinPool;

/**
 * The ResponseGenerator provides notifications about status changes registered by a provided BroadcastContentManager.
 * The result of the below methods can be directly used as result object of the calling Spring Rest controllers to
 * long-polling rest clients. All of the three offered methods place on of the following HTTP response codes in the
 * returned object: 200 (OK) if an update occurred, 408 (Timeout) if no update occurred before the provided timeout,
 * 204 (NoContent) if no more updates are to expect.
 *
 * @author Maximilian Schiedermeier
 */
public class ResponseGenerator {

    /**
     * The most basic usage of the Async Rest Library. If called, the result is deferred until a status change appears
     * on server side (new BroadcastContent registered) or a timeout occurred, whatever comes first. The result only
     * carries a BroadcastContent (serialized as json string) in the body, if an update was registered. Waiting for
     * status changes is a blocking process, the reply therefore comes asynchronously.
     * @param longPollTimeout         maximum amount in milliseconds before a result is returned.
     * @param broadcastContentManager reference to the entity that handles broadcast content status updated.
     * @param <C>                     as the specific library-external class that implements the broadcastContent
     *                                interface. Using a generic allows to use the library without a need to cast the
     *                                result.
     * @return a DeferredResult that wraps the determined BroadCastContent revision in a ResponseEntity so it can be
     * used for HTTP/REST replies. As the eliciting process is asynchronous, the result furthermore has to be wrapped
     * into a DeferredResult with preset timeout.
     */
    public static <C extends BroadcastContent> DeferredResult<ResponseEntity<String>> getAsyncUpdate(long longPollTimeout, BroadcastContentManager broadcastContentManager) {

        return getDeferredResult(longPollTimeout, broadcastContentManager, null, new IdentityTransformer(), null);
    }

    /**
     * Overloaded variant of previous method that additionally uses a MD-5 hashsum to additionally enable direct
     * BroadcastContent updates.
     *
     * @param longPollTimeout         maximum amount in milliseconds before a result is returned.
     * @param broadcastContentManager reference to the entity that handles broadcast content status updated.
     * @param broadcastContentHash    a MD5-Hashsum of an external BroadcastContent object (typically at client side).
     *                                If this parameter is present, the response will be synchronous if (and only if)
     *                                the received hash differs from the BroadcastContent currently stored in the
     *                                BroadcastContentManager. In case of a match the requesting entity already hold the
     *                                most recent BroadcastContent revision and therefore will be informed about the
     *                                next relevant revision asynchronously. The hash must not be null. If an empty
     *                                string is provided, the current BroadcastContent cannot match and an the current
     *                                BroadcastContent is returned as immediate result.
     * @param <C>                     as the specific library-external class that implements the broadcastContent
     *                                interface. Using a generic allows to use the library without a need to cast the
     *                                result.
     * @return a DeferredResult that wraps the determined BroadCastContent revision in a ResponseEntity so it can be
     * used for HTTP/REST replies. As the eliciting process is potentially asynchronous, the result furthermore has to
     * be wrapped into a DeferredResult with preset timeout.
     */
    public static <C extends BroadcastContent> DeferredResult<ResponseEntity<String>> getHashBasedUpdate(long longPollTimeout, BroadcastContentManager broadcastContentManager, String broadcastContentHash) {
        // hash can be empty string but must not be null
        if (broadcastContentHash == null)
            throw new NullPointerException("BroadcastContentHash is null. Use getAsyncUpdate(...) if you are only " + "interested in future updates or set it to the empty-string for a synced update.");

        return getDeferredResult(longPollTimeout, broadcastContentManager, broadcastContentHash,
                new IdentityTransformer(), null);
    }

    /**
     * Overloaded variant of previous Hash-based method that additionally invokes a transformation mechanism that allows
     * application specific server sided transformations prior to eliciting whether a BroadcastContent is relevant to
     * the caller. The main applications for transformations are server sided pub-sub mechanisms and server side
     * broadcast content cropping, e.g. if a certain client should only see a partial representation of the managed
     * content. See Transformer, IdentityTransformer for more details.
     *
     * @param longPollTimeout         maximum amount in milliseconds before a result is returned.
     * @param broadcastContentManager reference to the entity that handles broadcast content status updated.
     * @param broadcastContentHash    a MD5-Hashsum of an external BroadcastContent object (typically at client side).
     *                                If this parameter is present, the response will be synchronous if (and only if)
     *                                the received hash differs from the BroadcastContent currently stored in the
     *                                BroadcastContentManager. In case of a match the requesting entity already hold the
     *                                most recent BroadcastContent revision and therefore will be informed about the
     *                                next relevant revision asynchronously.
     * @param transformer             the algorithm run by the server to generate a manipulated copy that will
     *                                potentially be returned to the caller. Note that hash verifications are
     *                                effectuated on the outcome of this algorithm. Therefore a BroadcastContent update
     *                                on server side does not necessarily result in a notification for the caller (image
     *                                the outcome of the transformer being hash-wise identical to the previous state,
     *                                e.g. because the updated parts were removed by the transformer). Transformers are
     *                                in most cases application specific, therefore the user of this library has to
     *                                implement a custom modification algorithm for the specific BroadcastContent used
     *                                by implementing the Transformer interface.
     * @param transformTag            the string parameter used to steer the provided transformer entity. E.g. in case
     *                                of a pub-sub transformer this would be the keyword that must appear in a
     *                                BroadcastContent to prevent the content from being nullified. Or in case of a
     *                                blackening transformer this could be a userId that indicated the security
     *                                clearance for the erasing/cropping procedure.
     * @param <C>                     as the specific library-external class that implements the broadcastContent
     *                                interface. Using a generic allows to use the library without a need to cast the
     *                                result.
     * @return a DeferredResult that wraps the determined transformed BroadCastContent revision in a ResponseEntity so
     * it can be used for HTTP/REST replies. As the eliciting process is potentially asynchronous, the result
     * furthermore has to be wrapped into a DeferredResult with preset timeout.
     */
    public static <C extends BroadcastContent> DeferredResult<ResponseEntity<String>> getTransformedUpdate(long longPollTimeout, BroadcastContentManager<C> broadcastContentManager, String broadcastContentHash, Transformer<C> transformer, String transformTag) {

        // hash can be empty string but must not be null, so the private methods can distinguish it from the async
        // method (without hash involved)
        if (broadcastContentHash == null)
            throw new NullPointerException("BroadcastContentHash is null. Use getAsyncUpdate(...) if you are only " + "interested in future updates or set it to the empty-string for a synced update.");

        return getDeferredResult(longPollTimeout, broadcastContentManager, broadcastContentHash, transformer,
                transformTag);
    }

    /**
     * Used by public methods to generate deferred result based on status changes on broadcast content maintained by
     * provided broadcastContentManager.
     *
     * @param longPollTimeout         maximum amount in milliseconds before a result is returned.
     * @param broadcastContentManager reference to the entity that handles broadcast content status updated.
     * @param clientContentHashString hash of broadcast content that is considered the callers current state. A response
     *                                is returned as soon as the maintained (and transformed) broadcastContent showcases
     *                                a hash distinct to the received hash. If the hash is null immediate synchronous
     *                                updates are excluded.
     * @param transformer             the transformation algorithm to be applied on the maintained broadcastContent
     *                                before considering a response generation. If no transformation is desired an
     *                                identity-transformer can be passed as argument.
     * @param transformTag            an additional string that can be used by the optional transformer to steer the
     *                                applied transformation algorithm. Can be null e.g. if the identity-transformer is
     *                                applied.
     * @param <C>                     as the specific library-external class that implements the broadcastContent
     *                                interface. Using a generic allows to use the library without a need to cast the
     *                                result.
     * @return a DeferredResult that wraps the determined transformed BroadCastContent revision in a ResponseEntity so
     * it can be used for HTTP/REST replies. As the eliciting process is potentially asynchronous, the result
     * furthermore has to be wrapped into a DeferredResult with preset timeout.
     */
    private static <C extends BroadcastContent> DeferredResult<ResponseEntity<String>> getDeferredResult(long longPollTimeout, BroadcastContentManager<C> broadcastContentManager, String clientContentHashString, Transformer<C> transformer, String transformTag) {

        // First of all don't bother with closed endpoints, directly send a 204 (Gone).
        if(broadcastContentManager.isTerminated()) {
            DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>(longPollTimeout);
            deferredResult.setErrorResult(ResponseEntity.status((HttpStatus.GONE)));
            return deferredResult;
        }

        // We configure a timeout + strategy, so we automatically get an HTTP timeout header if no update was
        // registered by the broadcastContentManager within a given time-frame.
        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>(longPollTimeout);
        deferredResult.onTimeout(() -> deferredResult.setErrorResult(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Request timeout occurred."))); // This is very hard to test with JUNIT.

        // If a hash is provided we run a preliminary check (the current broadcast content might already be new to the
        // caller. In that case we forget about async updates and directly return the current broadcast content as
        // synchronous reply.
        if (isSynchronousUpdateRequired(clientContentHashString, broadcastContentManager.getCurrentBroadcastContent()
                , transformer, transformTag)) {
            // The currently stored version is an update to caller. We therefore send a direct update
            C transformedBroadcastContent =
                    transformer.transform(broadcastContentManager.getCurrentBroadcastContent(), transformTag);

            // Note that ResponseEntity do not support proper json serialization of custom objects out of the box.
            // Therefore the payload is a JSON string that we crated with GSON.
            deferredResult.setResult(ResponseEntity.ok(new Gson().toJson(transformedBroadcastContent)));
            return deferredResult;
        }


        // When the above IF did not trigger, the client either already holds the current version (so we need to
        // wait for something to happen on server side), or he did not provide a hash at all (so he is only
        // interested in versions resulting from a future status change). In either case we have to wait for updates
        // and provide an asynchronous result. The waiting is done in an extra thread, so the application-container's
        // worker thread who handles this call-stack is not blocked.
        // The new thread can be interrupted by the already running DeferredResult's timeout handler.
        // This is a blocking thread, so we use the ForkJoinPoll, to avoid waisting ordinary worker threads.
        Thread awaitInternalStateChangeAndUpdateDeferredResultIfNeededThread =
                new AsyncNotifyThread<>(broadcastContentManager,
                        clientContentHashString, transformer, transformTag, deferredResult);
        awaitInternalStateChangeAndUpdateDeferredResultIfNeededThread.start();

        return deferredResult;
    }

    /**
     * This method uses a provided hash to check if the caller holds a deprecated version of thecurrently managed
     * broadcastContent (or the requested transformation of the current broadcastContent). This check is required to
     * determine if a direct synchronous update is applicable.
     */
    private static boolean isSynchronousUpdateRequired(String callerBroadcastContentHash,
                                                       BroadcastContent currentBroadcastContent,
                                                       Transformer transformer, String transformerTag) {
        // Direct updates are only possible if a hash mismatch can be detected. No hash provided, means no direct
        // updates possible.
        if (callerBroadcastContentHash == null)
            return false;

        // compute hash of the transformed version of what is currently stored.
        BroadcastContent transformedBroadcastContent = transformer.transform(currentBroadcastContent, transformerTag);

        // Immediate updates are not required for empty / whitespace messages.
        if (transformedBroadcastContent.isEmpty())
            return false;

        // a direct update is required, if the computed hash is distinct to the version provided by the caller.
        return !callerBroadcastContentHash.equals(BroadcastContentHasher.hash(transformedBroadcastContent));
    }

}
