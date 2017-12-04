package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class TransactionListActivity extends AppCompatActivity {

    public final static String KEY_PARTICIPANT = "participant"; // String
    public final static String KEY_GROUP_NAME = "group"; // String
    public final static String KEY_FILTER_TYPE = "filter_type"; // String
        // NO_FILTER (all transactions); PAID_BY_NAME; NAME_INVOLVED
    public final static String KEY_GROUP_ID = "filter_type"; // String

    private String participantName;
    private String groupName;
    private UUID groupID;
    private String type;
    private Group group;

    // also referred to by the adapter
    List<Transaction> transactionList = new ArrayList<>();
    ListView listView;

    String TAG = "###TransactionListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        //get information from intent
        Intent intent = getIntent();
        groupName = intent.getStringExtra(KEY_GROUP_NAME);
        groupID = UUID.fromString(intent.getStringExtra(KEY_GROUP_ID));
        type = intent.getStringExtra(KEY_FILTER_TYPE);

        //set title
        setTitle("Transactions in " + groupName);

        // if no type defined, show all transactions
        if (type == null)
            type = getString(R.string.filter_no_filter);

        // read Group from file to object
        FileHelper fileHelper = new FileHelper(this);
        try {
            String groupString = fileHelper.readFromFile(getString(R.string.path_groups),
                    groupID.toString());
            group = new Group(new JSONObject(groupString));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "not possible to load group from file :(", Toast.LENGTH_SHORT);
            onDestroy();
        }

        //make sure Group object is not null
        if (group == null) onDestroy();


        //-----------------------------------------------------------------
        //type case distinction
        // TODO: move to special method which takes the filter string and changes the transactionList
        //-----------------------------------------------------------------

        Log.d(TAG, "entering type case distinction with type " + type);
        //type: all transactions
        if (type.equals(getString(R.string.filter_no_filter))){
            setTitle("All Transactions for " + groupName);
            transactionList = group.getTransactions();

        //type: filter paid by
        } else if (type.equals(getString(R.string.filter_paid_by_name))) {
            //get payer name
            participantName = intent.getStringExtra(KEY_PARTICIPANT);
            setTitle(participantName + "'s expenses for " + groupName);

            //iterate over all transactions of this group
            for (Transaction t : group.getTransactions()){
                if (participantName.equals(t.getPayer())){
                    Log.d(TAG, "adding a transaction");
                    transactionList.add(t);
                }
            }

        //type: all involved (and paid)
        } else if (type.equals(getString(R.string.filter_name_involved))){
            //get name
            participantName = intent.getStringExtra(KEY_PARTICIPANT);
            setTitle("All " + participantName + "'s transactions for " + groupName);

            //iterate over all transactions of this group
            for (Transaction t : group.getTransactions()){
                if (t.getInvolved().contains(participantName) || participantName.equals(t.getPayer())){
                    Log.d(TAG, "adding a transaction");
                    transactionList.add(t);
                }
            }

        }

        //todo: take out nextline
        transactionList = group.getTransactions();

        ListTransactionAdapter adapter = new ListTransactionAdapter(this, transactionList);
        listView = findViewById(R.id.listView_transaction);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: define action on click
                // open transaction view
            }
        });
    }
}
