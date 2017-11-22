package ch.ethz.inf.vs.fstreun.payapp;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.datasharing.Block;
import ch.ethz.inf.vs.fstreun.datasharing.Chain;
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
        Session<ChainImpl> session = new Session<>(sessionID, new ChainImpl());
        assertEquals("session ID expected", sessionID, session.getSessionID());
    }


    @Test
    public void session_json() throws Exception {
        UUID sessionID = UUID.randomUUID();
        Session<ChainImpl> session = new Session<>(sessionID, new ChainImpl());
        String s = session.toJSON().toString();

        session = new Session<ChainImpl>(new JSONObject(s), new ChainImpl());
        assertEquals("session ID expected", sessionID, session.getSessionID());
    }

    @Test
    public void session_emptyData() throws Exception {
        UUID sessionID = UUID.randomUUID();
        Session<ChainImpl> session = new Session<>(sessionID, new ChainImpl());

        assert session.put(new HashMap<UUID, Chain>(0), new HashMap<UUID, Integer>(0)).isEmpty();

        UUID user1ID = UUID.randomUUID();
        assert 0 == session.put(user1ID, new ChainImpl().createEmpty(), 0);
        assert 0 == session.put(user1ID, new ChainImpl().createEmpty(), 0);

        assertTrue("no data expected", ( null == session.getLength().get(user1ID) || 0 == session.getLength().get(user1ID)));

        String s = session.toJSON().toString();

        session = new Session<ChainImpl>(new JSONObject(s), new ChainImpl());

        assertEquals("session ID expected", sessionID, session.getSessionID());
        assertTrue("no data expected", ( null == session.getLength().get(user1ID) || 0 == session.getLength().get(user1ID)));
    }

    @Test
    public void session_data() throws Exception {
        UUID sessionID = UUID.randomUUID();
        Session<ChainImpl> session = new Session<>(sessionID, new ChainImpl());

        UUID user1ID = UUID.randomUUID();
        Chain chain1 = session.chainFactory.createEmpty();
        chain1.append(Block.createWithContent("first"), 0);
        chain1.append(Block.createWithContent("second"), 1);


        HashMap<UUID, Chain> data = new HashMap<>(2);
        data.put(user1ID, chain1);
        HashMap<UUID, Integer> expected = new HashMap<>(2);
        expected.put(user1ID, 0);
        session.put(data, expected);

        session.put(user1ID, new ChainImpl().createEmpty(), 2);
        session.put(user1ID, Block.createWithContent("third"), 2);
        session.put(user1ID, Block.createWithContent("fourth"),3);

        ChainImpl chain = session.chainFactory.createEmpty();
        chain.append(Block.createWithContent("fifth"), 0);
        chain.append(Block.createWithContent("sixth"), 1);
        session.put(user1ID, chain, 4);

        assertEquals("data size expected", new Integer(6), session.getLength().get(user1ID));

        String s = session.toJSON().toString();

        session = new Session<ChainImpl>(new JSONObject(s), new ChainImpl());

        assertEquals("session ID expected", sessionID, session.getSessionID());
        assertEquals("data size expected", new Integer(6), session.getLength().get(user1ID));
    }



    @Test
    public void sessionImpl_creation() throws Exception {
        UUID sessionID = UUID.randomUUID();
        UUID userID = UUID.randomUUID();
        SessionImpl s = new SessionImpl(sessionID, userID);
        assertEquals("session ID expected", sessionID, s.getSessionID());
        assertEquals("user ID expected", userID, s.getUserID());
    }

    @Test
    public void sessionImpl_json() throws Exception {
        UUID sessionID = UUID.randomUUID();
        UUID userID = UUID.randomUUID();
        SessionImpl session = new SessionImpl(sessionID, userID);

        String content = "this is content";
        session.add(content);

        String s = session.toJSON().toString();

        session = new SessionImpl(new JSONObject(s));

        assertEquals("session ID expected", sessionID, session.getSessionID());
        assertEquals("user ID expected", userID, session.getUserID());


        String content_j = session.getData().get(userID).get(0).getContent();
        assertEquals("content compared", content, content_j);
    }


    @Test
    public void sessionClient_json() throws Exception {
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
    public void sessionClient_data() throws Exception {
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

        assertEquals("content 1 compared", content, client.getContent().get(0));
        assertEquals("content 2 compared", content2, client.getContent().get(1));

    }


}
