package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

    public SessionImpl(JSONObject object) throws JSONException {
        super(object, new ChainImpl());
    }

    @Override
    public boolean add(String content) {
        Block block = Block.createWithContent(content);
        return add(block);
    }

    @Override
    public List<String> getContent() {
        return chainsToList(getData());
    }

    @Override
    public List<String> getContentAfter(Map<UUID, Integer> start) {
        return chainsToList(getDataAfter(start));
    }

    private List<String> chainsToList(Map<UUID, ChainImpl> chains){
        List<String> res = new ArrayList<>();
        for (ChainImpl c : chains.values()){
            List<Block> blocks = c.getBlocks();
            for (Block b : blocks){
                res.add(b.getContent());
            }
        }
        return res;
    }
}
