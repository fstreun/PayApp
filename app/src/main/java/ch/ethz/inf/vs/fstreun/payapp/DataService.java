package ch.ethz.inf.vs.fstreun.payapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.datasharing.Chain;
import ch.ethz.inf.vs.fstreun.datasharing.Session;
import ch.ethz.inf.vs.fstreun.datasharing.SessionClient;
import ch.ethz.inf.vs.fstreun.datasharing.SessionImpl;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

/**
 * Created by fabio on 11/26/17.
 * This service could get create by the SynchronizeService[publish] (since that one runs always)
 * and bounded by any other activity.
 */

public class DataService extends Service {

    /*
    Binding of the service:
     */

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {

        /**
         * Return this instance of LocalService so clients can call public methods
         * @return instance of service
         */
        DataService getService(){
            return DataService.this;
        }

        /**
         * access for client manipulating one session.
         * @param sessionID of the session
         * @return session if exists.
         */
        SessionClientImpl getSessionClient(UUID sessionID) {
            // Return this instance of LocalService so clients can call public methods
            return new SessionClientImpl(sessionID);
        }


        // TODO: return access for network
        void getNetworkAccess(){

        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    /*
    actual data service:
    TODO: access for network services
     */

    // all sessions stored in the device
    private Map<UUID, SessionImpl> sessions;

    public final boolean addSession(UUID sessionID, SessionImpl session){
        if (sessions.containsKey(sessionID)){
            return false;
        }else {
            sessions.put(sessionID, session);
            return true;
        }
    }

    public final boolean removeSession(UUID sessionID){
        return !(null == sessions.remove(sessionID));
    }

    /**
     * load session from file
     * @param sessionID to be loaded (also file name of session)
     * @return session if exists and possible to be parsed
     */
    @Nullable
    private SessionImpl loadSession(UUID sessionID){
        String path = getString(R.string.path_sessions);
        String fileName = sessionID.toString();
        FileHelper fileHelper = new FileHelper(this);
        String content;
        try {
            content = fileHelper.readFromFile(path, fileName);
        } catch (FileNotFoundException e) {
            return null;
        }

        SessionImpl s = null;
        try {
            s = new SessionImpl(new JSONObject(content));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * stores session to file
     * @param session
     */
    private boolean storeSession(SessionImpl session){
        if (session == null){
            return true;
        }

        String content = null;
        try {
            content = session.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (content == null){
            return false;
        }

        FileHelper fileHelper = new FileHelper(this);
        return fileHelper.writeToFile(getString(R.string.path_sessions), session.getSessionID().toString(), content);
    }


    public final class SessionClientImpl implements SessionClient{

        private final UUID sessionID;

        private SessionClientImpl(UUID sessionID) {
            this.sessionID = sessionID;
        }

        private SessionClient getSession(){
            return sessions.get(sessionID);
        }

        /**
         * TODO: calls synchronize on the network service for this session
         */
        public void synchronize(){

        }

        @Override
        public boolean add(String content) {
            SessionClient s = getSession();
            if (s == null){
                return false;
            }else {
                return s.add(content);
            }
        }

        @Override
        public List<String> getContent() {
            SessionClient s = getSession();
            if (s == null){
                return null;
            }else {
                return s.getContent();
            }
        }

        @Override
        public List<String> getContentAfter(Map<UUID, Integer> start) {
            SessionClient s = getSession();
            if (s == null){
                return null;
            }else {
                return s.getContentAfter(start);
            }
        }

        @Override
        public Map<UUID, ? extends Chain> getContentMap() {
            SessionClient s = getSession();
            if (s == null){
                return null;
            }else {
                return s.getContentMap();
            }
        }

        @Override
        public Map<UUID, ? extends Chain> getContentMapAfter(Map<UUID, Integer> start) {
            SessionClient s = getSession();
            if (s == null){
                return null;
            }else {
                return s.getContentMapAfter(start);
            }
        }

        @Override
        public UUID getUserID() {
            SessionClient s = getSession();
            if (s == null){
                return null;
            }else {
                return s.getUserID();
            }
        }

        @Override
        public UUID getSessionID() {
            return sessionID;
        }
    }
}