package eu.kartoffelquadrat.asyncrestlib;

/**
 * Represents a broadcastable content. A content represents server state at one specific moment in time - the internals
 * of this object should therefore be implemented with final fields. To propagate state changes, a new BroadcastContent
 * object should be created and passed to the manager.
 *
 * @author Maximilian Schiedermeier
 */
public interface BroadcastContent {

    /**
     * must be implemented so we can filter empty messages (not considered updates).
     *
     * @return a flag whether the content is empty
     */
    boolean isEmpty();

    /**
     * The content must be convertable into something reconstructable, e.g. jsons or xmls.
     *
     * @return a serialized string representation
     */
    // TODO: check if obsolete!
    String serialize();

    /**
     * Helper method to determine whether this object is bitwise identical to another BroadcastContent entity.
     *
     * @param other as the object to compare this instance to
     * @return a flag whether this and the received object match by MD5 hash
     */
    public boolean equalsByMD5(BroadcastContent other);
}
