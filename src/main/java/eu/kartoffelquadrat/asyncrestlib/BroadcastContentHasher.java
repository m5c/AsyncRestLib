package eu.kartoffelquadrat.asyncrestlib;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Helper class to generate the MD5 hash of a string-serialized version of broadcast-content. This can be used on client
 * and server-side.
 *
 * @author Maximilian Schiedermeier
 */
public class BroadcastContentHasher {

    public static String hash(BroadcastContent content) {
        return DigestUtils.md5Hex(new Gson().toJson(content));
    }
}
