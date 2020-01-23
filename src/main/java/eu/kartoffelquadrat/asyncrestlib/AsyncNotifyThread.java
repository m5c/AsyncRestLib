package eu.kartoffelquadrat.asyncrestlib;

import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * This thread blocks and awaits internal state changes (of the broadcastmanager). If the observed state change is
 * relevant to the subscribing client, it sets the result for the deferred http reply.
 *
 * @author Maximilian Schiedermeier
 * @param <C> as the user provided BroadcastContent extension to this library.
 */
class AsyncNotifyThread<C extends BroadcastContent> implements Runnable {


    private BroadcastContentManager<C> broadcastContentManager;
    private String clientContentHashString;
    private Transformer<C> transformer;
    String transformTag;
    DeferredResult<ResponseEntity<String>> deferredResult;


    /**
     * Constructor to set the parameters required infere whether an internal state change is relevant and to update the
     * deferred result object.
     *
     * @param broadcastContentManager as the entity holding the observed state
     * @param clientContentHashString as the hash of the current client state. Hash describes the string version of the
     *                                client object's json serialization.
     * @param transformer             as the optional transformer to apply on arising new states. This allows connection
     *                                / client speciific subscriptions.
     * @param transformTag            as an optional transformer parameter to customize the transformation to be
     *                                applied.
     * @param deferredResult          as the result object that is completed upon the first relevant status change
     */
    public AsyncNotifyThread(BroadcastContentManager<C> broadcastContentManager, String clientContentHashString,
                             Transformer<C> transformer, String transformTag,
                             DeferredResult<ResponseEntity<String>> deferredResult) {
        this.broadcastContentManager = broadcastContentManager;
        this.clientContentHashString = clientContentHashString;
        this.transformer = transformer;
        this.transformTag = transformTag;
        this.deferredResult = deferredResult;
    }

    /**
     * Concurrent functinoallity that blocks, waiting for status changes. Client response objects are not completed
     * until either of: server was instructed to shutdown (send 500, to tell client no more updates will come) a timeout
     * already occurred (408) - no need to deal with this, that is done by the deferred result, see above. an actual
     * update is ready for the client (200). That means either of: no hash was provided and the server state just
     * changed a hash was provided and it differs from the connection specific transformation of the most recent state
     * change.
     */
    @Override
    public void run() {
        boolean stopWaiting = false;

        // stores the final connection-specific update that may result from repeated transformations on generic
        // content updates.
        C connectionSpecificContent = null;

        while (!stopWaiting) {

            // Block until managed broadcast content changes
            // Stay in update loop until content new to current client content (hash difference).
            // Do not re-iterate if broadcast manager terminate gracefully (send "nomorecontent / 500)" message
            boolean closed = broadcastContentManager.awaitUpdate();

            // determine whether this update triggers a notification to the client. Thats the case if either of
            // (1/2/3) are fulfilled.
            // (1) no more updates to await, if server wants to shut down connection.
            // (2) no more update to await, if we just had a status change and no hash was provided
            boolean noHashProvided = clientContentHashString == null || clientContentHashString.isEmpty();
            // (3) no more update to await, if we just had a status change, a hash was provided, and the
            // (transformed) most recent update is not empty and differs in hash
            connectionSpecificContent = transformer.transform(broadcastContentManager.getCurrentBroadcastContent(),
                    transformTag);
            boolean contentEmpty = connectionSpecificContent == null || connectionSpecificContent.isEmpty();
            boolean relevantUpdate =
                    (!noHashProvided && !contentEmpty && !clientContentHashString.equals(BroadcastContentHasher.hash(connectionSpecificContent)));

            // check if any of (1) / (2) / (3) are fulfilled (loop end-criteria)
            stopWaiting = closed || noHashProvided || relevantUpdate;
        }

        // Now actually set the deferredMessages payload and send it.
        // If there is no content update, but the server closed the connection:
        if (broadcastContentManager.isTerminated())
            deferredResult.setErrorResult(ResponseEntity.noContent());
            // If there is an update (either due to lack of hash or actual hash mismatch of (transformed) update)
        else {
            // Note that ResponseEntity do not support proper json serialization of custom objects out of the box.
            // Therefore the payload is a JSON string that we crated with GSON.
            ResponseEntity<String> response = new ResponseEntity<>(new Gson().toJson(connectionSpecificContent),
                    HttpStatus.OK);
            deferredResult.setResult(response);
        }
    }
}