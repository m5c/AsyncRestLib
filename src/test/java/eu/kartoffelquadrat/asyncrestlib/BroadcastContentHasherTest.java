package eu.kartoffelquadrat.asyncrestlib;

import org.junit.Test;

/**
 * Tests if the ARL build in hasher produces the expected outcome. Hashes are always computed based on the
 * string-json-serialization of boradcastContent extending objects.
 *
 * @author Maximilian Schiedermeier on 2019/09/02 maximilian.schiedermeier@mail.mcgill.ca
 */
public class BroadcastContentHasherTest {

    @Test
    public void verifyHash() {
        String testString = "abc123";
        BroadcastContent bcContent = new StringBroadcastContent(testString);
        //Hash must correspond to: DigestUtils.md5Hex(new Gson().toJson(bcContent))
        assert BroadcastContentHasher.hash(bcContent).equals("11d01fbe9e6c039033d76fe26a2d9d77");
    }

}