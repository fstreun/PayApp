package ch.ethz.inf.vs.fstreun.payapp;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.datasharing.Block;
import ch.ethz.inf.vs.fstreun.datasharing.ChainInterface;
import ch.ethz.inf.vs.fstreun.datasharing.Chain;
import ch.ethz.inf.vs.fstreun.datasharing.Session;
import ch.ethz.inf.vs.fstreun.datasharing.SessionClientInterface;
import ch.ethz.inf.vs.fstreun.datasharing.SessionImpl;

import static org.junit.Assert.*;


/**
 * Created by fabio on 11/19/17.
 *
 */

public class SessionUnitTest {


    @Test
    public void session_test() throws Exception {
        UUID sessionID = UUID.randomUUID();
        Session session = new Session(sessionID);
        assertEquals("session ID expected", sessionID, session.getSessionID());

        System.out.println(session);

        UUID id = UUID.randomUUID();
        Chain chain = new Chain();
        chain.append(Block.createWithContent("content"),0);
        System.out.println(chain);

        session.putChain(id, chain, 0);

        System.out.println(session);


    }

    @Test
    public void session_creation() throws Exception {
        UUID sessionID = UUID.randomUUID();
        Session session = new Session(sessionID);
        assertEquals("session ID expected", sessionID, session.getSessionID());
    }


    @Test
    public void session_json() throws Exception {
        UUID sessionID = UUID.randomUUID();
        Session session = new Session(sessionID);
        String s = session.toJSON().toString();

        session = new Session(new JSONObject(s));
        assertEquals("session ID expected", sessionID, session.getSessionID());
    }

    @Test
    public void session_emptyData() throws Exception {
        UUID sessionID = UUID.randomUUID();
        Session session = new Session(sessionID);

        assert session.putData(new HashMap<UUID, Chain>(0), new HashMap<UUID, Integer>(0)).isEmpty();

        UUID user1ID = UUID.randomUUID();
        assert 0 == session.putChain(user1ID, new Chain(), 0);
        assert 0 == session.putChain(user1ID, new Chain(), 0);

        assertTrue("no listData expected", ( null == session.getLength().get(user1ID) || 0 == session.getLength().get(user1ID)));

        String s = session.toJSON().toString();

        session = new Session(new JSONObject(s));

        assertEquals("session ID expected", sessionID, session.getSessionID());
        assertTrue("no listData expected", ( null == session.getLength().get(user1ID) || 0 == session.getLength().get(user1ID)));
    }

    @Test
    public void session_data() throws Exception {
        UUID sessionID = UUID.randomUUID();
        Session session = new Session(sessionID);

        UUID user1ID = UUID.randomUUID();
        Chain chain1 = new Chain();
        Integer i = chain1.append(Block.createWithContent("first"), 0);
        assertEquals("listData size expected", new Integer(0), i);
        i = chain1.append(Block.createWithContent("second"), 1);
        assertEquals("listData size expected", new Integer(1), i);


        HashMap<UUID, Chain> data = new HashMap<>(2);
        data.put(user1ID, chain1);
        HashMap<UUID, Integer> expected = new HashMap<>(2);
        expected.put(user1ID, 0);
        session.putData(data, expected);

        session.putChain(user1ID, new Chain(), 2);
        session.putBlock(user1ID, Block.createWithContent("third"), 2);
        session.putBlock(user1ID, Block.createWithContent("fourth"),3);

        Chain chain = new Chain();
        chain.append(Block.createWithContent("fifth"), 0);
        chain.append(Block.createWithContent("sixth"), 1);
        session.putChain(user1ID, chain, 4);

        assertEquals("listData size expected", new Integer(6), session.getLength().get(user1ID));

        String s = session.toJSON().toString();

        session = new Session(new JSONObject(s));

        assertEquals("session ID expected", sessionID, session.getSessionID());
        assertEquals("listData size expected", new Integer(6), session.getLength().get(user1ID));
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


        String content_j = session.getData().get(userID).getBlock(0).getContent();
        assertEquals("content compared", content, content_j);
    }


    @Test
    public void sessionClient_json() throws Exception {
        UUID sessionID = UUID.randomUUID();
        UUID userID = UUID.randomUUID();
        SessionImpl session = new SessionImpl(sessionID, userID);
        SessionClientInterface client = session;

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
        SessionClientInterface client = session;

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
