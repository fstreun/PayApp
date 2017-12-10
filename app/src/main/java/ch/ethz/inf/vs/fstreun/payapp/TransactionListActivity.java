package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class TransactionListActivity extends AppCompatActivity {

    public final static String KEY_PARTICIPANT = "participant"; // String
    public final static String KEY_FILTER_TYPE = "filter_type"; // String
    private String filterType;
        // NO_FILTER (all transactions); PAID_BY_NAME; NAME_INVOLVED
        // see R.string.filter_* for definitions

    // simple group expected to be in the intent
    public static final String KEY_SIMPLE_GROUP = "simple_group";
    private SimpleGroup mSimpleGroup;

    // Session Service communication
    private boolean bound;
    private DataService.SessionClientAccess sessionAccess;

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
        // get SimpleGroup
        String stringSimpleGroup = intent.getStringExtra(KEY_SIMPLE_GROUP);
        if(stringSimpleGroup != null && !stringSimpleGroup.isEmpty()) {
            try {
                JSONObject object = new JSONObject(stringSimpleGroup);
                mSimpleGroup = new SimpleGroup(object);
            } catch (JSONException e) {
                Toast.makeText(this, "failed to load group", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "SimpleGroup creation from JSON failed.", e);
                return;
            }

            Log.d(TAG, "received SimpleGroup: " + stringSimpleGroup);

        } else {
            Toast.makeText(this, "failed to load group", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "no SimpleGroup received");
            return;
        }

        groupName = mSimpleGroup.groupName;
        groupID = mSimpleGroup.groupID;
        filterType = intent.getStringExtra(KEY_FILTER_TYPE);
        Log.d(TAG, "type = " + filterType);
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
        if (filterType == null)
            filterType = getString(R.string.filter_no_filter);


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

        // call updated function
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
        registerForContextMenu(listView);
        listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                Log.d(TAG, "onCreateContextMenu started");
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.context_menu_transaction, menu);
            }
        });
        setButtonColor(filterType);
        updateListView();

        // Bind DataService
        Intent intentService = new Intent(this, DataService.class);
        bindService(intentService, connection, BIND_AUTO_CREATE);
        Log.d(TAG, "called bindService");
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
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from service
        if (bound){
            unbindService(connection);
            Log.d(TAG, "called unbindService");
            bound = false;
        }
    }

    // service binding handler
    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (name.getClassName().equals(DataService.class.getName())){
                DataService.LocalBinder binder = (DataService.LocalBinder) service;

                sessionAccess = binder.getSessionClientAccess(group.getSessionID());
                bound = true;
                Log.d(TAG, "onServiceConnected: " + name.getClassName());

                // if a open transaction exists, try to store it!
                storeOpenTransaction();
                // update all views
                updateViews();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected");
            bound = false;
        }
    };


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

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.menu_deleteTransaction:
                int position = info.position;
                Transaction transactionToDelete = adapter.getItem(position);
                Transaction reverseTransaction = transactionToDelete.reverse(System.currentTimeMillis());
                //todo: open transaction creation activity or dialog to set the comment for reverse transaction
                Intent intent = new Intent(TransactionListActivity.this,
                        TransactionCreationActivity.class);
                //----------------------------------------------------------------------------
                // GETTING INFORMATION
                //----------------------------------------------------------------------------

                //putting participants of group into String array
                String[] participants = new String[0];
                if (group != null){
                    int n = group.numParticipants();
                    participants = new String[n];
                    List<String> participantsList = group.getParticipants();
                    for (int i=0; i<n; i++){
                        participants[i] = participantsList.get(i);
                    }
                }

                //putting involved participants into string array
                int n = reverseTransaction.getNumInvolved();
                String[] checkedParticipants = new String[n];
                for(int i=0; i<n; i++) checkedParticipants[i] = reverseTransaction.getInvolved().get(i);

                //putting comment
                String comment = reverseTransaction.getComment();

                //----------------------------------------------------------------------------
                // PUTTING INFORMATION to intent
                //----------------------------------------------------------------------------

                //put intents
                intent.putExtra(TransactionCreationActivity.KEY_PARTICIPANTS, participants);
                intent.putExtra(TransactionCreationActivity.KEY_PAYER, reverseTransaction.getPayer());
                intent.putExtra(TransactionCreationActivity.KEY_PARTICIPANTS_CHECKED, checkedParticipants);
                intent.putExtra(TransactionCreationActivity.KEY_AMOUNT, reverseTransaction.amount);
                intent.putExtra(TransactionCreationActivity.KEY_COMMENT, comment);
                startActivityForResult(intent, GroupActivity.CREATE_TRANSACTION_REQUEST);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GroupActivity.CREATE_TRANSACTION_REQUEST){
            if (resultCode == RESULT_OK){
                Log.d(TAG, "transaction creation received");

                // get some infos
                String payer = data.getStringExtra(TransactionCreationActivity.KEY_PAYER);
                String[] involvedString = data.getStringArrayExtra(TransactionCreationActivity.KEY_PARTICIPANTS_INVOLVED);
                List<String> involved = Arrays.asList(involvedString);

                //store data to shared prefs for next transaction creation
                //editor.putString(payerKey, payer);
                //editor.putStringSet(involvedKey, new HashSet<>(involved));
                //editor.apply();

                // transaction can only be stored after service was bound.
                // so store it for the service in global field
                openTransaction = data.getExtras();
                storeOpenTransaction();
                updateViews();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setFilteredList(String type) {
        //-----------------------------------------------------------------
        //type case distinction
        //-----------------------------------------------------------------

        if (!bound){
            return;
        }

        //empty transactionList
        transactionList.clear();

        List<String> stringsTransactions = sessionAccess.getContent();
        if (stringsTransactions == null){
            return;
        }
        List<Transaction> tempTransactions = new ArrayList<>(stringsTransactions.size());

        for (String s : stringsTransactions){
            try {
                JSONObject object = new JSONObject(s);
                tempTransactions.add(new Transaction(object));
            } catch (JSONException e) {
                tempTransactions.clear();
                Log.e(TAG, "Failed to create Transactions from JSON.", e);
                return;
            }
        }

        Log.d(TAG, "entering type case distinction with type " + type);
        //type: all transactions
        if (type.equals(getString(R.string.filter_no_filter))){
            transactionList.addAll(tempTransactions);
            //type: filter paid by
        } else if (type.equals(getString(R.string.filter_paid_by_name))) {
            //iterate over all transactions of this group
            for (Transaction t : tempTransactions){

                if (participantName != null && participantName.equals(t.getPayer())){
                    Log.d(TAG, "adding a transaction");
                    transactionList.add(t);
                }
            }

            //type: all involved (and paid)
        } else if (type.equals(getString(R.string.filter_name_involved))){
            //iterate over all transactions of this group
            for (Transaction t : tempTransactions){
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


    /**
     *
     */
    private void updateViews() {
        setFilteredList(filterType);
        updateListView();
    }


    /**
     * transaction which was not been saved yet
     */
    Bundle openTransaction = null;

    /**
     * stores transaction stored in openTransaction field.
     * adds to the session and to the group
     * @return success
     */
    private synchronized boolean storeOpenTransaction(){
        if (openTransaction == null){
            return true;
        }

        if (!bound){
            return false;
        }

        // get Transaction info
        double amount = openTransaction.getDouble(TransactionCreationActivity.KEY_AMOUNT, 0.0);
        String comment = openTransaction.getString(TransactionCreationActivity.KEY_COMMENT);
        String payer = openTransaction.getString(TransactionCreationActivity.KEY_PAYER);
        String[] involvedString = openTransaction.getStringArray(TransactionCreationActivity.KEY_PARTICIPANTS_INVOLVED);
        List<String> involved = Arrays.asList(involvedString);

        UUID userUuid = sessionAccess.getUserID();
        if(userUuid == null){
            return false;
        }
        Transaction transaction = new Transaction(userUuid, payer, involved, amount,
                System.currentTimeMillis(), comment);

        try {
            sessionAccess.add(transaction.toJson().toString());
            Log.d(TAG, "Transactions in Session: " + sessionAccess.getContent().toString());
        } catch (JSONException e) {
            Log.e(TAG, "Transaction to JSON failed", e);
            return false;
        }


        Toast.makeText(this, "Saved Transaction", Toast.LENGTH_SHORT).show();
        openTransaction = null;
        return true;
    }

}
