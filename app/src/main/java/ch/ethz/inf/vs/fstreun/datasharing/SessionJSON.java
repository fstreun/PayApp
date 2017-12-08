package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

/**
 * Created by fabio on 12/1/17.
 */

public interface SessionJSON {

    public Map<UUID, Integer> appendJSON(JSONObject map, Map<UUID, Integer> expected) throws JSONException;

    public JSONObject getJSON(Map<UUID, Integer> start) throws JSONException;

    public JSONObject getJSON() throws JSONException;
}
