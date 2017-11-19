package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by fabio on 11/12/17.
 * optimal interface for a client using a session
 */

public interface SessionClient {

    public boolean add (JSONObject content);
    public List<JSONObject> getContent ();
    public List<JSONObject> getContentAfter (Map<UUID, Integer> start);
}
