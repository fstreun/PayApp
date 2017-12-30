package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.fstreun.finance.DebtSolver;
import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.SimpleGroup;
import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.network.DataSync.Client.DataSyncSubscribeService;
import ch.ethz.inf.vs.fstreun.payapp.ListAdapters.ListSettleTransactionAdapter;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;

public class SettleActivity extends AppCompatActivity {

    private final String TAG = "## SettleActivity";

    // simple group expected to be in the intent
    public static final String KEY_SIMPLE_GROUP = "simple_group";
    private SimpleGroup mSimpleGroup;

    private Group mGroup = null;

    // Session Service communication
    private boolean bound;
    private DataService.SessionClientAccess sessionAccess;



    ListSettleTransactionAdapter adapter;
    List<ListSettleTransactionAdapter.CheckTransaction> transactionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle);

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


        adapter = new ListSettleTransactionAdapter(transactionList, this);
        ListView listViewTransactions = findViewById(R.id.listView_settleTransactions);
        listViewTransactions.setAdapter(adapter);
        listViewTransactions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // open transaction view
                Intent intent = new Intent(SettleActivity.this,
                        TransactionInfoActivity.class);
                intent.putExtra(TransactionInfoActivity.KEY_TRANSACTION, transactionList.get(position).transaction.toString());
                startActivity(intent);
            }
        });


        // Bind DataService
        Intent intentService = new Intent(this, DataService.class);
        bindService(intentService, connection, BIND_AUTO_CREATE);
        Log.d(TAG, "called bindService");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settle, menu);
        return true;
    }

    private void solveDebt(){
        if (mGroup != null){
            transactionList.clear();
            DebtSolver debtSolver = new DebtSolver(mGroup, sessionAccess.getUserID());
            List<Transaction> transactions = debtSolver.solvePrimitive(System.currentTimeMillis());
            for (Transaction t : transactions){
                transactionList.add(new ListSettleTransactionAdapter.CheckTransaction(t, true));
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void loadGroup(){
        if (bound){
            if (mGroup == null){
                mGroup = new Group(mSimpleGroup.sessionID);
            }

            List<Transaction> transactionList = new ArrayList<>();
            if (sessionAccess == null) return;
            for (String s : sessionAccess.getContent()){
                try {
                    transactionList.add(new Transaction(new JSONObject(s)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            mGroup.setTransactions(transactionList);
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
            if (name.getClassName().equals(DataService.class.getName())) {
                DataService.DataServiceBinder binder = (DataService.DataServiceBinder) service;

                sessionAccess = binder.getSessionClientAccess(mSimpleGroup.sessionID);
                if (sessionAccess != null) {
                    bound = true;

                    loadGroup();
                    solveDebt();
                } else Log.d(TAG, "sessionAccess is null");

                Log.d(TAG, "onServiceConnected: " + name.getClassName());

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected: " + name.getClassName());
            if (name.getClassName().equals(DataService.class.getName())) {
                bound = false;
            }
        }
    };


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.menu_positive:
                boolean success = applySettle();
                if (success){
                }else {
                    Toast.makeText(this, "Error occured", Toast.LENGTH_SHORT).show();
                }
                finish();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean applySettle(){
        if (transactionList.isEmpty()){
            Toast.makeText(this, "Nothing to settle.", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (bound){
            int count = 0;
            for (ListSettleTransactionAdapter.CheckTransaction checkTransaction : transactionList){
                if (checkTransaction.checked) {
                    Transaction transaction = checkTransaction.transaction;
                    try {
                        sessionAccess.add(transaction.toJson().toString());
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to add all transaction to session.", e);
                        return false;
                    }
                    count++;
                }
            }

            if (count > 0) {
                Toast.makeText(this, "Added new Transactions", Toast.LENGTH_SHORT).show();
            }
            return true;
        }


        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        return false;
    }
}
