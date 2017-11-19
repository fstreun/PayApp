package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by fabio on 11/12/17.
 *
 */

public class SessionImpl extends Session<ChainImpl> implements SessionClient {

    public SessionImpl(UUID sessionID, UUID userID) {
        super(sessionID, userID, new ChainImpl());
    }


    @Override
    public boolean add(JSONObject content) {
        Block block = new BlockImpl(content);
        return add(block);
    }

    @Override
    public List<JSONObject> getContent() {
        return null;
    }

    @Override
    public List<JSONObject> getContentAfter(Map<UUID, Integer> start) {
        return null;
    }
}
