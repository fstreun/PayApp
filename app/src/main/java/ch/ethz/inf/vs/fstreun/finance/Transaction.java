package ch.ethz.inf.vs.fstreun.finance;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by anton on 22.11.17.
 */

public class Transaction {
    private final UUID creator;
    private final String payer;
    private final List<String> involved = new ArrayList<>();
    private final int amountInCents;
    private final String comment;

    //JSON keys
    public final String CREATOR_KEY = "creator_key";
    public final String PAYER_KEY = "payer_key";
    public final String INVOLVED_KEY = "involved_key";
    public final String AMOUNT_KEY = "amount_key";
    public final String COMMENT_KEY = "comment_key";

    /**
     * creates transaction from all elements as input
     * @param creator UUID of the user who created the transaction
     * @param payer name (String) of the user who payed the amount
     * @param involved list of all users (their names) involved in the transaction
     * @param amountInCents the amount that the payer paid in cents as int
     * @param comment a comment of what has been purchased
     */
    public Transaction(UUID creator, String payer, List<String> involved, int amountInCents,
                       String comment) {
        this.creator = creator;
        this.payer = payer;
        this.involved.addAll(involved);
        this.amountInCents = amountInCents;
        this.comment = comment;
    }

    /**
     * creates transaction object from JSON input
     * @param o
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
        amountInCents = (int) o.get(AMOUNT_KEY);
        comment = o.getString(COMMENT_KEY);
    }

    /**
     * outputs the transaction to a JSONObject
     * @return transaction as JSONObject
     * @throws JSONException
     */
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put(CREATOR_KEY, creator);
        o.put(PAYER_KEY, payer);
        JSONArray involvedJson = new JSONArray();
        for (String participant : involved){
            involvedJson.put(participant);
        }
        o.put(INVOLVED_KEY, involvedJson);
        o.put(COMMENT_KEY, comment);
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
}
