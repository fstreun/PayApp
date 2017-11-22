package ch.ethz.inf.vs.fstreun.finance;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by anton on 22.11.17.
 */

public class Group {
    private List<String> participants;
    private List<Transaction> transactions;

    public final String PARTICIPANTS_KEY = "participants_key";
    public final String TRANSACTIONS_KEY = "transactions_key";

    private static final String TAG = "GroupTAG";

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
