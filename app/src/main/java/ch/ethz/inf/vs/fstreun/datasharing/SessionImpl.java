package ch.ethz.inf.vs.fstreun.datasharing;

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


    // identifier of the user which uses this object
    private final UUID userID;
    private static final String JSON_KEY_USER_ID = "user_id";

    public SessionImpl(UUID sessionID, UUID userID) {
        super(sessionID, new ChainImpl());
        this.userID = userID;
    }

    public SessionImpl(JSONObject object) throws JSONException {
        super(object, new ChainImpl());

        String errors = "";
        boolean error = false;

        UUID uID = null;
        try {
            uID = UUID.fromString(object.getString(JSON_KEY_USER_ID));
        } catch (JSONException e) {
            error = true;
            errors += "key " + JSON_KEY_USER_ID + " missing\n";
        }

        if (error){
            throw new JSONException("Session creation with JSONObject failed due to:\n" + errors);
        }

        userID = uID;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject res = super.toJSON();
        res.put(JSON_KEY_USER_ID, userID.toString());
        return res;
    }

    @Override
    public void add(String content) {
        Integer length = getLength().get(userID);
        if (length == null){
            length = 0;
        }
        put(userID, Block.createWithContent(content), length);
    }

    @Override
    public List<String> getContent() {
        return chainsToList(getData());
    }

    @Override
    public List<String> getContentAfter(Map<UUID, Integer> start) {
        return chainsToList(getDataAfter(start));
    }

    @Override
    public Map<UUID, ? extends Chain> getContentMap() {
        return getData();
    }

    @Override
    public Map<UUID, ? extends Chain> getContentMapAfter(Map<UUID, Integer> start) {
        return getDataAfter(start);
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

    public UUID getUserID(){
        return userID;
    }
}
