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
    private List<Transaction> transactions = new ArrayList<>();

    // ID of the corresponding session of this group
    private final UUID sessionID;

    // default Participant owner of the device where this group is stored
    private String deviceOwner;

    public static final String PARTICIPANTS_KEY = "participants_key";
    public static final String TRANSACTIONS_KEY = "transactions_key";
    public static final String SESSION_ID_KEY = "session_id_key";
    public static final String DEVICE_OWNER_KEY = "device_owner_key";

    private static final String TAG = "###GroupTAG";

    /**
     * creates an empty group from sessionID
     * @param sessionID
     */
    public Group (UUID sessionID){
        this.sessionID = sessionID;

        //empty participant list and empty transaction list and no default participant
        //already done
    }

    /**
     * creates a group
     * @param o is a JSONObject containing:
     *          JSONArray of Strings: participants as names
     *          JSONArray of JSONObjects: transaction
     *          String: containing the UUID
     *          String: device owner
     * @throws JSONException
     */
    public Group (JSONObject o) throws JSONException {
        //participants
        // this part is actually redundant because the participants are added automatically
        JSONArray parJson = o.getJSONArray(PARTICIPANTS_KEY);
        int numPart = parJson.length();
        for (int i=0; i<numPart; i++) {
            addParticipant(parJson.getString(i));
        }

        //transactions
        JSONArray transArray = o.getJSONArray(TRANSACTIONS_KEY);
        int numTrans = transArray.length();
        for (int i=0; i<numTrans; i++){
            System.out.println(i + "th iteration through transaction JSONArray");
            JSONObject tempJson = transArray.getJSONObject(i);
            Transaction tempTrans = new Transaction(tempJson);
            addTransaction(tempTrans);
        }

        // sessionID
        sessionID = UUID.fromString(o.getString(SESSION_ID_KEY));

        // device owner
        try {
            deviceOwner = o.getString(DEVICE_OWNER_KEY);
            addParticipant(deviceOwner);
        } catch (JSONException e){
            deviceOwner = null;
        }

        //check if all involved users are in the participants list. if not: add them
        for (Transaction t : transactions){
            for (String p : t.involved){
                if (!participants.contains(p)) this.addParticipant(p);
            }
            if(!participants.contains(t.payer)) this.addParticipant(t.payer);
        }
    }

    public void removeTransaction(Transaction t){
        addTransaction(t.reverse(System.currentTimeMillis()));
    }

    /**
     * getter function for participants
     * @return participants as List<String>
     */
    public List<String> getParticipants() {
        return participants;
    }

    /**
     * getter function for transactions
     * @return transactions as List<Transaction>
     */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    /**
     * getter function for default participant
     * @return defaultparticipant as String, null if not defined
     */
    public String getDeviceOwner() {
        return deviceOwner;
    }

    /**
     * setter function for default participant
     * @param deviceOwner
     */
    public void setDeviceOwner(String deviceOwner) {
        this.deviceOwner = deviceOwner;
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
     * this function is actually unnecessary because when a transaction is added, all involved
     * participants are added automatically
     * @param p will be added to the list of participants
     */
    public void addParticipant(String p){
        // if p not already in participants add him to participants
        if (!participants.contains(p)) participants.add(p);
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

        //session ID
        o.put(SESSION_ID_KEY, sessionID.toString());

        //device owner
        o.put(DEVICE_OWNER_KEY, deviceOwner);

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
     * @param p a participant
     * @return
     */
    public double toPay(String p){
        double result = 0;
        if (participants.contains(p)){
            for (Transaction t : transactions){
                if (t.involved.contains(p)) result += t.amount /
                        t.getNumInvolved();
                if (t.payer.equals(p)) result -= t.amount;
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
