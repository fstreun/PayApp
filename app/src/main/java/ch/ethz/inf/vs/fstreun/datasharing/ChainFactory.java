package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by fabio on 11/18/17.
 */

public interface ChainFactory<C extends Chain> {

    public C createEmpty();

    public C createFromJSON(JSONArray object) throws JSONException;

    public C createFromBlocks (List<Block> blocks);

    public JSONArray createJSON(C chain);
}
