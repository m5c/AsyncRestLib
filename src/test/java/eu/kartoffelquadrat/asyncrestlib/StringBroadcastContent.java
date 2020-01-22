package eu.kartoffelquadrat.asyncrestlib;

/**
 * Sample content for the Junit tests. It represents a custom implementation of the broadcastcontent interface (some
 * implementation has to be provided by any ARL user). In this case the state is super simple: just a string named
 * "content".
 *
 * @author Maximilian Schiedermeier on 2019/09/02 maximilian.schiedermeier@mail.mcgill.ca
 */
public class StringBroadcastContent implements BroadcastContent {

    private String content;

    public StringBroadcastContent(String defaultContent) {
        content = defaultContent;
    }

    @Override
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public String serialize() {
        return content;
    }

    @Override
    public boolean equalsByMD5(BroadcastContent other) {

        // reject null references
        if (other == null)
            return false;

        // compare by md5 hash, with lib-provided hash tool.
        return BroadcastContentHasher.hash(this).equals(BroadcastContentHasher.hash(other));
    }

    public boolean contains(String other)
    {
        return(content.contains(other));
    }
}
