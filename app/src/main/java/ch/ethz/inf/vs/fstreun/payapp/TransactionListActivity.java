package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
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
    ListTransactionAdapter adapter;

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
                setButtonColor(getString(R.string.filter_no_filter));
                setFilteredList(getString(R.string.filter_no_filter));
                updateListView();
            }
        });
        btnPaid = findViewById(R.id.btn_show_paid_by);
        btnPaid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonColor(getString(R.string.filter_paid_by_name));
                setFilteredList(getString(R.string.filter_paid_by_name));
                updateListView();
            }
        });
        btnInvolved = findViewById(R.id.btn_show_involved);
        btnInvolved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonColor(getString(R.string.filter_name_involved));
                setFilteredList(getString(R.string.filter_name_involved));
                updateListView();
            }
        });


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

        //hide buttons if no participant in intent
        if (participantName == null){
            LinearLayout linearLayoutButtons = findViewById(R.id.lin_lay_filter_btn);
            linearLayoutButtons.setVisibility(View.GONE);
        }

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
        setFilteredList(type);

        // call updated function
        Collections.sort(transactionList);
        adapter = new ListTransactionAdapter(this, transactionList);
        ListView listView = findViewById(R.id.listView_transaction);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // open transaction view
                Intent intent = new Intent(TransactionListActivity.this,
                        TransactionInfoActivity.class);
                intent.putExtra(TransactionInfoActivity.KEY_TRANSACTION, transactionList.get(position).toString());
                startActivity(intent);
            }
        });
        setButtonColor(type);
        updateListView();
    }

    private void setButtonColor(String type) {
        //set colors
        final int checked = getResources().getColor(R.color.colorAccent);
        final int unchecked = getResources().getColor(R.color.colorPrimary);

        if(type.equals(getString(R.string.filter_no_filter))) {
            btnAll.setBackgroundColor(checked);
            btnPaid.setBackgroundColor(unchecked);
            btnInvolved.setBackgroundColor(unchecked);
        } else if (type.equals(getString(R.string.filter_paid_by_name))){
            btnAll.setBackgroundColor(unchecked);
            btnPaid.setBackgroundColor(checked);
            btnInvolved.setBackgroundColor(unchecked);
        } else if (type.equals(getString(R.string.filter_name_involved))){
            btnAll.setBackgroundColor(unchecked);
            btnPaid.setBackgroundColor(unchecked);
            btnInvolved.setBackgroundColor(checked);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void setFilteredList(String type) {
        //-----------------------------------------------------------------
        //type case distinction
        //-----------------------------------------------------------------

        //empty transactionList
        transactionList.clear();

        Log.d(TAG, "entering type case distinction with type " + type);
        //type: all transactions
        if (type.equals(getString(R.string.filter_no_filter))){
            transactionList.addAll(group.getTransactions());

            //type: filter paid by
        } else if (type.equals(getString(R.string.filter_paid_by_name))) {
            //iterate over all transactions of this group
            for (Transaction t : group.getTransactions()){
                if (participantName != null && participantName.equals(t.getPayer())){
                    Log.d(TAG, "adding a transaction");
                    transactionList.add(t);
                }
            }

            //type: all involved (and paid)
        } else if (type.equals(getString(R.string.filter_name_involved))){
            //iterate over all transactions of this group
            for (Transaction t : group.getTransactions()){
                if (t.getInvolved().contains(participantName) || participantName.equals(t.getPayer())){
                    Log.d(TAG, "adding a transaction");
                    transactionList.add(t);
                }
            }

        }
    }

    private void updateListView() {
        adapter.notifyDataSetChanged();
    }

}
