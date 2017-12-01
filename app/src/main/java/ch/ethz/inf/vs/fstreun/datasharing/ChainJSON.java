package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by fabio on 11/18/17.
 */

public interface ChainJSON {

    public Integer appendJSON(JSONArray chain, int expected) throws JSONException;

    public Integer appendJSON(JSONObject block, int expected);

    public JSONArray subChainJSON(int start) throws JSONException;

    public JSONObject getBlockJSON(int position);
}
