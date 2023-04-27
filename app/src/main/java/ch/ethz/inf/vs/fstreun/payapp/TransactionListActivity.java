package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.SimpleGroup;
import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.network.DataSync.Client.DataSyncSubscribeService;
import ch.ethz.inf.vs.fstreun.payapp.ListAdapters.ListTransactionAdapter;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class TransactionListActivity extends AppCompatActivity implements DataSyncSubscribeService.DataSyncCallback {

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

    private boolean boundDataSync;
    private DataSyncSubscribeService.DataSync dataSync;

    private String participantName;
    private String groupName;
    private UUID groupID;
    private Group group;

    // also referred to by the adapter
    List<Transaction> transactionList = new ArrayList<>();
    ListTransactionAdapter adapter;

    //Swipe data sync
    SwipeRefreshLayout swipeRefreshLayout;

    // Filter Buttons
    ToggleButton btnAll, btnPaid, btnInvolved;

    //shared prefs
    SharedPreferences sharedPreferences;

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
                Log.d(TAG, "btnAll pressed");

                // update buttons
                btnAll.setChecked(true);
                btnPaid.setChecked(false);
                btnInvolved.setChecked(false);

                // update filter type and button color according to button state
                setFilterType();
                setButtonColor();

                // update list according to filterType
                setFilteredList();
                updateListView();
            }
        });
        btnPaid = findViewById(R.id.btn_show_paid_by);
        btnPaid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnPaid pressed");

                // update buttons
                if(btnPaid.isChecked()) { // this means that the button was pressed and is now checked
                    btnAll.setChecked(false);
                    btnPaid.setChecked(true);
                } else if (btnInvolved.isChecked()){
                    btnPaid.setChecked(false);
                } else {
                    btnAll.setChecked(true);
                    btnPaid.setChecked(false);
                }

                // update filter type and button color according to button state
                setFilterType();
                setButtonColor();

                // update list according to filterType
                setFilteredList();
                updateListView();
            }
        });
        btnInvolved = findViewById(R.id.btn_show_involved);
        btnInvolved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnInvolved pressed");

                // update buttons
                if(btnInvolved.isChecked()) {// this means that the button was pressed and is now checked
                    btnAll.setChecked(false);
                    btnInvolved.setChecked(true);
                } else if (btnPaid.isChecked()) {
                    btnInvolved.setChecked(false);
                }
                else{
                    btnAll.setChecked(true);
                    btnInvolved.setChecked(false);
                }

                // update filter type and button color according to button state
                setFilterType();
                setButtonColor();

                // update list according to filterType
                setFilteredList();
                updateListView();
            }
        });

        // swipe refresh
        swipeRefreshLayout = findViewById(R.id.swipeSyncDataTransactionList);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                dataSync.synchronizeSession(mSimpleGroup.sessionID, TransactionListActivity.this);
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
        /*
        Log.d(TAG, "groupName = " + groupName);
        Log.d(TAG, "groupID = " + groupID.toString());
        */

        //get shared prefs
        String prefName = getString(R.string.pref_name) + groupID;
        sharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);

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
            btnPaid.setTextOn("Paid by " + participantName);
            btnPaid.setTextOff("Paid by " + participantName);
            btnInvolved.setTextOn("" + participantName + " involved");
            btnInvolved.setTextOff("" + participantName + " involved");
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
        setButtonColor();
        updateListView();

        // Bind DataService
        Intent intentService = new Intent(this, DataService.class);
        bindService(intentService, connection, BIND_AUTO_CREATE);
        Log.d(TAG, "called bindService");

        // Bind DataSync
        Intent intentDataSync = new Intent(this, DataSyncSubscribeService.class);
        bindService(intentDataSync, connection, BIND_AUTO_CREATE);
    }

    /**
     * set Button states according to filterType
     * (this should be used only when creating this activity)
     */
    private void setButtonsAccordingToFilterType() {
        if (filterType == null) {
            btnAll.callOnClick();
            Log.d(TAG, "setButtonsAccordingToFilterType failed. filterType == null");
        }
        else if (filterType.equals(getString(R.string.filter_no_filter))) btnAll.callOnClick();
        else if (filterType.equals(getString(R.string.filter_paid_by_name))) {
            btnInvolved.setChecked(false);
            btnPaid.setChecked(true);
            btnPaid.callOnClick();
        } else if (filterType.equals(getString(R.string.filter_name_involved))) {
            btnPaid.setChecked(false);
            btnInvolved.setChecked(true);
            btnInvolved.callOnClick();
        } else if (filterType.equals(getString(R.string.filter_involved_or_paid))){
            btnPaid.setChecked(true);
            btnInvolved.setChecked(true);
            btnInvolved.callOnClick();
        } else {
            btnAll.callOnClick();
            Log.d(TAG, "setButtonsAccordingToFilterType failed to read a right filter type");
        }
    }

    /**
     * set filterType according to buttons (the usual way)
     */
    private void setFilterType() {
        if(btnAll.isChecked()) filterType = getString(R.string.filter_no_filter);
        else if (btnInvolved.isChecked()){
            if (btnPaid.isChecked()) filterType = getString(R.string.filter_involved_or_paid);
            else filterType = getString(R.string.filter_name_involved);
        } else if (btnPaid.isChecked()) filterType = getString(R.string.filter_paid_by_name);
        else{
            // no button is checked
            Log.d(TAG, "somehow now button is checked");
        }
    }

    private void setButtonColor() {
        // set colors
        final int checked = getResources().getColor(R.color.colorGreyDark);
        final int unchecked = getResources().getColor(R.color.colorPrimary);

        // set each button color according to isChecked()
        if(btnAll.isChecked()) btnAll.setBackgroundColor(checked);
        else btnAll.setBackgroundColor(unchecked);
        if(btnPaid.isChecked()) btnPaid.setBackgroundColor(checked);
        else btnPaid.setBackgroundColor(unchecked);
        if(btnInvolved.isChecked()) btnInvolved.setBackgroundColor(checked);
        else btnInvolved.setBackgroundColor(unchecked);

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
                DataService.DataServiceBinder binder = (DataService.DataServiceBinder) service;

                sessionAccess = binder.getSessionClientAccess(group.getSessionID());
                if(sessionAccess != null) {
                    bound = true;
                } else Log.d(TAG, "sessionAccess is null");

                Log.d(TAG, "onServiceConnected: " + name.getClassName());

                Log.d(TAG, "setButtonsAccordingToFilterType in onServiceConnected");
                setButtonsAccordingToFilterType();
                setButtonColor();

                // if a open transaction exists, try to store it!
                storeOpenTransaction();
                // update all views
                updateViews();
            }else if (name.getClassName().equalsIgnoreCase(DataSyncSubscribeService.class.getName())) {
                DataSyncSubscribeService.LocalBinder binder = (DataSyncSubscribeService.LocalBinder) service;
                dataSync = binder.getDataSync();
                if (dataSync == null) {
                    Log.e(TAG, "Failed to get DataSync Service access");
                    return;
                }
                boundDataSync = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
                Log.e(TAG, "onServiceDisconnected: " + name.getClassName());
                if (name.getClassName().equals(DataService.class.getName())){
                    bound = false;
                }else if (name.getClassName().equals(DataSyncSubscribeService.class.getName())){
                    boundDataSync = false;
                }
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_addTransaction:
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
                    List<String> participantsList = group.getParticipantNames();
                    for (int i=0; i<n; i++){
                        participants[i] = participantsList.get(i);
                    }
                }


                // get LRU payer from shared prefs
                String payerKey = getString(R.string.pref_payer_lru);
                String payer = sharedPreferences.getString(payerKey, group.getDefaultParticipantName());

                //get initially checked participants from shared prefs
                Set<String> checkedPartiSet = sharedPreferences.getStringSet(
                        getString(R.string.pref_involved_lru), null);
                String[] checkedParticipants;
                if(checkedPartiSet != null) {
                    checkedParticipants = checkedPartiSet.toArray(new String[checkedPartiSet.size()]);
                } else {
                    checkedParticipants = new String[0];
                }

                //----------------------------------------------------------------------------
                // PUTTING INFORMATION to intent
                //----------------------------------------------------------------------------

                //put intents
                intent.putExtra(TransactionCreationActivity.KEY_PARTICIPANTS, participants);
                intent.putExtra(TransactionCreationActivity.KEY_PAYER, payer);
                intent.putExtra(TransactionCreationActivity.KEY_PARTICIPANTS_CHECKED, checkedParticipants);
                startActivityForResult(intent, GroupActivity.CREATE_TRANSACTION_REQUEST);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_transaction_list, menu);
        return true;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.menu_createReverseTransaction:
                int position = info.position;
                Transaction transactionToDelete = adapter.getItem(position);
                Transaction reverseTransaction = transactionToDelete.reverse(System.currentTimeMillis());
                //open transaction creation activity or dialog to set the comment for reverse transaction
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
                    List<String> participantsList = group.getParticipantNames();
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

                // transaction can only be stored after service was bound.
                // so store it for the service in global field
                openTransaction = data.getExtras();

                //store payer & involved in sharedPrefs
                SharedPreferences.Editor editor = sharedPreferences.edit();
                String payerKey = getString(R.string.pref_payer_lru);
                String[] involvedArray = openTransaction.getStringArray(TransactionCreationActivity.KEY_PARTICIPANTS_INVOLVED);
                Set<String> involvedSet = new HashSet();
                for(int i=0; i<involvedArray.length; i++) involvedSet.add(involvedArray[i]);
                String involvedKey = getString(R.string.pref_involved_lru);
                editor.putString(payerKey, openTransaction.getString(TransactionCreationActivity.KEY_PAYER));
                editor.putStringSet(involvedKey, involvedSet);
                editor.apply();

                storeOpenTransaction();
                updateViews();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setFilteredList() {

        //empty transactionList
        transactionList.clear();
        List<Transaction> tempTransactions;

        if (bound){
            List<String> stringsTransactions = sessionAccess.getContent();
            if (stringsTransactions == null){
                return;
            }
            tempTransactions = new ArrayList<>(stringsTransactions.size());

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


        }
        else {
            Log.d(TAG, "not possible to load data from session");

            tempTransactions = group.getTransactions();
        }

        //-----------------------------------------------------------------
        //type case distinction
        //-----------------------------------------------------------------

        Log.d(TAG, "entering type case distinction with type " + filterType);
        //type: all transactions
        if (filterType.equals(getString(R.string.filter_no_filter))){
            transactionList.addAll(tempTransactions);
            //type: filter paid by
        } else if (filterType.equals(getString(R.string.filter_paid_by_name))) {
            //iterate over all transactions of this group
            for (Transaction t : tempTransactions){

                if (participantName != null && Transaction.caseInsensitiveEquals(participantName,
                        t.getPayer())){
                    transactionList.add(t);
                }
            }

            //type: all involved
        } else if (filterType.equals(getString(R.string.filter_name_involved))){
            //iterate over all transactions of this group
            for (Transaction t : tempTransactions){
                if (t.involvedContains(participantName)){
                    transactionList.add(t);
                }
            }

            //type: both involved and paid are selected
        } else if (filterType.equals(getString(R.string.filter_involved_or_paid))){
            //iterate over all transactions of this group
            for (Transaction t : tempTransactions){
                if (t.involvedContains(participantName) || Transaction.caseInsensitiveEquals(
                        participantName, t.getPayer())){
                    transactionList.add(t);
                }
            }

        }

        Collections.sort(transactionList);
        Collections.reverse(transactionList);
    }

    private void updateListView() {
        adapter.notifyDataSetChanged();
    }


    /**
     *
     */
    private void updateViews() {
        setFilteredList();
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

    @Override
    public void dataUpdated() {
        Log.d(TAG, "DataUpdated callback");

        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(this.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                updateViews();
                swipeRefreshLayout.setRefreshing(false);
            } // This is your code
        };
        mainHandler.post(myRunnable);
    }
}
