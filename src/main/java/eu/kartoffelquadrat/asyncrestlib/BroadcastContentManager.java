package eu.kartoffelquadrat.asyncrestlib;

import java.util.concurrent.CountDownLatch;

/**
 * The Broadcast Content Manager (BCM) maintains a reference to the current ontent state (Broadcast Content). The entity
 * behind the reference should not ne internally modified. If changes appear the entire reference should eb updated, via
 * the updateBroadCastContent method.
 *
 * @author Maximilian Schiedermeier
 */
public class BroadcastContentManager<C extends BroadcastContent> {

    // the current content. Can be updated.
    private C customBroadcastContent;

    // a broadcast manager can be actively terminated. If this happens, all the latch is unblocked and a terminated flag
    // is set. This way open connections can be gracefully closed.
    private boolean terminated = false;

    // stores a universal latch that is unblocked and replaced every time the server status changes.
    CountDownLatch stateUpdateLatch = new CountDownLatch(1);

    /**
     * Blocks the calling thread until there is something new to propagate (update of referenced broadcast content)
     * returns true if the update was due to termination of the broadcastContentManager, false otherwise.
     *
     * @return a flag that indicates whether there are further updates to expect after this one.
     */
    public boolean awaitUpdate() {
        try {
            stateUpdateLatch.await();
            return isTerminated();
        } catch (InterruptedException ie) {
            throw new RuntimeException("Unable to await broadcast update.");
        }
    }

    public BroadcastContentManager(C content) {
        this.customBroadcastContent = content;
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
        if (!contentUpdate.isEmpty() && !customBroadcastContent.equalsByMD5(contentUpdate)) {
            this.customBroadcastContent = contentUpdate;

            // unblock all threads blocked by current latch
            stateUpdateLatch.countDown();

            // create a new latch for future threads
            stateUpdateLatch = new CountDownLatch(1);
        }
    }

    // Ignore the "never used" warning, this method is called by the library-user.
    public void terminate() {
        this.terminated = true;

        // unblock all threads blocked by current latch
        stateUpdateLatch.countDown();
    }

    public boolean isTerminated() {
        return terminated;
    }

    /**
     * returns the md5-sum of the serialized version of the currently stored content. This can be used to avoid status
     * updates when the managed content hs not actually changed.
     *
     * @return the hash of the content
     */
    public String getContentHash() {
        return BroadcastContentHasher.hash(customBroadcastContent);
    }

    public C getCurrentBroadcastContent() {
        return customBroadcastContent;
    }
}
