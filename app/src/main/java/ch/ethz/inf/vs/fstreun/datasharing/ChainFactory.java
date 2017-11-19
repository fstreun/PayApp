package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONObject;

/**
 * Created by fabio on 11/18/17.
 */

public interface ChainFactory<C extends Chain> {

    public C createEmpty();

    public C createFromJSON(JSONObject object);

    public JSONObject createJSON(C chain);
}
