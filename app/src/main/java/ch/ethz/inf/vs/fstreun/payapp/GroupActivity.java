package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.Participant;
import ch.ethz.inf.vs.fstreun.finance.SimpleGroup;
import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.network.DataSyncSubscribeService;
import ch.ethz.inf.vs.fstreun.payapp.ListAdapters.ListParticipantsAdapter;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class GroupActivity extends AppCompatActivity implements DataSyncSubscribeService.DataSyncCallback{

    String TAG = "###GroupActivity###";

    // different results
    public static final String KEY_RESULT_CODE = "key_result_type";
    public static final String KEY_START_CODE = "start_code";
    public static final int CODE_DEFAULT = 0;
    public static final int CODE_DELETE = 1;
    private boolean groupForDeletion = false;

    // simple group expected to be in the intent
    public static final String KEY_SIMPLE_GROUP = "simple_group";
    private SimpleGroup mSimpleGroup;

    // current group loaded from file (with simple group information)
    private Group group = null;

    // Session Service communication
    private boolean boundDataService;
    private DataService.SessionClientAccess sessionAccess;

    private boolean boundDataSync;
    private DataSyncSubscribeService.DataSync dataSync;

    //file stuff
    FileHelper fileHelper;

    // main participant views
    LinearLayout linLayOwn;
    TextView tvDeviceOwner;
    TextView tvOwnSpent, tvOwnOwes, tvOwnCredit;

    // swipe to sync
    SwipeRefreshLayout swipeRefreshLayout;

    // list of all participants
    private ListParticipantsAdapter adapter;
    private List<Participant> participantList = new ArrayList<>();


    //Shared Preferences for specific group
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String prefName;
    String payerKey, involvedKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        Log.d(TAG, "creation started");

        // add UpButton
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //create fileHelper
        fileHelper = new FileHelper(this);

        //getting intent
        Intent intent = getIntent();
        String startCode = intent.getStringExtra(KEY_START_CODE);
        if(startCode != null && startCode.equals("delete")) groupForDeletion = true;
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

        // load group from file
        if (group == null) {
            group = loadGroup(mSimpleGroup);
        }
        // check loaded group
        if (group == null){
            Toast.makeText(this, "failed to load group", Toast.LENGTH_SHORT).show();
            return;
        }


        //set shared prefsKeys
        payerKey = getString(R.string.pref_payer_lru);
        involvedKey = getString(R.string.pref_involved_lru);

        //initialize shared Prefs for this group
        prefName = getString(R.string.pref_name) + mSimpleGroup.groupID;
        sharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
        editor = sharedPreferences.edit();


        // GUI //

        //set group name as title
        setTitle(mSimpleGroup.groupName);


        //textView device owner
        tvDeviceOwner = findViewById(R.id.textView_device_owner);
        tvOwnSpent = findViewById(R.id.textView_ownSpent);
        tvOwnOwes = findViewById(R.id.textView_ownOwes);
        tvOwnCredit = findViewById(R.id.textView_ownCredit);
        linLayOwn = findViewById(R.id.lin_lay_deviceOwner);

        linLayOwn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupActivity.this,
                        TransactionListActivity.class);
                intent.putExtra(TransactionListActivity.KEY_FILTER_TYPE,
                        getString(R.string.filter_paid_by_name));
                intent.putExtra(TransactionListActivity.KEY_PARTICIPANT,
                        group.getDefaultParticipantName());
                try {
                    intent.putExtra(TransactionListActivity.KEY_SIMPLE_GROUP, mSimpleGroup.toJSON().toString());
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to create SimpleGroup JSON.", e);
                    return;
                }
                GroupActivity.this.startActivity(intent);
            }
        });

        // floating button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createTransaction();
            }
        });

        // swipe to sync data
        swipeRefreshLayout = findViewById(R.id.swipeSyncData);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(TAG, "swipeRefreshLayout with refreshing = " + swipeRefreshLayout.isRefreshing());
                dataSync.synchronizeSession(mSimpleGroup.sessionID, GroupActivity.this);
            }
        });
        /*
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "post refresh");
                swipeRefreshLayout.setRefreshing(true);
            }
        });
        */

        // participant view
        adapter = new ListParticipantsAdapter(this, participantList);

        ListView listView = findViewById(R.id.listView_main);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Participant List Item clicked. position: " + position);
                Intent intent = new Intent(GroupActivity.this,
                        TransactionListActivity.class);
                intent.putExtra(TransactionListActivity.KEY_FILTER_TYPE,
                        getString(R.string.filter_paid_by_name));
                intent.putExtra(TransactionListActivity.KEY_PARTICIPANT,
                        adapter.getItem(position).name);
                try {
                    intent.putExtra(TransactionListActivity.KEY_SIMPLE_GROUP, mSimpleGroup.toJSON().toString());
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to create SimpleGroup JSON.", e);
                    return;
                }
                GroupActivity.this.startActivity(intent);
            }
        });

        //update all views
        updateViews();

        // Bind DataService
        Intent intentService = new Intent(this, DataService.class);
        bindService(intentService, connection, BIND_AUTO_CREATE);
        Log.d(TAG, "called bindService");

        // Bind DataSync
        Intent intentDataSync = new Intent(this, DataSyncSubscribeService.class);
        bindService(intentDataSync, connection, BIND_AUTO_CREATE);

        Log.d(TAG, "creation finished");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from service
        if (boundDataService){
            unbindService(connection);
            Log.d(TAG, "called unbindService");
            boundDataService = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        writeGroup();
    }

    // service binding handler
    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (name.getClassName().equals(DataService.class.getName())){
                DataService.LocalBinder binder = (DataService.LocalBinder) service;

                sessionAccess = binder.getSessionClientAccess(mSimpleGroup.sessionID);
                if (sessionAccess == null){
                    Log.e(TAG, "Failed to get Session access: " + mSimpleGroup.sessionID);
                    return;
                }
                boundDataService = true;
                Log.d(TAG, "onServiceConnected: " + name.getClassName());

                if (groupForDeletion){
                    deleteGroup();
                }
                // load transactions to the group
                loadTransactions();
                // if a open transaction exists, try to store it!
                storeOpenTransaction();
                // update all views
                updateViews();
            }else if (name.getClassName().equalsIgnoreCase(DataSyncSubscribeService.class.getName())){
                DataSyncSubscribeService.LocalBinder binder = (DataSyncSubscribeService.LocalBinder) service;
                dataSync = binder.getDataSync();
                if (dataSync == null){
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
                boundDataService = false;
            }else if (name.getClassName().equals(DataSyncSubscribeService.class.getName())){
                boundDataService = false;
            }
        }
    };


    /**
     * loads group from file.
     * @param simpleGroup defines the to be loaded group
     * @return
     */
    @Nullable
    private Group loadGroup(@NonNull SimpleGroup simpleGroup){
        Group g = null;
        try {
            String file = fileHelper.readFromFile(
                    getString(R.string.path_groups), simpleGroup.groupID.toString());
            JSONObject groupJson = new JSONObject(file);
            Log.d(TAG, "reading from file: " + simpleGroup.groupID.toString());
            g = new Group(groupJson);
        } catch (Exception e) {
            //if fail return null
            Log.e(TAG, "failed to load group from file.", e);
            return null;
        }
        return g;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!loadTransactions()){
            //load data from group file if not possible from session
            List<Transaction> transactionList = new ArrayList<>();
            if (sessionAccess == null) return;
            for (String s : sessionAccess.getContent()){
                try {
                    transactionList.add(new Transaction(new JSONObject(s)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            group.setTransactions(transactionList);
        }
        updateViews();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_showAllTransactions:
                //case view all transactions
                Intent intent1 = new Intent(GroupActivity.this,
                        TransactionListActivity.class);
                intent1.putExtra(TransactionListActivity.KEY_FILTER_TYPE,
                        getString(R.string.filter_no_filter));
                try {
                    intent1.putExtra(TransactionListActivity.KEY_SIMPLE_GROUP, mSimpleGroup.toJSON().toString());
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to create SimpleGroup JSON.", e);
                    return true;
                }

                GroupActivity.this.startActivity(intent1);
                return true;

            case R.id.menu_syncData:
                if (boundDataSync){
                    dataSync.synchronizeSession(mSimpleGroup.sessionID, this);
                    swipeRefreshLayout.setRefreshing(true);
                }

                // TODO: update data with dataAccess after some time

                return true;
            case R.id.menu_publishGroup:
                Intent intent = new Intent(this, PublishGroupActivity.class);
                try {
                    intent.putExtra(PublishGroupActivity.KEY_SIMPLEGROUP, mSimpleGroup.toJSON().toString());
                } catch (JSONException e) {
                    // failed to create SimpleGroup JSON
                    Log.e(TAG, "Failed to create JSON of SimpleGroup.", e);
                }
                startActivity(intent);
                return true;

            case R.id.menu_setMainParticipant:
                showMainParticipantDialog();
                return true;

            case R.id.menu_deleteGroup:
                showDeleteGroupDialog();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public final static int CREATE_TRANSACTION_REQUEST = 666;
    private void createTransaction(){
        Intent intent = new Intent(this, TransactionCreationActivity.class);

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

        //get LRU payer from shared prefs (default value is the deviceOwner)
        String payer = sharedPreferences.getString(payerKey,
                group.getDefaultParticipantName());

        //get initially checked participants from shared prefs
        Set<String> checkedPartiSet = sharedPreferences.getStringSet(
                involvedKey, null);
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
        startActivityForResult(intent, CREATE_TRANSACTION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CREATE_TRANSACTION_REQUEST){
            if (resultCode == RESULT_OK){
                Log.d(TAG, "transaction creation received");

                // get some infos
                String payer = data.getStringExtra(TransactionCreationActivity.KEY_PAYER);
                String[] involvedString = data.getStringArrayExtra(TransactionCreationActivity.KEY_PARTICIPANTS_INVOLVED);
                List<String> involved = Arrays.asList(involvedString);

                //store data to shared prefs for next transaction creation
                editor.putString(payerKey, payer);
                editor.putStringSet(involvedKey, new HashSet<>(involved));
                editor.apply();

                // transaction can only be stored after service was boundDataService.
                // so store it for the service in global field
                openTransaction = data.getExtras();
                storeOpenTransaction();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * updates all the views according to the information given in the global fields
     */
    private void updateViews() {
        participantList.clear();
        participantList.addAll(group.getParticipants());


        String defPart = group.getDefaultParticipantName();
        if(defPart == null || defPart.isEmpty()){
            linLayOwn.setVisibility(View.GONE);
        } else {
            linLayOwn.setVisibility(View.VISIBLE);
            tvDeviceOwner.setText(defPart);

            // set values to default Participant
            tvOwnSpent.setText(Transaction.doubleToString(group.spent(defPart)));
            tvOwnOwes.setText(Transaction.doubleToString(group.owes(defPart)));
            Double credit = group.credit(defPart);
            tvOwnCredit.setText(Transaction.doubleToString(credit));

            //color the credit value
            int compare = credit.compareTo(0.0);
            if (compare < 0){
                // toPay is a negativ number
                tvOwnCredit.setTextColor(getResources().getColor(R.color.colorAccent));
            }else if (compare > 0){
                // toPay is a positiv number
                tvOwnCredit.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
            }else {
                // toPay is equals to 0.0
                tvOwnCredit.setTextColor(getResources().getColor(R.color.colorGrey));
            }

            // remove default participant from list
            for (int i = 0; i < participantList.size(); i++){
                if (participantList.get(i).name.equalsIgnoreCase(defPart)){
                    participantList.remove(i);
                }
            }
        }
        adapter.notifyDataSetChanged();
        Log.d(TAG, "updated all views");
    }

    private void showMainParticipantDialog(){
        List<String> partList = group.getParticipantNames();
        // get index of current default participant
        int current = partList.indexOf(group.getDefaultParticipantName());
        if (current < 0){
            // if none set current to the last element
            current = group.numParticipants();
        }
        final String[] participants = group.getParticipantNames().toArray(new String[group.numParticipants()+1]);
        participants[group.numParticipants()] = "(None)";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Main Participant");
        builder.setSingleChoiceItems(participants, current, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String main = participants[which];
                setMainParticipant(main);
                dialog.dismiss();
            }
        });
        builder.create().show();
        Log.d(TAG, "main participant dialog shown");
    }

    private void setMainParticipant(String participant){
        Log.d(TAG, "main participant chosen: " + participant);
        if (participant.equalsIgnoreCase("(None)")){
            group.setDeviceOwner(null);
        }else {
            group.setDeviceOwner(participant);
        }

        updateViews();
    }

    private void writeGroup() {
        if (group != null) {
            fileHelper.writeToFile(getString(R.string.path_groups), mSimpleGroup.groupID.toString(), group.toString());
        }
    }

    private void showDeleteGroupDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete group: " + mSimpleGroup.groupName + " ?");
        builder.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteGroup();
            }
        });
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        builder.create().show();

        Log.d(TAG, "delete group dialog shown");
    }

    /**
     * deletes the current group and al its dependence
     */
    private boolean deleteGroup(){
        boolean success = false;
        if (sessionAccess != null) {
            success = sessionAccess.removeSession();
        }

        // remove group file
        success &= fileHelper.removeFile(getString(R.string.path_groups), mSimpleGroup.groupID.toString());

        Log.d(TAG, "delete group. success: " + success);

        Intent intent = new Intent();
        intent.putExtra(KEY_RESULT_CODE, CODE_DELETE);
        intent.putExtra(KEY_SIMPLE_GROUP, mSimpleGroup.toString());

        setResult(RESULT_OK, intent);
        finish();
        return success;

    }


    /**
     * loads all transactions from the Session Service into the group
     * @return success of accessing Session
     */
    private synchronized boolean loadTransactions() {
        if (!boundDataService){
            return false;
        }

        List<String> list = sessionAccess.getContent();
        if (list == null){
            return false;
        }

        List<Transaction> transactions = new ArrayList<>(list.size());
        for (String s : list) {
            JSONObject object;
            try {
                object = new JSONObject(s);
                Transaction transaction = new Transaction(object);
                transactions.add(transaction);
            } catch (JSONException e) {
                Log.e(TAG, "Transaction creation from JSON failed");
                return false;
            }
        }
        group.setTransactions(transactions);
        Log.d(TAG, "loaded transaction from session to group");
        return true;
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

        if (!boundDataService){
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

        group.addTransaction(transaction);

        // save transaction to file
        writeGroup();

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
                loadTransactions();
                writeGroup();
                updateViews();
                swipeRefreshLayout.setRefreshing(false);
            } // This is your code
        };
        mainHandler.post(myRunnable);
    }

}
