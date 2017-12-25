package ch.ethz.inf.vs.fstreun.payapp.filemanager;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.datasharing.SessionClientInterface;
import ch.ethz.inf.vs.fstreun.datasharing.SessionClient;
import ch.ethz.inf.vs.fstreun.payapp.R;

/**
 * Created by fabio on 11/26/17.
 * This service could get create by the SynchronizeService[publish] (since that one runs always)
 * and bounded by any other activity.
 *
 * Offers access to manipulation of one session (by client and network),
 * and creation/removing of sessions.
 *
 * Stores sessions in file.
 */

public class DataService extends Service {

    private static final String TAG = "DataService";
    // all sessions on the device
    Set<UUID> sessionIDs = new HashSet<>();
    private static final String IDSplitter = "##";

    FileHelper fileHelper;

    /**
     * called when service is not yet running
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");

        fileHelper = new FileHelper(this);

        sessionIDs.addAll(loadSessionIDs());
    }

    private Set<UUID> loadSessionIDs(){
        Set<UUID> sessionIDs = new HashSet<>();
        // load all session ID on this device
        String fileString;
        try {
            fileString = fileHelper.readFromFile(getString(R.string.sessions_directory), getString(R.string.sessionIDs_file));
        } catch (FileNotFoundException e) {
            return sessionIDs;
        }


        Log.d(TAG, fileString);

        String[] idStrings = fileString.split(IDSplitter);
        for (String s : idStrings){
            // remove all white spaces
            s = s.replaceAll("\\s+","");
            Log.d(TAG, s);
            if (s != null && !s.isEmpty()){
                UUID id = UUID.fromString(s);
                sessionIDs.add(id);
            }
        }
        return sessionIDs;
    }

    /**
     * called when service is ending
     * not really called anytime!
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
    }

    private void storeSessionIDs(Set<UUID> sessionIDs){
        // store all session ID on this device

        StringBuilder file = new StringBuilder();
        for (UUID id : sessionIDs){
            file.append(IDSplitter).append(id.toString()).append(IDSplitter);
        }

        fileHelper.writeToFile(getString(R.string.sessions_directory), getString(R.string.sessionIDs_file), file.toString() );
    }

    /*
    Binding of the service:
     */

    // Binder given to clients
    private final IBinder mBinder = new DataServiceBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class DataServiceBinder extends Binder {

        /**
         * Return this instance of LocalService so clients can call public methods
         *
         * @return instance of service
         */
        public DataService getService() {
            return DataService.this;
        }

        /**
         * access for network manipulating one session
         *
         * @param sessionID of the session to be manipulated
         * @return access interface to session if exists, else null
         */
        public SessionNetworkAccess getSessionNetworkAccess(UUID sessionID) {
            if (sessionIDs.contains(sessionID)) {
                return new SessionNetworkAccess(sessionID);
            } else {
                return null;
            }
        }

        /**
         * access for client manipulating one session.
         *
         * @param sessionID of the session
         * @return access interface to session if exists, else null
         */
        public SessionClientAccess getSessionClientAccess(UUID sessionID) {
            if (sessionIDs.contains(sessionID)) {
                return new SessionClientAccess(sessionID);
            } else {
                return null;
            }
        }


    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    /*
    actual data service:
     */

    // all sessions loaded from the file (cached)
    private Map<UUID, SessionClient> loadedSessions = new HashMap<>();

    public final UUID getUserID() {

        String stringID;
        UUID userID = null;
        try {
            stringID = fileHelper.readFromFile(null, "deviceID");
            // remove all whitespaces
            stringID = stringID.replaceAll("\\s+", "");
            userID = UUID.fromString(stringID);
        } catch (FileNotFoundException e) {
            userID = UUID.randomUUID();
            fileHelper.writeToFile(null, "deviceID", userID.toString());
        }

        return userID;
    }

    public final boolean createSession(UUID sessionID, UUID userID) {
        boolean add = sessionIDs.add(sessionID);
        if (add){
            storeSessionIDs(sessionIDs);
            // not yet in the list
            SessionClient session = new SessionClient(sessionID, userID);
            loadedSessions.put(sessionID, session);
            storeSession(session);
        }
        return add;
    }

    public final boolean removeSession(UUID sessionID) {
        boolean remove = sessionIDs.remove(sessionID);
        if (remove){
            // session is in the list
            loadedSessions.remove(sessionID);
            removeSessionFile(sessionID);
        }
        return remove;
    }

    public final SessionClient getSession(UUID sessionID){
        if (!sessionIDs.contains(sessionID)){
            return null;
        }

        if (loadedSessions.containsKey(sessionID)){
            return loadedSessions.get(sessionID);
        }

        SessionClient session = loadSession(sessionID);
        if (session != null){
            loadedSessions.put(sessionID, session);
            return session;
        }

        return null;
    }

    public final Set<UUID> getSessionIDs(){
        return new HashSet<>(sessionIDs);
    }

    /**
     * load session from file
     *
     * @param sessionID to be loaded (also file name of session)
     * @return session if exists and possible to be parsed
     */
    @Nullable
    private SessionClient loadSession(UUID sessionID) {
        String path = getString(R.string.path_sessions);
        String fileName = sessionID.toString();
        String content;
        try {
            content = fileHelper.readFromFile(path, fileName);
        } catch (FileNotFoundException e) {
            return null;
        }

        SessionClient s = null;
        try {
            s = new SessionClient(new JSONObject(content));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * stores session to file
     *
     * @param session
     */
    private boolean storeSession(SessionClient session) {
        if (session == null) {
            return true;
        }

        String content = null;
        try {
            content = session.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (content == null) {
            return false;
        }

        return fileHelper.writeToFile(getString(R.string.path_sessions), session.getSessionID().toString(), content);
    }

    private boolean removeSessionFile(UUID sessionID){
        return fileHelper.removeFile(getString(R.string.path_sessions), sessionID.toString());
    }






    /**
     * Access to one Session for one Client
     */
    public final class SessionClientAccess {

        private final UUID sessionID;

        private SessionClientAccess(UUID sessionID) {
            this.sessionID = sessionID;
        }

        /**
         * returns session with sessionID
         * @return null if not exists
         */
        @Nullable
        private SessionClient getSession() {
            return DataService.this.getSession(sessionID);
        }

        public boolean add(String content) {
            SessionClient s = getSession();
            if (s == null) {
                return false;
            } else {
                boolean success = s.add(content);
                storeSession(s);
                return success;
            }
        }

        public List<String> getContent() {
            SessionClientInterface s = getSession();
            if (s == null) {
                return null;
            } else {
                return s.getContent();
            }
        }

        public List<String> getContentAfter(Map<UUID, Integer> start) {
            SessionClientInterface s = getSession();
            if (s == null) {
                return null;
            } else {
                return s.getContentAfter(start);
            }
        }

        public UUID getUserID() {
            SessionClientInterface s = getSession();
            if (s == null) {
                return null;
            } else {
                return s.getUserID();
            }
        }

        public UUID getSessionID() {
            return sessionID;
        }

        public boolean removeSession(){
            return DataService.this.removeSession(sessionID);
        }
    }


    public final class SessionNetworkAccess {

        private final UUID sessionID;

        private SessionNetworkAccess(UUID sessionID) {
            this.sessionID = sessionID;
        }

        /**
         * session with sessionID
         * @return null if not exists
         */
        @Nullable
        private SessionClient getSession() {
            return DataService.this.getSession(sessionID);
        }

        public UUID getSessionID(){
            return sessionID;
        }

        @Nullable
        public JSONObject getData() {
            JSONObject res;
            try {
                res = getSession().getJSON();
            } catch (JSONException e) {
                return null;
            }
            return res;
        }

        @Nullable
        public JSONObject getData(Map<UUID, Integer> after) {
            JSONObject res = null;
            try {
                res = getSession().getJSON(after);
            } catch (JSONException e) {
                return null;
            }
            return res;
        }

        public Map<UUID, Integer> getLength(){
            return getSession().getLength();
        }

        @Nullable
        public Map<UUID, Integer> putData(JSONObject mapData, Map<UUID, Integer> expected){
            SessionClient s = getSession();
            if (s != null) {
                try {
                    Map<UUID, Integer> result = s.appendJSON(mapData, expected);
                    storeSession(s);
                    return result;
                } catch (JSONException e) {
                    return null;
                }
            }else {
                return null;
            }
        }

    }
}
