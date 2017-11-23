package ch.ethz.inf.vs.fstreun.payapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.Transaction;

/**
 * Created by anton on 23.11.17.
 */

public class FinanceUnitTest {
    String TAG = "FinanceUnitTest";


    @Test
    public void test1() throws JSONException {
        // creating transaction
        UUID creatorUuid = UUID.randomUUID();
        List<String> involved = new ArrayList<>(2);
        involved.add("Sepp Payer");
        involved.add("John Consumer");
        double amount = 2000;
        Transaction t = new Transaction(creatorUuid, "Sepp Payer", involved, amount,
                "beer");
        String transactionJsonString = t.toString();
        System.out.println("created transaction: \n" + transactionJsonString);

        // creating group
        UUID sessionId = UUID.randomUUID();
        JSONArray parti= new JSONArray();
        //parti.put("Sepp Payer");
        JSONObject groupJson = new JSONObject();
        JSONArray transJsonArray = new JSONArray();
        groupJson.put(Group.PARTICIPANTS_KEY, parti);
        groupJson.put(Group.TRANSACTIONS_KEY, transJsonArray);
        groupJson.put(Group.SESSION_ID_KEY, sessionId.toString());
        Group testWG = new Group(groupJson);
        testWG.addTransaction(t);
        String groupJsonString = testWG.toString();
        System.out.println("created group: \n" + groupJsonString);
        System.out.println("number of participants: " + testWG.numParticipants());

        // get toPay value
        double seppToPay = testWG.toPay("Sepp Payer");
        double johnToPay = testWG.toPay("John Consumer");

        //System.out.println("John has to pay: " + johnToPay);
        System.out.println("Sepp has to pay " + seppToPay + "\n" +
                "John has to pay " + johnToPay);
        System.out.println("sumOfAllTransactions " + testWG.sumOfAllTransactions());
        assert(1 == seppToPay+johnToPay);

    }


    @Test
    public void test2() throws JSONException {
        // creating transaction json 1
        JSONObject transJson1 = new JSONObject();
        UUID creator = UUID.randomUUID();
        JSONArray involved = new JSONArray();
        involved.put("michi").put("knöppel").put("häuplting weisse blume");
        transJson1.put(Transaction.AMOUNT_KEY, 1.50);
        transJson1.put(Transaction.COMMENT_KEY, "einkauf im coop vom 32.11.17");
        transJson1.put(Transaction.CREATOR_KEY, creator);
        transJson1.put(Transaction.INVOLVED_KEY, involved);
        transJson1.put(Transaction.PAYER_KEY, "gönner");

        //todo create 2nd transaction json

        //creating group json
        JSONObject groupJson = new JSONObject();
        UUID sessionID = UUID.randomUUID();
        JSONArray transarray = new JSONArray();
    }
}
