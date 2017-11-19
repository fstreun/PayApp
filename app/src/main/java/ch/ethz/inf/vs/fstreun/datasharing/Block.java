package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by fabio on 11/12/17.
 * Immutable block
 */

public final class Block {

    public static String KEY_CONTENT = "content";
    private final String content;

    public static Block createFromJSON(JSONObject object) throws JSONException {
        String content = object.getString(KEY_CONTENT);
        return new Block(content);
    }

    public static Block createWithContent(String content){
        return new Block(content);
    }

    private Block(String content){
        this.content = content;
    }

    /**
     *
     * @return
     */
    public String getContent() {
        return content;
    }

    public JSONObject getJSONObject() throws JSONException {
        return new JSONObject()
                .put(KEY_CONTENT, content);
    }

}
