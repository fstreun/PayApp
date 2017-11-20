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

public abstract class Session <C extends Chain>{

    // identifier of the session
    private final UUID sessionID;
    private static final String JSON_KEY_SESSION_ID = "session_id";

    // data in the session
    private final Map<UUID, C> data = new HashMap<>();
    private static final String JSON_KEY_DATA = "data";

    public final ChainFactory<C> chainFactory;


    /**
     * Create new basic Session
     * @param sessionID used by the session
     * @param chainFactory which defines the chain structure
     */
    public Session(UUID sessionID, ChainFactory<C> chainFactory) {
        this.sessionID = sessionID;
        this.chainFactory = chainFactory;
    }


    /**
     * Create Session of an JSON Object
     * @param object representing the Session Object
     * @param chainFactory which defines the chain structure
     * @throws JSONException if fails
     */
    public Session(JSONObject object, ChainFactory<C> chainFactory) throws JSONException {
        this.chainFactory = chainFactory;

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
                data.put(UUID.fromString(key), chainFactory.createFromJSON(map.getJSONArray(key)));
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
        for (Map.Entry<UUID, C> entry : data.entrySet()){
            map.put(entry.getKey().toString(), chainFactory.createJSON(entry.getValue()));
        }
        object.put(JSON_KEY_DATA, map);
        return object;
    }


    /**
     * accesses all the chains in the session
     * @return a copy of the map with the chains
     */
    public final Map<UUID, C> getData(){
        Map<UUID, C> res = new HashMap<>();
        for (Map.Entry<UUID, C> entry : data.entrySet()){
            res.put(entry.getKey(), chainFactory.copy(entry.getValue()));
        }
        return res;
    }


    /**
     * accesses all the sub chains in the session
     * @param start of the beginning of the chain (included)
     * @return a map of sub chains
     */
    public final Map<UUID, C> getDataAfter(Map<UUID, Integer> start){
        Map<UUID, C> res = new HashMap<>();
        for (Map.Entry<UUID, C> entry : data.entrySet()){
            UUID key = entry.getKey();
            Integer s = start.get(key);
            if (s == null){
                s = 0;
            }
            res.put(key, (C) entry.getValue().getSubChain(s));
        }
        return res;
    }

    /**
     * appends chains if possible (holes in the chain are not allowed)
     * @param chainMap chains to be appended
     * @param expected length of each chain (position of first block to be appended)
     * @return actual length of the chain before appending
     */
    public final Map<UUID, Integer> put (Map<UUID, Chain> chainMap, Map<UUID, Integer> expected){
        Map<UUID, Integer> res = new HashMap<>();
        for (Map.Entry<UUID, Chain> entry : chainMap.entrySet()){
            UUID key = entry.getKey();
            if (!data.containsKey(key)){
                data.put(entry.getKey(), chainFactory.createEmpty());
            }
            Integer actual = data.get(key).append(entry.getValue(), expected.get(key));
            res.put(key, actual);
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
    public final int put(UUID userID, Chain chain, int expected){
        if (!data.containsKey(userID)){
            data.put(userID, chainFactory.createEmpty());
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
    public final int put(UUID userID, Block block, int expected){
        if (!data.containsKey(userID)){
            data.put(userID, chainFactory.createEmpty());
        }
        return data.get(userID).append(block, expected);
    }


    /**
     * @return length of current chains
     */
    public final Map<UUID, Integer> getLength(){
        Map<UUID, Integer> res = new HashMap<>();
        for (Map.Entry<UUID, C> entry : data.entrySet()){
            res.put(entry.getKey(), entry.getValue().length());
        }
        return res;
    }


    /**
     * @return all user UUID the session has a chain of
     */
    @NonNull
    public final Set<UUID> getAllUserID(){
        return data.keySet();
    }

    /**
     * @return the UUID of the Session
     */
    public final UUID getSessionID(){
        return sessionID;
    }


}
