package ch.ethz.inf.vs.fstreun.datasharing;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by fabio on 11/12/17.
 * Basic class for a session (not recommended for clients)
 * using Chains of type C
 */

public class Session implements SessionInterface, SessionJSON{

    // identifier of the session
    private final UUID sessionID;
    private static final String JSON_KEY_SESSION_ID = "session_id";

    // data in the session
    final Map<UUID, Chain> data = new HashMap<>();
    private static final String JSON_KEY_DATA = "data";


    /**
     * Create new basic Session
     * @param sessionID used by the session
     */
    public Session(UUID sessionID) {
        this.sessionID = sessionID;
    }


    /**
     * Create Session of an JSON Object
     * @param object representing the Session Object
     * @throws JSONException if fails
     */
    public Session(JSONObject object) throws JSONException {

        String errors = ""; // string of all errors occurred
        boolean error = false;

        UUID sID = null;
        try {
            sID = UUID.fromString(object.getString(JSON_KEY_SESSION_ID));
        } catch (JSONException e) {
            error = true;
            errors += "key " + JSON_KEY_SESSION_ID + " missing\n";
        }

        JSONObject map = null;
        try {
            map = object.getJSONObject(JSON_KEY_DATA);
        } catch (JSONException e) {
            error = true;
            errors += "key " + JSON_KEY_DATA + " missing\n";
        }


        if (error){
            throw new JSONException("Session creation with JSONObject failed due to:\n" + errors);
        }

        if (map != null){
            Iterator<String> iterator = map.keys();
            while (iterator.hasNext()){
                String key = iterator.next();
                data.put(UUID.fromString(key), new Chain(map.getJSONArray(key)));
            }
        }

        sessionID = sID;
    }


    /**
     * @return the JSON object representing this Session object
     * @throws JSONException if fails
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(JSON_KEY_SESSION_ID, sessionID.toString());
        JSONObject map = new JSONObject();
        for (Map.Entry<UUID, Chain> entry : data.entrySet()){
            map.put(entry.getKey().toString(), entry.getValue().toJSON());
        }
        object.put(JSON_KEY_DATA, map);
        return object;
    }


    public String toString(){
        try {
            return toJSON().toString();
        } catch (JSONException e) {
            return "Failed to create String of Session";
        }
    }



    /**
     * accesses all the chains in the session
     * @return a copy of the map with the chains
     */
    @Override
    public final Map<UUID, Chain> getData(){
        Map<UUID, Chain> res = new HashMap<>();
        for (Map.Entry<UUID, Chain> entry : data.entrySet()){
            res.put(entry.getKey(), (entry.getValue().clone()));
        }
        return res;
    }

    @Override
    public Map<UUID, Chain> getData(Map<UUID, Integer> after) {
        Map<UUID, Chain> res = new HashMap<>();
        for (Map.Entry<UUID, Chain> entry : data.entrySet()){
            UUID key = entry.getKey();
            Integer s = after.get(key);
            if (s == null){
                s = 0;
            }
            res.put(key, entry.getValue().getSubChain(s));
        }
        return res;
    }

    @Override
    public Map<UUID, Integer> putData(Map<UUID, Chain> mapData, Map<UUID, Integer> expected) {
        Map <UUID, Integer> res = new HashMap<>();
        for (Map.Entry<UUID, Chain> entry : mapData.entrySet()){
            if (!data.containsKey(entry.getKey())){
                data.put(entry.getKey(), new Chain());
            }

            Integer actual = data.get(entry.getKey()).append(entry.getValue(), expected.get(entry.getKey()));
            res.put(entry.getKey(), actual);
        }
        return res;
    }

    /**
     * appends chain if possible (hole in the chain is not allowed)
     * @param userID of the chain
     * @param chain to be appended
     * @param expected length of the current chain (position of first block to be appended)
     * @return actual length of the current chain before appending
     */
    @Override
    public final Integer putChain(UUID userID, Chain chain, int expected){
        if (!data.containsKey(userID)){
            data.put(userID, new Chain());
        }
        return data.get(userID).append(chain, expected);
    }

    /**
     * appends block if possible (hole in the chain is not allowed
     * @param userID of the chain
     * @param block to be appended
     * @param expected length of the current chain (position of the block to be appended)
     * @return
     */
    @Override
    public final Integer putBlock(UUID userID, Block block, int expected){
        if (!data.containsKey(userID)){
            data.put(userID, new Chain());
        }
        return data.get(userID).append(block, expected);
    }


    /**
     * @return length of current chains
     */
    @Override
    public final Map<UUID, Integer> getLength(){
        Map<UUID, Integer> res = new HashMap<>();
        for (Map.Entry<UUID, Chain> entry : data.entrySet()){
            res.put(entry.getKey(), entry.getValue().length());
        }
        return res;
    }


    /**
     * @return all user UUID the session has a chain of
     */
    @NonNull
    @Override
    public final Set<UUID> getAllUserID(){
        return data.keySet();
    }

    /**
     * @return the UUID of the Session
     */
    @Override
    public final UUID getSessionID(){
        return sessionID;
    }



    @Override
    public Map<UUID, Integer> appendJSON(JSONObject chainMap, Map<UUID, Integer> expected) throws JSONException {
        Map<UUID, Integer> res = new HashMap<>();


        for (Iterator<String> it1 = chainMap.keys(); it1.hasNext(); ) {
            String key = it1.next();
            UUID uKey = UUID.fromString(key);
            if (!data.containsKey(uKey)){
                data.put(uKey, new Chain());
            }

            Integer actaul = data.get(uKey).appendJSON(chainMap.getJSONArray(key), expected.get(uKey));
            res.put(uKey, actaul);
        }
        return res;
    }

    @Override
    public JSONObject getJSON(Map<UUID, Integer> start) throws JSONException {
        JSONObject res = new JSONObject();
        for (Map.Entry<UUID, Chain> entry : data.entrySet()){
            res.put(entry.getKey().toString(), entry.getValue().subChainJSON(start.get(entry.getKey())));
        }
        return res;
    }

    @Override
    public JSONObject getJSON() throws JSONException {
        JSONObject res = new JSONObject();
        for (Map.Entry<UUID, Chain> entry : data.entrySet()){
            res.put(entry.getKey().toString(), entry.getValue().toJSON());
        }
        return res;
    }
}
