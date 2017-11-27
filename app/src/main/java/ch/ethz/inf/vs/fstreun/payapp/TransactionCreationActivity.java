package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

    ListParticipantsCheckAdapter adapterParticipants;
    Spinner spinnerPayer;
    EditText editTextAmount;
    EditText editTextComment;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_creation);

        //reading participants list from intent


        //find views for editTexts
        editTextAmount = findViewById(R.id.editText_amount);
        editTextComment = findViewById(R.id.editText_comment);

        // TODO: just test data
        ArrayList<ListParticipantsCheckAdapter.ParticipantCheck> data = new ArrayList<>();
        data.add(new ListParticipantsCheckAdapter.ParticipantCheck("Kaan"));
        data.add(new ListParticipantsCheckAdapter.ParticipantCheck("Toni"));
        data.add(new ListParticipantsCheckAdapter.ParticipantCheck("Fabio"));

        adapterParticipants = new ListParticipantsCheckAdapter(this, data);
        ListView listView = findViewById(R.id.listView_participants);
        listView.setAdapter(adapterParticipants);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapterParticipants.getItem(position).changeCheck();
                adapterParticipants.notifyDataSetChanged();
            }
        });

        //TODO: just test data
        ArrayList<String> spinnerData = new ArrayList<>();
        spinnerData.add("Kaan");
        spinnerData.add("Toni");
        spinnerData.add("Fabio");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerData);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPayer = findViewById(R.id.spinner);
        spinnerPayer.setAdapter(spinnerAdapter);
        spinnerPayer.setSelection(1);
    }


    private Double getAmount(){
        String amountString = String.valueOf(editTextAmount.getText());
        if (amountString != null) {
            try {

                //case: everything ok
                double result = Double.parseDouble(amountString);
                return result;
            }catch (NumberFormatException e) {

                // case: get amount did not work
                return null;
            }
        }

        // case: get amount did not work
        return null;
    }

    private String getPayer(){
        return (String) spinnerPayer.getSelectedItem();
    }

    private String getComment(){
        String comment = String.valueOf(editTextComment.getText());
        if (comment.isEmpty() || comment == null){
            return "no comment";
        } else {
            return comment;
        }
    }

    private String[] getInvolved(){
        int n = adapterParticipants.getCount();
        String[] result = new String[n];
        int j = 0;

        //get items as string array
        for (int i =0; i<n; i++){
            ListParticipantsCheckAdapter.ParticipantCheck item = adapterParticipants.getItem(i);
            if(item.checked) {
                result[j++] = item.name;
            }
        }

        //shorten array
        String[] result2 = new String[j];
        for (int i=0; i<j; i++){
            result2[i] = result[i];
        }

        return result2;
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
                Intent intent = new Intent();

                //case: wrong amount
                Double amount = getAmount();
                if (amount == null){
                    Toast.makeText(this, "wrong amount input format (try eg 5.85)",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }


                //case: payer was null
                String payer = getPayer();
                if (payer == null || payer.isEmpty()){
                    Toast.makeText(this, "somehow the payer was not specified",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                //case: no involved participants or involved == null
                String[] involved = getInvolved();
                if (involved == null){
                    Toast.makeText(this, "involved string array is null",
                            Toast.LENGTH_SHORT).show();
                    return true;
                } else if (involved.length < 1){
                    Toast.makeText(this, "at least one participant must be involved",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }

                //putting values
                intent.putExtra(KEY_AMOUNT, amount);
                intent.putExtra(KEY_PAYER, payer);
                intent.putExtra(KEY_DESCRIPTION, getComment());
                intent.putExtra(KEY_PARTICIPANTS_INVOLVED, getInvolved());
                setResult(RESULT_OK, intent);
                finish();
                return true;

            case R.id.menu_addName:
                //TODO: open dialog to add name and add result to both lists (payer and participants)
                Toast.makeText(this, "User Added", Toast.LENGTH_SHORT).show();
                return true;

            case android.R.id.home:
                Toast.makeText(this, "cancelled transaction creation",
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}
