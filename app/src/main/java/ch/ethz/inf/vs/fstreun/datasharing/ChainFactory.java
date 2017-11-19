package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by fabio on 11/18/17.
 * Factory of Chains of type C
 */

public interface ChainFactory<C extends Chain> {

    public C createEmpty();

    public C createFromJSON(JSONArray object) throws JSONException;

    public JSONArray createJSON(C chain) throws JSONException;

}
