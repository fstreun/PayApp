package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by fabio on 11/19/17.
 * Factory of Block of type B
 */

public interface BlockFactory<B extends Block> {

    public B createFromJSON(JSONObject object) throws JSONException;

    public JSONObject getJSONObject(B block) throws JSONException;

}
