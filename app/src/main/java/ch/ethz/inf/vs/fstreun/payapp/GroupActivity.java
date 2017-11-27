package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.Transaction;

public class GroupActivity extends AppCompatActivity {

    ListParticipantsAdapter adapter;

    public static final String KEY_GROUP_ID = "key_group_id";
    private Group group;

    TextView tvDefaultParticipant;
    TextView tvDefToPay;
    LinearLayout linLayDef;

    UUID userUuid;

    String TAG = "GroupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //getting intent
        Intent intent = getIntent();
        try {
            group = loadGroup(intent.getStringExtra(KEY_GROUP_ID));
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "not possible to create group", Toast.LENGTH_SHORT);
        }
        //todo: get name of device owner (default payer)
        group.setDefaultParticipant("Toni");

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

        //textView default participant
        tvDefaultParticipant = findViewById(R.id.textView_defaultParticipant);
        tvDefToPay = findViewById(R.id.textView_defToPay);
        linLayDef = findViewById(R.id.lin_lay_defaultParticipant);

        //todo: get user UUID from file (we are now getting a random one
        userUuid = UUID.randomUUID();

        //update view
        updateViews();
    }

    private Group loadGroup(String stringExtra) throws JSONException {
        //todo: load group from fileHelper
        String content = emptyGroup();
        Group g = new Group(new JSONObject(content));
        return g;
    }


    /**
     * for testing
     * @return empty group (i.e. no transactions, no participants, random sessionId)
     */
    private String emptyGroup() throws JSONException {
        JSONObject groupJson = new JSONObject();
        groupJson.put(Group.SESSION_ID_KEY, UUID.randomUUID().toString());
        JSONArray transArray = new JSONArray();
        JSONArray partArray = new JSONArray();
        groupJson.put(Group.TRANSACTIONS_KEY, transArray);
        groupJson.put(Group.PARTICIPANTS_KEY, partArray);

        return groupJson.toString();
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
        startActivityForResult(intent, CREATE_TRANSACTION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CREATE_TRANSACTION_REQUEST){
            if (resultCode == RESULT_OK){
                // Transaction was finished with save
                double amount = data.getDoubleExtra(TransactionCreationActivity.KEY_AMOUNT, 0.0);
                String comment = data.getStringExtra(TransactionCreationActivity.KEY_DESCRIPTION);
                String payer = data.getStringExtra(TransactionCreationActivity.KEY_PAYER);
                String[] involvedString = data.getStringArrayExtra(TransactionCreationActivity.KEY_PARTICIPANTS_INVOLVED);
                List<String> involved = Arrays.asList(involvedString);

                //TODO: Create transaction from the intent data and save to file
                Transaction transaction = new Transaction(userUuid, payer, involved, amount, comment);
                group.addTransaction(transaction);

                Toast.makeText(this, "Saved Transaction", Toast.LENGTH_SHORT).show();
                updateViews();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateViews() {
        String defPart = group.getDefaultParticipant();
        if(defPart == null){
            linLayDef.setVisibility(View.GONE);
        } else {
            linLayDef.setVisibility(View.VISIBLE);
            tvDefaultParticipant.setText(defPart);
            tvDefToPay.setText(String.valueOf(group.toPay(defPart)));

        }
        adapter.notifyDataSetChanged();
    }
}
