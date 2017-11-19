package ch.ethz.inf.vs.fstreun.payapp;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.datasharing.Block;
import ch.ethz.inf.vs.fstreun.datasharing.ChainImpl;
import ch.ethz.inf.vs.fstreun.datasharing.Session;
import ch.ethz.inf.vs.fstreun.datasharing.SessionClient;
import ch.ethz.inf.vs.fstreun.datasharing.SessionImpl;

import static org.junit.Assert.*;


/**
 * Created by fabio on 11/19/17.
 *
 */

public class SessionUnitTest {


    @Test
    public void session_creation() throws Exception {
        UUID sessionID = UUID.randomUUID();
        UUID userID = UUID.randomUUID();
        Session s = new SessionImpl(sessionID, userID);
        assertEquals("session ID expected", sessionID, s.getSessionID());
        assertEquals("user ID expected", userID, s.getUserID());
    }

    @Test
    public void session_json() throws Exception {
        UUID sessionID = UUID.randomUUID();
        UUID userID = UUID.randomUUID();
        Session<ChainImpl> s = new SessionImpl(sessionID, userID);

        String content = "this is content";
        s.add(Block.createWithContent(content));

        JSONObject j = s.toJSON();

        s = new SessionImpl(j);

        assertEquals("session ID expected", sessionID, s.getSessionID());
        assertEquals("user ID expected", userID, s.getUserID());


        String content_j = s.getData().get(userID).get(0).getContent();
        assertEquals("content compared", content, content_j);
    }


    @Test
    public void sessionClient_creation() throws Exception {
        UUID sessionID = UUID.randomUUID();
        UUID userID = UUID.randomUUID();
        SessionImpl session = new SessionImpl(sessionID, userID);
        SessionClient client = session;

        String content = "this is content";
        client.add(content);

        JSONObject jSession = session.toJSON();
        session = new SessionImpl(jSession);
        client = session;

        String jContent = client.getContent().get(0);
        assertEquals("content compared", content, jContent);
    }

    @Test
    public void sessionClient_multiple() throws Exception {
        UUID sessionID = UUID.randomUUID();
        UUID userID = UUID.randomUUID();
        SessionImpl session = new SessionImpl(sessionID, userID);
        SessionClient client = session;

        String content = "this is content";
        String content2 = "2nd";

        client.add(content);
        client.add(content2);

        JSONObject j = session.toJSON();

        session = new SessionImpl(j);

        assertEquals("content compared", content, client.getContent().get(0));
        assertEquals("content compared", content2, client.getContent().get(1));

    }


}
