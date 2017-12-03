package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import ch.ethz.inf.vs.fstreun.datasharing.Block;
import ch.ethz.inf.vs.fstreun.datasharing.Chain;
import ch.ethz.inf.vs.fstreun.datasharing.Session;
import ch.ethz.inf.vs.fstreun.datasharing.SessionClient;

import static android.content.Context.BIND_AUTO_CREATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by fabio on 12/2/17.
 */

@RunWith(AndroidJUnit4.class)
public class DataServiceTest {
    private static final String TAG = "TEST DataServiceTest";

    @ClassRule
    public static final ServiceTestRule mServiceRule = new ServiceTestRule();

    static DataService.LocalBinder binder;
    static UUID userID;


    /**
     * runs on class creation once
     */
    @BeforeClass
    public static void setUP() throws TimeoutException {
        userID = UUID.randomUUID();
        setBinder();
    }


    public static void setBinder() throws TimeoutException {
        // Create the service Intent.
        Intent serviceIntent =
                new Intent(InstrumentationRegistry.getTargetContext(),
                        DataService.class);

        // Bind the service and grab a reference to the binder.
        binder = (DataService.LocalBinder) mServiceRule.bindService(serviceIntent);
    }


    public boolean createSession(UUID sessionID, UUID userID){
        DataService service = binder.getService();
        return service.createSession(sessionID, userID);
    }

    public boolean removeSession(UUID sessionID){
        DataService service = binder.getService();
        return service.removeSession(sessionID);
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("ch.ethz.inf.vs.fstreun.payapp", appContext.getPackageName());
    }

    @Test
    public void Service_CreateSession(){
        DataService service = binder.getService();
        assertNotNull(service);

        UUID sessionID = UUID.randomUUID();

        boolean create = service.createSession(sessionID, userID);
        assertTrue(create);

        assertTrue("session ID not in the set",service.getSessionIDs().contains(sessionID));

        SessionClient session = service.getSession(sessionID);
        assertNotNull("session not returned", session);

        assertEquals("User ID of session", session.getUserID(), userID);

        boolean remove = service.removeSession(sessionID);
        assertTrue(remove);

        assertFalse("removed session still in the list", service.getSessionIDs().contains(sessionID));

        assertNull("removed session still exists", service.getSession(sessionID));

    }

    @Test
    public void ClientAccess_Access(){
        DataService.SessionClientAccess session;
        // unknown session
        session = binder.getSessionClientAccess(UUID.randomUUID());
        assertNull("unknown session", session);

        // known session
        UUID sessionID = UUID.randomUUID();
        assertTrue(createSession(sessionID, userID));
        session = binder.getSessionClientAccess(sessionID);
        assertNotNull("knwon session", session);

        assertEquals("sessione ID", sessionID, session.getSessionID());
        assertEquals("user ID", userID, session.getUserID());

        assertTrue(session.getContent().isEmpty());

        assertTrue(removeSession(sessionID));

        session = binder.getSessionClientAccess(sessionID);
        assertNull("removed session", session);
    }

    @Test
    public void ClientAccess_Data(){
        int DATANUMBER = 10;
        DataService.SessionClientAccess session;
        UUID sessionID = UUID.randomUUID();
        createSession(sessionID, userID);

        session = binder.getSessionClientAccess(sessionID);
        assertNotNull(session);

        for (int i = 0; i < DATANUMBER; i++){
            assertTrue(session.add("data " + i));
        }

        List<String> data = session.getContent();
        for (int i = 0; i < DATANUMBER; i++){
            assertTrue(data.get(i).equals("data " + i));
        }

        Map<UUID, Integer> map = new HashMap<>(1);
        map.put(userID, DATANUMBER);

        data = session.getContentAfter(map);
        assertTrue(data.isEmpty());

        for (int i = 0; i < DATANUMBER; i++){
            assertTrue(session.add("data2 " + i));
        }


        data = session.getContentAfter(map);
        for (int i = 0; i < DATANUMBER; i++){
            assertTrue(data.get(i).equals("data2 " + i));
        }

        assertTrue(removeSession(sessionID));
    }

    @Test
    public void NetworkAccess_Access() {
        DataService.SessionNetworkAccess session;

        // unknown session
        session = binder.getSessionNetworkAccess(UUID.randomUUID());
        assertNull(session);

        // known session
        UUID sessionID = UUID.randomUUID();
        createSession(sessionID, userID);
        session = binder.getSessionNetworkAccess(sessionID);

        assertNotNull(session);
        assertEquals(sessionID, session.getSessionID());

        assertTrue(removeSession(sessionID));

        session = binder.getSessionNetworkAccess(sessionID);

        assertNull(session);

    }


    @Test
    public void NetworkAccess_Data() throws JSONException {
        int DATANUMBER = 10;

        // session from which data is send
        UUID session1ID = UUID.randomUUID();
        createSession(session1ID, userID);
        DataService.SessionClientAccess session1 = binder.getSessionClientAccess(session1ID);
        DataService.SessionNetworkAccess network1 = binder.getSessionNetworkAccess(session1ID);

        // session to which data is send
        UUID session2ID = UUID.randomUUID();
        createSession(session2ID, userID);
        DataService.SessionNetworkAccess network2 = binder.getSessionNetworkAccess(session2ID);
        DataService.SessionClientAccess session2 = binder.getSessionClientAccess(session2ID);


        // add data
        for (int i = 0; i < DATANUMBER; i++){
            session1.add("data " + i);
        }

        // get data
        JSONObject data = network1.getData();

        assertTrue(session2.getContent().isEmpty());

        // receive data
        Map<UUID, Integer> start = new HashMap<>();
        network2.putData(data, start);


        // read data
        List<String> content = session2.getContent();
        for (int i = 0; i < DATANUMBER; i++){
            assertTrue(content.get(i).equals("data " + i));
        }


        // second round TODO: still fails
        // add data
        for (int i = 0; i < DATANUMBER; i++){
            session1.add("data2 " + i);
        }

        // get data
        start.put(userID, DATANUMBER);
        data = network1.getData(start);

        // receive data
        network2.putData(data, start);

        // read data
        content = session2.getContent();
        for (int i = 0; i < DATANUMBER; i++){
            assertTrue(content.get(i).equals("data2 " + i));
        }
    }

}
