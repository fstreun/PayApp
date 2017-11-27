package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public class TransactionCreationActivity extends AppCompatActivity {

    public final static String KEY_PAYER = "payer"; // String in and out
    public final static String KEY_AMOUNT = "amount"; // double in and out
    public final static String KEY_DESCRIPTION = "description"; // String in and out
    public final static String KEY_PARTICIPANTS = "participants"; // String[] in
    public final static String KEY_PARTICIPANTS_CHECKED = "checked"; // boolean[] in
    public final static String KEY_PARTICIPANTS_INVOLVED = "involved"; // String[] out

    ListParticipantsCheckAdapter adapter;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_creation);

        // TODO: just test data
        ArrayList<ListParticipantsCheckAdapter.ParticipantCheck> data = new ArrayList<>();
        data.add(new ListParticipantsCheckAdapter.ParticipantCheck("Kaan"));
        data.add(new ListParticipantsCheckAdapter.ParticipantCheck("Toni"));
        data.add(new ListParticipantsCheckAdapter.ParticipantCheck("Fabio"));

        adapter = new ListParticipantsCheckAdapter(this, data);
        ListView listView = findViewById(R.id.listView_participants);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.getItem(position).changeCheck();
                adapter.notifyDataSetChanged();
            }
        });

        //TODO: just test data
        ArrayList<String> spinnerData = new ArrayList<>();
        spinnerData.add("Kaan");
        spinnerData.add("Toni");
        spinnerData.add("Fabio");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerData);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = findViewById(R.id.spinner);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(1);
    }


    private double getAmount(){
        // TODO
        return 0.0;
    }

    private String getPayer(){
        // TODO
        return (String) spinner.getSelectedItem();
    }

    private String getDescription(){
        // TODO
        return "Description";
    }

    private String[] getInvolved(){
        // TODO (with the adapter)
        return new String[0];
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_transaction_creation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_save:
                //TODO: close activity and transfer transaction information over intent.
                Intent intent = new Intent();
                intent.putExtra(KEY_AMOUNT, getAmount());
                intent.putExtra(KEY_PAYER, getPayer());
                intent.putExtra(KEY_DESCRIPTION, getDescription());
                intent.putExtra(KEY_PARTICIPANTS_INVOLVED, getInvolved());
                setResult(RESULT_OK, intent);
                finish();
                return true;

            case R.id.menu_addName:
                //TODO: open dialog to add name and add result to both lists (payer and participants)
                Toast.makeText(this, "User Added", Toast.LENGTH_SHORT).show();
                return true;

            case android.R.id.home:
                Toast.makeText(this, "home clicked", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
