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
    protected List<String> participants = new ArrayList<>();

    // list of all transactions in this group
    protected List<Transaction> transactions = new ArrayList<>();

    // ID of the corresponding session of this group
    private final UUID sessionID;

    public static final String PARTICIPANTS_KEY = "participants_key";
    public static final String TRANSACTIONS_KEY = "transactions_key";
    public static final String SESSION_ID_KEY = "session_id_key";

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
            System.out.println(i + "th iteration through transaction JSONArray");
            JSONObject tempJson = transJson.getJSONObject(i);
            Transaction tempTrans = new Transaction(tempJson);
            transactions.add(tempTrans);
        }

        // sessionID
        sessionID = UUID.fromString(o.getString(SESSION_ID_KEY));

        //check if all involved users are in the participants list. if not: add them
        for (Transaction t : transactions){
            for (String p : t.involved){
                if (!participants.contains(p)) this.addParticipant(p);
            }
            if(!participants.contains(t.payer)) this.addParticipant(t.payer);
        }
    }

    /**
     * adds the transaction t
     * attention: transaction should not already be in the list. no checking is performed here
     * @param t transaction to be added
     */
    public void addTransaction(Transaction t){

        //check if all involved users are already in this group. if not: add them
        for (String participant : t.involved){
            if(!participants.contains(participant)) {
                addParticipant(participant);
            } else {
                System.out.println("no participant added");
            }
        }

        //check if payer is in the group. if not: add him
        if(!participants.contains(t.payer)) addParticipant(t.payer);
        transactions.add(t);
    }

    /**
     * @param p will be added to the list of participants
     */
    public void addParticipant(String p){
        participants.add(p);
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
            transJson.put(transaction.toJson());
        }
        o.put(TRANSACTIONS_KEY, transJson);

        return o;
    }

    /**
     * @return number of participants of this group
     */
    public int numParticipants(){
        int res = 0;
        for (String p : participants){
            res++;
        }
        return res;
    }

    /**
     * this function is used to determine the value which a participant p has to pay
     * (resp. how much p will get from other users)
     * @param p a person
     * @return
     */
    public double toPay(String p){
        double result = 0;
        if (participants.contains(p)){
            for (Transaction transaction : transactions){
                if (transaction.involved.contains(p)) result += transaction.amount /
                        transaction.getNumInvolved();
                if (transaction.payer.equals(p)) result -= transaction.amount;
            }
        } //else (i.e. if p is not a participant of this group) returns 0
        return result;
    }

    /**
     * just for debugging (so far)
     * @return returns the total amount spent in the group
     */
    public double sumOfAllTransactions(){
        double sum = 0;
        for (Transaction t : transactions){
            sum += t.amount;
        }
        return sum;
    }

    /**
     * ability to return the whole object as a string
     * @return JSON as string
     */
    @Override
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
