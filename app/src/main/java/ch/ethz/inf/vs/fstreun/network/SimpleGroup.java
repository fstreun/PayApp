package ch.ethz.inf.vs.fstreun.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by fabio on 12/5/17.
 */

public class SimpleGroup {

    final static String KEY_GROUPID = "groupID";
    public final UUID groupID;
    final static String KEY_GROUPNAME = "groupName";
    public final String groupName;
    final static String KEY_SESSIONID = "sessionID";
    public final UUID sessionID;

    public SimpleGroup(UUID groupID, String groupName, UUID sessionID) {
        this.groupID = groupID;
        this.groupName = groupName;
        this.sessionID = sessionID;
    }

    public SimpleGroup(JSONObject object) throws JSONException {
        groupID = UUID.fromString(object.getString(KEY_GROUPID));
        sessionID = UUID.fromString(object.getString(KEY_SESSIONID));
        groupName = object.getString(KEY_GROUPNAME);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(KEY_GROUPID, groupID);
        object.put(KEY_SESSIONID, sessionID);
        object.put(KEY_GROUPNAME, groupName);
        return object;
    }
}
