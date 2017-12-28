package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.fstreun.finance.DebtSolver;
import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.network.DataSync.Client.DataSyncSubscribeService;
import ch.ethz.inf.vs.fstreun.payapp.ListAdapters.ListSettleTransactionAdapter;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;

public class SettleActivity extends AppCompatActivity {

    private final String TAG = "## SettleActivity";

    // Session Service communication
    private boolean bound;
    private DataService.SessionClientAccess sessionAccess;

    // Group which should be settled
    static Group sGroup = null;

    ListSettleTransactionAdapter adapter;
    List<Transaction> transactionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle);

        if (sGroup == null){
            // data not received
            Log.e(TAG, "Group data not loaded to the Activity.");
            Toast.makeText(this, "failed to load Group", Toast.LENGTH_SHORT).show();
            return;
        }

        DebtSolver debtSolver = new DebtSolver(sGroup,null);

        transactionList.addAll(debtSolver.solvePrimitive(System.currentTimeMillis()));

        adapter = new ListSettleTransactionAdapter(transactionList, this);
        ListView listViewTransactions = findViewById(R.id.listView_settleTransactions);
        listViewTransactions.setAdapter(adapter);
        listViewTransactions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // open transaction view
                Intent intent = new Intent(SettleActivity.this,
                        TransactionInfoActivity.class);
                intent.putExtra(TransactionInfoActivity.KEY_TRANSACTION, transactionList.get(position).toString());
                startActivity(intent);
            }
        });


        // TODO: bind DataService
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

                sessionAccess = binder.getSessionClientAccess(sGroup.getSessionID());
                if (sessionAccess != null) {
                    bound = true;
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
}
