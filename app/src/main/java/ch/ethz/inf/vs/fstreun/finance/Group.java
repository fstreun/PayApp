package ch.ethz.inf.vs.fstreun.finance;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by anton on 22.11.17.
 */

public class Group {

    // all participants that are involved in or have paid a transaction
    private List<String> participants = new ArrayList<>();

    // list of all transactions in this group
    private List<Transaction> transactions;

    // ID of the corresponding session of this group
    private final UUID sessionID;

    public final String PARTICIPANTS_KEY = "participants_key";
    public final String TRANSACTIONS_KEY = "transactions_key";
    public final String SESSION_ID_KEY = "session_id_key";

    private static final String TAG = "GroupTAG";

    /**
     * creates a group
     * @param o is a JSONObject containing:
     *          JSONArray of Strings: participants as names
     *          JSONArray of JSONObjects: transaction
     *          String: containing the UUID
     * @throws JSONException
     */
    public Group (JSONObject o) throws JSONException {
        //participants
        JSONArray parJson = o.getJSONArray(PARTICIPANTS_KEY);
        int numPart = parJson.length();
        for (int i=0; i<numPart; i++) {
            participants.add(parJson.getString(i));
        }

        //transactions
        JSONArray transJson = o.getJSONArray(TRANSACTIONS_KEY);
        int numTrans = transJson.length();
        for (int i=0; i<numTrans; i++){
            JSONObject tempJson = transJson.getJSONObject(i);
            transactions.add(new Transaction(tempJson));
        }

        // sessionID
        sessionID = UUID.fromString(o.getString(SESSION_ID_KEY));

    }

    /**
     * adds the transaction t
     * attention: transaction should not already be in the list. no checking is performed here
     * @param t transaction to be added
     */
    public void add(Transaction t){
        transactions.add(t);
    }

    /**
     * @return JSONObject of all participants and all transactions of this Group*
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();

        //participants
        JSONArray parJson = new JSONArray();
        for (String participant : participants) {
            parJson.put(participant);
        }
        o.put(PARTICIPANTS_KEY, parJson);

        //transactions
        JSONArray transJson = new JSONArray();
        for (Transaction transaction : transactions){
            transJson.put(transaction);
        }
        o.put(TRANSACTIONS_KEY, transJson);

        return o;
    }

    /**
     * ability to return the whole object as a string
     * @return JSON as string
     */
    public String toString(){
        try {
            return toJson().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "parsing this object to string didn't work");
        return "";
    }
}
