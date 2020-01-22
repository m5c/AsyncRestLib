package eu.kartoffelquadrat.asyncrestlib;

import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Imitates a client who buffers all received updates. (Specifically a client branded to StringStateBroadcastContent as
 * BroadcastContent instance)
 *
 * @author Maximilian Schiedermeier on 2019/09/02 maximilian.schiedermeier@mail.mcgill.ca
 */
public class StringResponseCollectingClient {

    // The client subscribes to a received content generator and buffers all replies.
    private List<String> bufferedJsonStringResponseEntities;

    /***
     * @param initialHash must not be null. Can be set to empty string to ensure immediate update or to a specific
     *                    hash to omit duplicate updates.
     */
    public StringResponseCollectingClient(long timeout, BroadcastContentManager bcm, String initialHash) {
        bufferedJsonStringResponseEntities = new LinkedList();

        // add a first response (only relevant in case a hash-mismatch was induced (?))
        bufferedJsonStringResponseEntities.add(((ResponseEntity<String>) ResponseGenerator.getHashBasedUpdate(timeout
                , bcm, initialHash).getResult()).getBody());
    }

    public List<String> getBufferedJsonStringResponseEntities() {
        return Collections.unmodifiableList(bufferedJsonStringResponseEntities);
    }
}
