package ch.ethz.inf.vs.fstreun.payapp;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Transaction;

/**
 * Created by anton on 23.11.17.
 */

public class FinanceUnitTest {
    String TAG = "FinanceUnitTest";

    @Test
    public void transactionCreation(){
        UUID creatorUuid = UUID.randomUUID();
        List<String> involved = new ArrayList<>(2);
        involved.add("Sepp Payer");
        involved.add("John Consumer");
        Transaction t = new Transaction(creatorUuid, "Sepp Payer", involved, 2000, "beer");
        String jsonString = t.toString();
        System.out.println("created transaction: \n" + jsonString);
    }
}
