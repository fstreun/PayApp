package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class GroupActivity extends AppCompatActivity {

    ListParticipantsAdapter adapter;

    public static final String KEY_GROUP_ID = "key_group_id";
    private Group group;

    //gui stuff
    TextView tvDeviceOwner;
    TextView tvOwnToPay;
    LinearLayout linLayOwn;

    //file stuff
    FileHelper fileHelper;

    UUID userUuid;

    String TAG = "###GroupActivity###";
    private UUID groupID;

    //Shared Preferences stuff
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String prefName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        //create fileHelper
        fileHelper = new FileHelper(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //initialize shared Prefs stuff
        prefName = getString(R.string.pref_name);
        sharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        //getting intent
        Intent intent = getIntent();
        try {
            String groupIdString = intent.getStringExtra(KEY_GROUP_ID);
            if(groupIdString != null && !groupIdString.isEmpty()) {
                groupID = UUID.fromString(groupIdString);
                group = loadGroup();
            } else {
                //todo: take this out as soon as createGroupActivity is implemented
                Log.d(TAG, "creating random uuid as groupID");
                groupID = UUID.randomUUID();
                group = new Group(groupID);
                fileHelper.writeToFile(getString(R.string.path_groups), groupID.toString(),
                        group.toString());
                try {
                    group = loadGroup();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    group = new Group(UUID.randomUUID());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "not possible to create group", Toast.LENGTH_SHORT);

        }
        //todo: get name of device owner
        group.setDeviceOwner("Toni");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createTransaction();
            }
        });


        adapter = new ListParticipantsAdapter(this, group);

        ListView listView = findViewById(R.id.listView_main);
        listView.setAdapter(adapter);

        //textView device owner
        tvDeviceOwner = findViewById(R.id.textView_device_owner);
        tvOwnToPay = findViewById(R.id.textView_ownToPay);
        linLayOwn = findViewById(R.id.lin_lay_deviceOwner);

        //todo: get user UUID from file (we are now getting a random one)
        userUuid = UUID.randomUUID();

        //update view
        updateViews();
    }

    private Group loadGroup(){
        Group g = null;
        try {
            JSONObject groupJson = new JSONObject(fileHelper.readFromFile(
                    getString(R.string.path_groups), groupID.toString()));
            Log.d(TAG, "reading from file: " + groupID.toString());
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
                //TODO: add information (SessionID's to be shared) to the intent
                Intent intent = new Intent(this, PublishGroupActivity.class);
                startActivity(intent);
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
        String payer = sharedPreferences.getString(getString(R.string.pref_payer_lru),
                group.getDeviceOwner());

        //get initially checked participants from shared prefs
        Set<String> checkedPartiSet = sharedPreferences.getStringSet(
                getString(R.string.pref_involved_lru), null);
        String[] checkedParticipants = checkedPartiSet.toArray(new String[checkedPartiSet.size()]);

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

                // Transaction was finished with save
                double amount = data.getDoubleExtra(TransactionCreationActivity.KEY_AMOUNT, 0.0);
                String comment = data.getStringExtra(TransactionCreationActivity.KEY_COMMENT);
                String payer = data.getStringExtra(TransactionCreationActivity.KEY_PAYER);
                String[] involvedString = data.getStringArrayExtra(TransactionCreationActivity.KEY_PARTICIPANTS_INVOLVED);
                List<String> involved = Arrays.asList(involvedString);

                //store data to shared prefs for next transaction creation
                editor.putString(getString(R.string.pref_payer_lru), payer);
                editor.putStringSet(getString(R.string.pref_involved_lru), new HashSet<>(involved));
                editor.apply();

                //Create transaction from the intent data
                Transaction transaction = new Transaction(userUuid, payer, involved, amount, comment);
                group.addTransaction(transaction);

                // save transaction to file
                fileHelper.writeToFile(getString(R.string.path_groups), groupID.toString(),
                        group.toString());
                Log.d(TAG, "writing to file: " + groupID.toString());

                Toast.makeText(this, "Saved Transaction", Toast.LENGTH_SHORT).show();
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

            //todo: make values appear like 49.95 (2 digits after the dot)
            tvOwnToPay.setText(String.valueOf(group.toPay(defPart)));

        }
        adapter.notifyDataSetChanged();
    }
}
