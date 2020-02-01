package eu.kartoffelquadrat.asyncrestlib;

/**
 * Represents a broadcastable content. A content represents server state at one specific moment in time - the attributes
 * of this object are recommended to be all final and immutable. To propagate state changes, a new BroadcastContent
 * object should be created and passed to the manager. In case of a non-immutable BroadcastContent implementation,
 * changes can also be signaled, using the BroadcastContentManager's touch() method.
 *
 * @author Maximilian Schiedermeier
 */
public interface BroadcastContent {

    /**
     * must be implemented so we can filter empty beans (not considered updates).
     *
     * @return a flag whether the content is empty
     */
    boolean isEmpty();
}
