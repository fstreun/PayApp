package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by fabio on 11/12/17.
 * simple implementation of a Session with
 * ChainImpl and BlockImpl
 */

public class SessionImpl extends Session<ChainImpl, BlockImpl> implements SessionClient {

    static final BlockFactory<BlockImpl> blockFactory = new BlockImpl("");

    public SessionImpl(UUID sessionID, UUID userID) {
        super(sessionID, userID, new ChainImpl(blockFactory));
    }

    public SessionImpl(JSONObject object) throws JSONException {
        super(object, new ChainImpl(blockFactory));
    }

    @Override
    public boolean add(String content) {
        BlockImpl block = new BlockImpl(content);
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
            List<BlockImpl> blocks = c.getBlocks();
            for (BlockImpl b : blocks){
                res.add(b.getContent());
            }
        }
        return res;
    }
}
