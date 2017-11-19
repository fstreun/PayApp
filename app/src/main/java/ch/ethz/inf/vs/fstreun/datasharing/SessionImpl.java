package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONArray;
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


    @Override
    public boolean add(JSONObject content) {
        Block block = new Block(content);
        return add(block);
    }


    @Override
    public List<JSONObject> getContent() {
        return chainsToList(getData());
    }

    @Override
    public List<JSONObject> getContentAfter(Map<UUID, Integer> start) {
        return chainsToList(getDataAfter(start));
    }

    private List<JSONObject> chainsToList(Map<UUID, ChainImpl> chains){
        List<JSONObject> res = new ArrayList<>();
        for (ChainImpl c : chains.values()){
            List<Block> blocks = c.getBlocks();
            for (Block b : blocks){
                res.add(b.getContent());
            }
        }
        return res;
    }
}
