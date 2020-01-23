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
     * must be implemented so we can filter empty beans (not considered updates).
     *
     * @return a flag whether the content is empty
     */
    boolean isEmpty();
}
