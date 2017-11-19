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

    public boolean add (String content);
    public List<String> getContent ();
    public List<String> getContentAfter (Map<UUID, Integer> start);
}
