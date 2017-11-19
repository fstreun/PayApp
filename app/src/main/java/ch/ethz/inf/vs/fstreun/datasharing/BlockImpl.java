package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by fabio on 11/19/17.
 * simple Block implementation
 * also a factory for itself
 */

public class BlockImpl implements Block, BlockFactory<BlockImpl> {

    public final static String KEY_CONTENT = "content";
    private final String content;

    public BlockImpl(String content) {
        this.content = content;
    }

    public String getContent(){
        return content;
    }

    @Override
    public BlockImpl createFromJSON(JSONObject object) throws JSONException {
        String content = object.getString(KEY_CONTENT);
        return new BlockImpl(content);
    }

    @Override
    public JSONObject getJSONObject(BlockImpl block) throws JSONException {
        String content = block.content;
        JSONObject object = new JSONObject()
                .put(KEY_CONTENT, content);
        return object;
    }
}
