package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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
    public final static String KEY_GROUP_NAME = "group_name"; // String
    public final static String KEY_FILTER_TYPE = "filter_type"; // String
        // NO_FILTER (all transactions); PAID_BY_NAME; NAME_INVOLVED
        // see R.string.filter_* for definitions
    public final static String KEY_GROUP_ID = "group_id"; // String

    private String participantName;
    private String groupName;
    private UUID groupID;
    private Group group;

    // also referred to by the adapter
    List<Transaction> transactionList = new ArrayList<>();
    ListView listView;

    // Filter Buttons
    Button btnAll, btnPaid, btnInvolved;

    String TAG = "###TransactionListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);

        // initialize buttons
        btnAll = findViewById(R.id.btn_show_all_transaction);
        btnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo: make this button marked and the others not marked
                updateListView(getFilteredList(getString(R.string.filter_no_filter)));
            }
        });
        btnPaid = findViewById(R.id.btn_show_paid_by);
        btnPaid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo: make this button marked and the others not marked
                updateListView(getFilteredList(getString(R.string.filter_paid_by_name)));
            }
        });
        btnInvolved = findViewById(R.id.btn_show_involved);
        btnInvolved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo: make this button marked and the others not marked
                updateListView(getFilteredList(getString(R.string.filter_name_involved)));
            }
        });

        //initialize listView
        listView = findViewById(R.id.listView_transaction);

        //get information from intent
        Intent intent = getIntent();
        groupName = intent.getStringExtra(KEY_GROUP_NAME);
        groupID = UUID.fromString(intent.getStringExtra(KEY_GROUP_ID));
        String type = intent.getStringExtra(KEY_FILTER_TYPE);
        Log.d(TAG, "type = " + type);
        Log.d(TAG, "groupName = " + groupName);
        Log.d(TAG, "groupID = " + groupID.toString());

        //set title
        setTitle("Transactions in " + groupName);

        //get payer name
        participantName = intent.getStringExtra(KEY_PARTICIPANT);

        //set button text
        if(participantName != null){
            btnPaid.setText("Paid by " + participantName);
            btnInvolved.setText("" + participantName + " involved");
        }

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

        // call filter function
        transactionList = getFilteredList(type);

        // call updated function
        updateListView(transactionList);
    }

    private List<Transaction> getFilteredList(String type) {
        //-----------------------------------------------------------------
        //type case distinction
        //-----------------------------------------------------------------

        List<Transaction> result = new ArrayList<>();

        Log.d(TAG, "entering type case distinction with type " + type);
        //type: all transactions
        if (type.equals(getString(R.string.filter_no_filter))){
            result = group.getTransactions();

            //type: filter paid by
        } else if (type.equals(getString(R.string.filter_paid_by_name))) {
            //iterate over all transactions of this group
            for (Transaction t : group.getTransactions()){
                if (participantName.equals(t.getPayer())){
                    Log.d(TAG, "adding a transaction");
                    result.add(t);
                }
            }

            //type: all involved (and paid)
        } else if (type.equals(getString(R.string.filter_name_involved))){
            //iterate over all transactions of this group
            for (Transaction t : group.getTransactions()){
                if (t.getInvolved().contains(participantName) || participantName.equals(t.getPayer())){
                    Log.d(TAG, "adding a transaction");
                    result.add(t);
                }
            }

        }

        return result;

    }

    private void updateListView(List<Transaction> transactionList) {
        ListTransactionAdapter adapter = new ListTransactionAdapter(this, transactionList);
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
