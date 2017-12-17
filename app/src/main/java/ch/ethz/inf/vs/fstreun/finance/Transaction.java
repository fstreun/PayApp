package ch.ethz.inf.vs.fstreun.finance;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by anton on 22.11.17.
 */

public class Transaction implements Comparable<Transaction>{
    //Log Tag
    String TAG = "###Transaction class";

    // fields of transaction
    // todo maybe: make private and add necessary getters
    public final UUID creator;
    public final String payer;
    public final List<String> involved = new ArrayList<>();
    public final double amount;
    public final long timestamp;
    public final String comment;

    //JSON keys
    public static final String CREATOR_KEY = "creator_key";
    public static final String PAYER_KEY = "payer_key";
    public static final String INVOLVED_KEY = "involved_key";
    public static final String AMOUNT_KEY = "amount_key";
    public static final String COMMENT_KEY = "comment_key";
    public static final String TIMESTAMP_KEY = "timestamp_key";

    /**
     * creates transaction from all elements as input
     * @param creator UUID of the user who created the transaction
     * @param payer name (String) of the user who payed the amount
     * @param involved list of all users (their names) involved in the transaction
     * @param amount the amount that the payer paid as a double
     * @param timestamp System.currentTimeMillis() when creating a transaction (done in activity)
     * @param comment a comment of what has been purchased
     */
    public Transaction(UUID creator, String payer, List<String> involved, double amount,
                       long timestamp, String comment) {
        this.creator = creator;
        this.payer = payer;
        this.timestamp = timestamp;
        if(!involved.isEmpty()) {
            this.involved.addAll(involved);
        } else {
            // case no one is involved in the transaction should not happen
            // we assume that the payer bought something for himself
            this.involved.add(payer);
        }
        this.amount = amount;
        this.comment = comment;

    }

    /**
     * creates transaction object from JSON input
     * @param o is a JSONObject with the following fields and keys:
     * creator_key: UUID stored as String
     * payer_key: name of the payer as String
     * involved_key: list of participants stored as JSONArray
     * amount: the amount paid stored as double
     * comment: indication of what has been purchased stored as String
     *
     * @throws JSONException
     */
    public Transaction(JSONObject o) throws JSONException {
        creator = UUID.fromString(o.getString(CREATOR_KEY));
        payer = o.getString(PAYER_KEY);
        JSONArray involvedJson = o.getJSONArray(INVOLVED_KEY);
        int numInv = involvedJson.length();
        for(int i=0; i<numInv; i++){
           involved.add(involvedJson.getString(i));
        }
        amount = o.getDouble(AMOUNT_KEY);
        comment = o.getString(COMMENT_KEY);
        timestamp = o.getLong(TIMESTAMP_KEY);
    }

    public Transaction reverse(long timestamp){
        String newComment = "DELETE TRANSACTION: " + comment;
        Transaction result = new Transaction(creator, payer, involved, -amount, timestamp, newComment);
        return result;
    }

    /**
     * getter function for amount
     * @return
     */
    public double getAmount() {
        return amount;
    }

    /**
     * getter function for comment
     * @return
     */
    public String getComment() {
        return comment;
    }

    /**
     * getter function for payer
     * @return
     */
    public String getPayer() {
        return payer;
    }

    /**
     * getter function for involved
     * @return
     */
    public List<String> getInvolved() {
        return involved;
    }

    /**
     * @return number of people involved in a transaction (NOT including the payer)
     */
    public int getNumInvolved (){
        int result = 0;
        for(String p : involved){
            result ++;
        }
        assert (result > 0);
        return result;
    }


    /**
     * outputs the transaction to a JSONObject
     * @return transaction as JSONObject
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put(CREATOR_KEY, creator.toString());
        o.put(PAYER_KEY, payer);
        JSONArray involvedJson = new JSONArray();
        for (String participant : involved){
            involvedJson.put(participant);
        }
        o.put(INVOLVED_KEY, involvedJson);
        o.put(AMOUNT_KEY, amount);
        o.put(COMMENT_KEY, comment);
        o.put(TIMESTAMP_KEY, timestamp);
        return o;
    }

    /**
     * @return the output from toJson() as a string
     */
    @Override
    public String toString() {
        try {
            return toJson().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * global method to get an amount of money as a string of the form 49.99
     * @param x
     * @return
     */
    public static String doubleToString(double x){
        return String.format("%1$.2f", x);
    }

    /**
     * compare function
     * @param o
     * @return -1 if this was created before o,
     * 0 if in the same milisecond
     * 1 if this created after o
     */
    @Override
    public int compareTo(@NonNull Transaction o) {
        return Long.valueOf(timestamp).compareTo(Long.valueOf(o.timestamp));
    }

    /**
     * case insensitive
     * @param p
     * @return whether a string is contained in this involved list
     */
    public boolean involvedContains(String p) {
        for(String i : involved){
            if(caseInsensitiveEquals(i, p)) return true;
        }
        return false;
    }

    public static boolean caseInsensitiveEquals(String p1, String p2) {
        if(String.CASE_INSENSITIVE_ORDER.compare(p1, p2) == 0) return true;
        else return false;
    }
}
