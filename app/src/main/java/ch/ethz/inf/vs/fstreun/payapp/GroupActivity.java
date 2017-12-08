package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class GroupActivity extends AppCompatActivity {

    String TAG = "###GroupActivity###";

    public static final String KEY_RESULT_CODE = "key_result_type";
    public static final int CODE_DEFAULT = 0;
    public static final int CODE_DELETE = 1;

    public static final String KEY_SIMPLE_GROUP = "simple_group";
    private SimpleGroup mSimpleGroup;

    private Group group;

    private boolean bound;
    private final Object lock = new Object();
    private DataService.SessionClientAccess sessionAccess;

    private ListParticipantsAdapter adapter;


    //gui stuff
    TextView tvDeviceOwner;
    TextView tvOwnToPay;
    LinearLayout linLayOwn;

    //file stuff
    FileHelper fileHelper;

    //Shared Preferences for specific group
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String prefName;
    String payerKey, involvedKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        // add UpButton
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //create fileHelper
        fileHelper = new FileHelper(this);

        //getting intent
        Intent intent = getIntent();
        String stringSimpleGroup = intent.getStringExtra(KEY_SIMPLE_GROUP);
        if(stringSimpleGroup != null && !stringSimpleGroup.isEmpty()) {
            try {
                JSONObject object = new JSONObject(stringSimpleGroup);
                mSimpleGroup = new SimpleGroup(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "group: " + stringSimpleGroup);

            group = loadGroup();
        } else {
            Toast.makeText(this, "not possible to create group", Toast.LENGTH_SHORT).show();
            return;
        }

        //set group name as title
        setTitle(mSimpleGroup.groupName);

        //set shared prefsKeys
        payerKey = getString(R.string.pref_payer_lru);
        involvedKey = getString(R.string.pref_involved_lru);

        //initialize shared Prefs for this group
        prefName = getString(R.string.pref_name) + mSimpleGroup.groupID;
        sharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createTransaction();
            }
        });


        adapter = new ListParticipantsAdapter(this, group);

        ListView listView = findViewById(R.id.listView_main);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent1 = new Intent(GroupActivity.this,
                        TransactionListActivity.class);
                intent1.putExtra(TransactionListActivity.KEY_FILTER_TYPE,
                        getString(R.string.filter_paid_by_name));
                intent1.putExtra(TransactionListActivity.KEY_PARTICIPANT,
                        adapter.getItem(position).name);
                intent1.putExtra(TransactionListActivity.KEY_GROUP_NAME, mSimpleGroup.groupName);
                intent1.putExtra(TransactionListActivity.KEY_GROUP_ID, mSimpleGroup.groupID.toString());

                GroupActivity.this.startActivity(intent1);
            }
        });

        //textView device owner
        tvDeviceOwner = findViewById(R.id.textView_device_owner);
        tvOwnToPay = findViewById(R.id.textView_ownToPay);
        linLayOwn = findViewById(R.id.lin_lay_deviceOwner);

        //update view
        updateViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind DataService
        Intent intent = new Intent(this, DataService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        Log.d(TAG, "called bindService");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from service
        if (bound){
            unbindService(connection);
            Log.d(TAG, "called unbindService");
            bound = false;
        }
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (name.getClassName().equals(DataService.class.getName())){
                DataService.LocalBinder binder = (DataService.LocalBinder) service;
                // TODO check if group is null:
                sessionAccess = binder.getSessionClientAccess(group.getSessionID());
                bound = true;
                Log.d(TAG, "onServiceConnected: " + name.getClassName());
                // if a open transaction exists, try to store it!
                storeOpenTransaction();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected");
            bound = false;
        }
    };



    private Group loadGroup(){
        Group g = null;
        try {
            JSONObject groupJson = new JSONObject(fileHelper.readFromFile(
                    getString(R.string.path_groups), mSimpleGroup.groupID.toString()));
            Log.d(TAG, "reading from file: " + mSimpleGroup.groupID.toString());
            g = new Group(groupJson);
        } catch (Exception e) {
            e.printStackTrace();
            //if fail return null
            Toast.makeText(this, "no group loaded", Toast.LENGTH_SHORT);
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
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_publishGroup:
                Intent intent = new Intent(this, PublishGroupActivity.class);
                try {
                    intent.putExtra(PublishGroupActivity.KEY_SIMPLEGROUP, mSimpleGroup.toJSON().toString());
                } catch (JSONException e) {
                    // failed to create SimpleGroup JSON
                }
                startActivity(intent);
                return true;

            case R.id.menu_showAllTransactions:
                //case view all transactions
                Intent intent1 = new Intent(GroupActivity.this,
                        TransactionListActivity.class);
                intent1.putExtra(TransactionListActivity.KEY_FILTER_TYPE,
                        getString(R.string.filter_no_filter));
                intent1.putExtra(TransactionListActivity.KEY_GROUP_NAME, mSimpleGroup.groupName);
                intent1.putExtra(TransactionListActivity.KEY_GROUP_ID, mSimpleGroup.groupID.toString());

                GroupActivity.this.startActivity(intent1);
                return true;

            case R.id.menu_setMainParticipant:
                chooseMainParticipant();
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
        // GETTING INFORMATION from shared prefs
        //----------------------------------------------------------------------------

        //getting participants of group into String array
        String[] participants = new String[0];
        if (group != null){
            int n = group.numParticipants();
            participants = new String[n];
            List<String> participantsList = group.getParticipants();
            for (int i=0; i<n; i++){
                participants[i] = participantsList.get(i);
            }
        }

        //get LRU payer from shared prefs (default value is the deviceOwner)
        String payer = sharedPreferences.getString(payerKey,
                group.getDeviceOwner());

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

                //store listData to shared prefs for next transaction creation
                editor.putString(payerKey, payer);
                editor.putStringSet(involvedKey, new HashSet<>(involved));
                editor.apply();

                // transaction can only be stored after service was bound.
                openTransaction = data.getExtras();
                // try to store transaction (it will fail)
                storeOpenTransaction();
                updateViews();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateViews() {
        String defPart = group.getDeviceOwner();
        if(defPart == null){
            linLayOwn.setVisibility(View.GONE);
        } else {
            linLayOwn.setVisibility(View.VISIBLE);
            tvDeviceOwner.setText(defPart);

            //make values appear like 49.99 (2 digits after the dot)
            double toPay = group.toPay(defPart);
            String toPayString = Transaction.doubleToString(toPay);
            tvOwnToPay.setText(toPayString);

        }
        adapter.notifyDataSetChanged();
    }

    private void chooseMainParticipant(){
        List<String> partList = group.getParticipants();
        int current = partList.indexOf(group.getDeviceOwner());
        if (current < 0){
            current = group.numParticipants();
        }
        final String[] participants = group.getParticipants().toArray(new String[group.numParticipants()+1]);
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
    }

    private void setMainParticipant(String participant){
        if (participant.equalsIgnoreCase("(None)")){
            group.setDeviceOwner(null);
        }else {
            group.setDeviceOwner(participant);
        }
        updateViews();

        writeGroup();
        Log.d(TAG, "writing to file: " + mSimpleGroup.groupID.toString());
    }

    private void writeGroup() {
        fileHelper.writeToFile(getString(R.string.path_groups), mSimpleGroup.groupID.toString(), group.toString());
    }

    private void showDeleteGroupDialog(){
        //todo: fix bug that the shared prefs are deleted after force closing the app
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete group: " + mSimpleGroup.groupName + " ?");
        builder.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteGroup();
                return;
            }
        });
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        builder.create().show();

        return;
    }

    /**
     * delets the current group and al its dependence
     */
    private void deleteGroup(){
        //TODO: sessions

        // remove group file
        fileHelper.removeFile(getString(R.string.path_groups), mSimpleGroup.groupID.toString());

        Intent intent = new Intent();
        intent.putExtra(KEY_RESULT_CODE, CODE_DELETE);
        try {
            intent.putExtra(KEY_SIMPLE_GROUP, mSimpleGroup.toJSON().toString());
        } catch (JSONException e) {
            //TODO: handle exception
        }
        setResult(RESULT_OK, intent);
        finish();
    }


    /**
     * transaction which was not been saved yet
     */
    Bundle openTransaction = null;
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

        //store listData to shared prefs for next transaction creation
        editor.putString(payerKey, payer);
        editor.putStringSet(involvedKey, new HashSet<>(involved));
        editor.apply();

        UUID userUuid = sessionAccess.getUserID();
        if(userUuid == null){
            return false;
        }
        Transaction transaction = new Transaction(userUuid, payer, involved, amount,
                System.currentTimeMillis(), comment);


        group.addTransaction(transaction);

        // save transaction to file
        writeGroup();
        Log.d(TAG, "writing to file: " + mSimpleGroup.groupID.toString());

        Toast.makeText(this, "Saved Transaction", Toast.LENGTH_SHORT).show();
        openTransaction = null;
        return true;
    }
}
