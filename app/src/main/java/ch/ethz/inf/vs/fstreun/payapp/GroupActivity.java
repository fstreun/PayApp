package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class GroupActivity extends AppCompatActivity {

    ListParticipantsAdapter adapter;
    ArrayList<ListParticipantsAdapter.Participant> participants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createTransaction();
            }
        });


        ListParticipantsAdapter.Participant p = new ListParticipantsAdapter.Participant();
        p.Name = "First"; p.Account = "+16";
        participants.add(p);

        adapter = new ListParticipantsAdapter(this, participants);

        ListView listView = findViewById(R.id.listView_main);
        listView.setAdapter(adapter);
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
                String desciption = data.getStringExtra(TransactionCreationActivity.KEY_DESCRIPTION);
                String payer = data.getStringExtra(TransactionCreationActivity.KEY_PAYER);
                String[] involved = data.getStringArrayExtra(TransactionCreationActivity.KEY_PARTICIPANTS_INVOLVED);
                //TODO: Create transaction from the intent data.
                Toast.makeText(this, "Saved Transaction", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
