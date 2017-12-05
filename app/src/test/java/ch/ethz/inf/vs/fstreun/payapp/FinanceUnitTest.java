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

import static org.junit.Assert.*;

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
                timestamp, "beer");
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
        groupJson.put(Group.DEVICE_OWNER_KEY, "Sepp");
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
        assertEquals("some Message", 1.0, seppToPay+johnToPay, 0.001);

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
        transJson1.put(Transaction.CREATOR_KEY, creator.toString());
        transJson1.put(Transaction.INVOLVED_KEY, involved);
        transJson1.put(Transaction.PAYER_KEY, "gönner");

        //getting amount value:

        //create 2nd transaction json
        JSONObject transJson2 = new JSONObject();
        creator = UUID.randomUUID();
        involved = new JSONArray();
        involved.put("richi").put("knöppel");
        transJson2.put(Transaction.AMOUNT_KEY, 68.168);
        transJson2.put(Transaction.COMMENT_KEY, "stromrechnung");
        transJson2.put(Transaction.CREATOR_KEY, creator.toString());
        transJson2.put(Transaction.INVOLVED_KEY, involved);
        transJson2.put(Transaction.PAYER_KEY, "richi");

        //creating group json
        JSONObject groupJson = new JSONObject();
        UUID sessionID = UUID.randomUUID();
        JSONArray transArray = new JSONArray();
        transArray.put(transJson1).put(transJson2);
        JSONArray partiArray = new JSONArray();
        groupJson.put(Group.TRANSACTIONS_KEY, transArray);
        groupJson.put(Group.SESSION_ID_KEY, sessionID.toString());
        groupJson.put(Group.PARTICIPANTS_KEY, partiArray);
        groupJson.put(Group.DEVICE_OWNER_KEY, "richi");

        //creating group
        Group awesomeWG = new Group(groupJson);

        /* works :)
        //output the amount of the transactions
        System.out.println("the amount of the transactions of the group" +
                awesomeWG.getTransactions().get(0).getAmount() +
                " and " + awesomeWG.getTransactions().get(1).getAmount());
        */

        // iterating over participants and printing emerson's toPay value to console
        List<Double> members = new ArrayList<Double>();
        int i = 0;
        for(String p : awesomeWG.getParticipants()){
            members.add(i, awesomeWG.toPay(p));
            System.out.println(p + " has to pay " + members.get(i));
            i++;
        }
    }


    //convert a transaction to a JSONObject and back
    @Test
    public void test3() throws JSONException {
        // creating transaction
        UUID creatorUuid = UUID.randomUUID();
        List<String> involved = new ArrayList<>(2);
        involved.add("Sepp Payer");
        involved.add("John Consumer");
        double amount = 2000;
        Transaction t = new Transaction(creatorUuid, "Sepp Payer", involved, amount,
                timestamp, "beer");
        String transactionJsonString = t.toString();
        System.out.println("created transaction in usual way: \n" + transactionJsonString);

        //////////////////////////////// IMPORTANT PART /////////////////////////////////////
        //reading Json object into transaction object
        Transaction transaction = new Transaction(t.toJson());
        transactionJsonString = transaction.toString();
        System.out.println("created transaction from t.toJson(): \n" + transactionJsonString);
        /////////////////////////////////////////////////////////////////////////////////////

        // creating group
        UUID sessionId = UUID.randomUUID();
        JSONArray parti= new JSONArray();
        //parti.put("Sepp Payer");
        JSONObject groupJson = new JSONObject();
        JSONArray transJsonArray = new JSONArray();
        groupJson.put(Group.PARTICIPANTS_KEY, parti);
        groupJson.put(Group.TRANSACTIONS_KEY, transJsonArray);
        groupJson.put(Group.SESSION_ID_KEY, sessionId.toString());
        groupJson.put(Group.DEVICE_OWNER_KEY, "richi");
        Group testWG = new Group(groupJson);
        testWG.addTransaction(t);
        String groupJsonString = testWG.toString();
        System.out.println("created group in the normal way: \n" + groupJsonString);
        System.out.println("number of participants: " + testWG.numParticipants());

        //////////////////////////////// IMPORTANT PART /////////////////////////////////////
        //creating group object from jsonobject
        Group group = new Group(testWG.toJson());
        groupJsonString = group.toString();
        System.out.println("created group from testWG.toJson(): \n" + groupJsonString);
        /////////////////////////////////////////////////////////////////////////////////////

        // get toPay value
        double seppToPay = testWG.toPay("Sepp Payer");
        double johnToPay = testWG.toPay("John Consumer");

        //System.out.println("John has to pay: " + johnToPay);
        System.out.println("Sepp has to pay " + seppToPay + "\n" +
                "John has to pay " + johnToPay);
        System.out.println("sumOfAllTransactions " + testWG.sumOfAllTransactions());
        assertEquals("some Message", 1.0, seppToPay+johnToPay, 0.001);

    }

    //convert a transaction to a String and back
    @Test
    public void test4() throws JSONException {
        // creating transaction
        UUID creatorUuid = UUID.randomUUID();
        List<String> involved = new ArrayList<>(2);
        involved.add("Sepp Payer");
        involved.add("John Consumer");
        double amount = 2000.0;
        Transaction t = new Transaction(creatorUuid, "Sepp Payer", involved, amount,
                timestamp, "beer");
        String transactionJsonString = t.toString();
        System.out.println("created transaction in usual way: \n" + transactionJsonString);

        //////////////////////////////// IMPORTANT PART /////////////////////////////////////
        //reading Json object into transaction object
        Transaction transaction = new Transaction(new JSONObject(transactionJsonString));
        transactionJsonString = transaction.toString();
        System.out.println("created transaction from t.toJson(): \n" + transactionJsonString);
        /////////////////////////////////////////////////////////////////////////////////////

        // creating group
        UUID sessionId = UUID.randomUUID();
        JSONArray parti= new JSONArray();
        //parti.put("Sepp Payer");
        JSONObject groupJson = new JSONObject();
        JSONArray transJsonArray = new JSONArray();
        groupJson.put(Group.PARTICIPANTS_KEY, parti);
        groupJson.put(Group.TRANSACTIONS_KEY, transJsonArray);
        groupJson.put(Group.SESSION_ID_KEY, sessionId.toString());
        groupJson.put(Group.DEVICE_OWNER_KEY, "richi");
        Group testWG = new Group(groupJson);
        testWG.addTransaction(t);
        String groupJsonString = testWG.toString();
        System.out.println("created group in the normal way: \n" + groupJsonString);
        System.out.println("number of participants: " + testWG.numParticipants());

        //////////////////////////////// IMPORTANT PART /////////////////////////////////////
        //creating group object from jsonobject
        Group group = new Group(new JSONObject(testWG.toString()));
        groupJsonString = group.toString();
        System.out.println("created group from testWG.toJson(): \n" + groupJsonString);
        /////////////////////////////////////////////////////////////////////////////////////

        // get toPay value
        double seppToPay = testWG.toPay("Sepp Payer");
        double johnToPay = testWG.toPay("John Consumer");

        //System.out.println("John has to pay: " + johnToPay);
        System.out.println("Sepp has to pay " + seppToPay + "\n" +
                "John has to pay " + johnToPay);
        System.out.println("sumOfAllTransactions " + testWG.sumOfAllTransactions());
        assertEquals("some Message", 1.0, seppToPay+johnToPay, 0.001);


    }

    // reverse transaction
    @Test
    public void test5(){
        // creating transaction1
        UUID creatorUuid = UUID.randomUUID();
        List<String> involved = new ArrayList<>(2);
        involved.add("Sepp Payer");
        involved.add("John Consumer");
        double amount = 2000.0;
        Transaction t = new Transaction(creatorUuid, "Sepp Payer", involved, amount,
                timestamp, "beer");

        // creating transaction2
        Transaction tReverse = t.reverse();

        //creating group
        Group g = new Group(UUID.randomUUID());
        g.addTransaction(t);
        g.addTransaction(tReverse);
        g.addTransaction(t);

        // get toPay value
        double seppToPay = g.toPay("Sepp Payer");
        double johnToPay = g.toPay("John Consumer");

        //System.out.println("John has to pay: " + johnToPay);
        System.out.println("Sepp has to pay " + seppToPay + "\n" +
                "John has to pay " + johnToPay);
        System.out.println("sumOfAllTransactions " + g.sumOfAllTransactions());


        assertFalse("some message", true);
    }

}
