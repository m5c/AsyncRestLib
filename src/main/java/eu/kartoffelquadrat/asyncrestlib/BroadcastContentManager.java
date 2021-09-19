package eu.kartoffelquadrat.asyncrestlib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.concurrent.CountDownLatch;

/**
 * The Broadcast Content Manager (BCM) maintains a reference to the current content state (Broadcast Content). The
 * entity behind the reference should not be internally modified, but replaced as a whole. If changes appear the entire
 * reference should eb updated, via the updateBroadCastContent method.
 *
 * @author Maximilian Schiedermeier
 */
public class BroadcastContentManager<C extends BroadcastContent> {

    // the modelMapper used for serialization (is the default jackson object mapper if no custom mapper was provider by
    // constructor)
    private final ObjectMapper objectMapper;
    // stores a universal latch that is unblocked and replaced every time the server status changes.
    CountDownLatch stateUpdateLatch = new CountDownLatch(1);
    // the current content. Can be updated.
    private C currentBroadcastContent;
    // a broadcast manager can be actively terminated. If this happens, all the latch is unblocked and a terminated flag
    // is set. This way open connections can be gracefully closed.
    private boolean terminated = false;

    /**
     * Standard constructor for a BroadcastContentManager. To be used if no custom serialization rules are required for
     * the BroadcastContent.
     *
     * @param content as the resource content observed by subscribers.
     */
    public BroadcastContentManager(C content) {
        this.currentBroadcastContent = content;
        objectMapper = new ObjectMapper();
    }

    /**
     * Advanced constructor for a BroadcastContentManager. Should only be used if the provided BroadcastContent is not
     * serializable with the default ObjectMapper. This is e.g. the case for object with inner circular dependencies.
     *
     * @param objectMapper as user provided serializer that applies custom rules during serialization of a
     *                     BroadcastContent.
     * @param content      as the resource content observed by subscribers.
     */
    public BroadcastContentManager(ObjectMapper objectMapper, C content) {
        this.objectMapper = objectMapper;
        this.currentBroadcastContent = content;
    }

    /**
     * Blocks the calling thread until there is something new to propagate (update of referenced broadcast content)
     * returns true if the update was due to termination of the broadcastContentManager, false otherwise.
     *
     * @return a flag that indicates whether there are further updates to expect after this one.
     */
    protected boolean awaitUpdate() {
        try {
            stateUpdateLatch.await();
            return isTerminated();
        } catch (InterruptedException ie) {
            throw new RuntimeException("Unable to await broadcast update.");
        }
    }

    /**
     * Updates the maintained content and unblocks the latch. Empty content or content identical by hash is rejected.
     *
     * @param contentUpdate the BroadcastContent update.
     */
    public void updateBroadcastContent(C contentUpdate) {
        if (isTerminated()) {
            throw new RuntimeException("Content can not be updated any more. The broadcast manager is already " +
                    "terminated.");
        }

        boolean cisEmpty = contentUpdate.isEmpty();
        String originalHash = getContentHash();
        String updateHash = getHashOfCustomContentUsingAssociatedSerializer(contentUpdate);
        if (!cisEmpty && !originalHash.equals(updateHash)) {
            this.currentBroadcastContent = contentUpdate;

            touch();
        }
    }

    /**
     * Manually advises to BroadcastContentManager that the content hash changed. The BroadcastContentManager then
     * unblocks all subscribers, waiting for an update. Usually calling this method is not required, for
     * BroadcastContents are supposed to be implemented as immutables. The default way to notify the
     * BroadcastContentManager about an update is therefore the "updateBroadcastContent" method. Use this one only if
     * your BroadcastContent is not immutable and you modified the internals of the instance maintained by the
     * BroadcastContentManager.
     */
    public void touch() {
        if (isTerminated()) {
            throw new RuntimeException("Content can not be updated any more. The broadcast manager is already " +
                    "terminated.");
        }

        // unblock all threads blocked by current latch
        stateUpdateLatch.countDown();

        // create a new latch for future threads
        stateUpdateLatch = new CountDownLatch(1);
    }

    /**
     * Call this method to prevent further updates. Calling this method unblocks subscribers to updates. The ARL
     * furthermore sets the HTTP return code to 500, to indicate that no further updates will be provided for this
     * resource.
     */
    public void terminate() {
        this.terminated = true;

        // unblock all threads blocked by current latch
        stateUpdateLatch.countDown();
    }


    /**
     * Getter to tell whether this BroadcastContentManager declines further updates.
     *
     * @return a flag to indicate if this manager is already terminated.
     */
    public boolean isTerminated() {
        return terminated;
    }

    /**
     * Returns the md5-sum of the serialized version of the currently stored content. This can be used to avoid status
     * updates when the managed content hs not actually changed.
     *
     * @return the hash of the content.
     */
    public String getContentHash() {
        return BroadcastContentHasher.hash(objectMapper.writer(), currentBroadcastContent);
    }

    /**
     * Getter to look up the current state of the maintained broadcast content.
     *
     * @return current broadcast content.
     */
    public C getCurrentBroadcastContent() {
        return currentBroadcastContent;
    }


    /**
     * Returns the immutable writer belonging to this BroadcastContentManager. Can be used to convert e.g. transformed
     * broadcastContents.
     *
     * @return object writer which is capable of serializing objects the same way this BCM would have processed them.
     */
    // TODO: Verify if needed.
    public ObjectWriter getImmutableSerializer() {
        return objectMapper.writer();
    }

    /**
     * Returns the json serialization computed for a provided BroadcastContent, using the serializer associated to this
     * BCM.
     *
     * @return String json string serialization of the received object, produced with serializer associated to this BCM.
     */
    public String serializeCustomContentUsingAssociatedSerializer(BroadcastContent customContent) {
        try {
            return objectMapper.writeValueAsString(customContent);
        } catch (JsonProcessingException jex) {
            throw new RuntimeException("Unable to serialize provided custom BroadcastContent: " + customContent);
        }
    }

    /**
     * Returns the hash computed for a provided BroadcastContent, using the serializer associated to this BCM.
     *
     * @return String md5 hash of the received object, serialized with serializer associated to this BCM.
     */
    public String getHashOfCustomContentUsingAssociatedSerializer(BroadcastContent customContent) {

        return BroadcastContentHasher.hash(objectMapper.writer(), customContent);
    }
}
