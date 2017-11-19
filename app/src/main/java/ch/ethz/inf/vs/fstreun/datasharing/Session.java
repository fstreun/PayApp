package ch.ethz.inf.vs.fstreun.datasharing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by fabio on 11/12/17.
 * Basic class for a session (not recommended for users)
 * using Chains of type C
 */

public abstract class Session <C extends Chain>{

    // identifier of the session
    private final UUID sessionID;
    private static final String JSON_KEY_SESSION_ID = "session_id";
    // identifier of the user which uses this object
    private final UUID userID;
    private static final String JSON_KEY_USER_ID = "user_id";

    // data in the session
    private final Map<UUID, C> data = new HashMap<>();
    private static final String JSON_KEY_DATA = "data";
    // fast access to the users chain (is also in the data)
    private final C own;
    private final ChainFactory<C> chainFactory;

    public Session(UUID sessionID, UUID userID, ChainFactory<C> chainFactory) {
        this.sessionID = sessionID;
        this.userID = userID;
        this.chainFactory = chainFactory;

        // TODO: not sure if that works!
        if (!data.containsKey(userID)) {
            data.put(userID, this.chainFactory.createEmpty());
        }
        own = data.get(userID);
    }


    public Session(JSONObject object, ChainFactory<C> chainFactory) throws JSONException {
        this.chainFactory = chainFactory;

        String errors = "Session creation with JSONObject failed due to:\n";
        boolean error = false;

        UUID sID = null;
        try {
            sID = UUID.fromString(object.getString(JSON_KEY_SESSION_ID));
        } catch (JSONException e) {
            error = true;
            errors += "key " + JSON_KEY_SESSION_ID + " missing\n";
        }


        UUID uID = null;
        try {
            uID = UUID.fromString(object.getString(JSON_KEY_USER_ID));
        } catch (JSONException e) {
            error = true;
            errors += "key " + JSON_KEY_USER_ID + " missing\n";
        }

        JSONObject map = null;
        try {
            map = object.getJSONObject(JSON_KEY_DATA);
        } catch (JSONException e) {
            error = true;
            errors += "key " + JSON_KEY_DATA + " missing\n";
        }


        if (error){
            throw new JSONException(errors);
        }

        if (map != null){
            Iterator<String> iterator = map.keys();
            while (iterator.hasNext()){
                String key = iterator.next();
                data.put(UUID.fromString(key), chainFactory.createFromJSON(map.getJSONObject(key)));
            }
        }

        sessionID = sID;

        userID = uID;

        // TODO: not sure if that works...
        if (!data.containsKey(userID)) {
            data.put(userID, this.chainFactory.createEmpty());
        }
        own = data.get(userID);
    }


    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(JSON_KEY_SESSION_ID, sessionID.toString());
        object.put(JSON_KEY_USER_ID, userID.toString());
        JSONObject map = new JSONObject();
        for (Map.Entry<UUID, C> entry : data.entrySet()){
            map.put(entry.getKey().toString(), chainFactory.createJSON(entry.getValue()));
        }
        object.put(JSON_KEY_DATA, new JSONObject(data));
        return object;
    }


    /**
     * adds block to the own chain
     * @param block to be added
     * @return true if success, else false
     */
    public final boolean add (Block block){
        return own.append(block);
    }

    /**
     * accesses all the chains in the session
     * @return a copy of the map with the chains
     */
    public final Map<UUID, C> getData(){
        return new HashMap<>(data);
    }


    /**
     * accesses all the sub chains in the session
     * @param start of the beginning of the chain (included)
     * @return a map of sub chains
     */
    public final Map<UUID, Chain> getDataAfter(Map<UUID, Integer> start){

        return null;
    }

    /**
     *
     * @return all user UUID the session has a chain of
     */
    public final Set<UUID> getAllUserID(){
        return data.keySet();
    }

    /**
     *
     * @return the UUID of the Session
     */
    public final UUID getSessionID(){
        return sessionID;
    }

    /**
     *
     * @return the user UUID of the owner of this session
     */
    public final UUID getUserID(){
        return userID;
    }
}
