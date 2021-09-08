package eu.kartoffelquadrat.asyncrestlib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Helper class to generate the MD5 hash of a string-serialized version of broadcast-content. This can be used on client
 * and server-side.
 *
 * @author Maximilian Schiedermeier
 */
class BroadcastContentHasher {

    /**
     * Computes the MD5 sum of the JSON serialization string for a provided BroadcastContent object.
     *
     * @param objectWriter as the serializer to be used to convert the content into a JSON string.
     * @param content as the object to be serialized and hashed.
     * @return
     */
    protected static String hash(ObjectWriter objectWriter, BroadcastContent content) {
        try {
            String jsonString = objectWriter.writeValueAsString(content);
            return DigestUtils.md5Hex(jsonString);
        } catch (JsonProcessingException jex) {
            throw new RuntimeException("Unable to serialize provided BroadcastContent: " + content);
        }
    }
}
