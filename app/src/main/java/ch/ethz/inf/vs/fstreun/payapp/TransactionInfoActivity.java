package ch.ethz.inf.vs.fstreun.payapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.payapp.ListAdapters.ListSimpleNameAdapter;

public class TransactionInfoActivity extends AppCompatActivity {

    public static final String KEY_TRANSACTION = "transaction";

    TextView tvComment, tvAmount, tvPayer, tvDatestamp, tvTimestamp;
    ListSimpleNameAdapter adapter;

    Transaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_info);

        // get transaction from intent
        try {
            transaction = new Transaction(new JSONObject(getIntent().getStringExtra(KEY_TRANSACTION)));
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "not possible to load transaction :(", Toast.LENGTH_SHORT);
            onDestroy();
        }

        // set title
        setTitle("Transaction Info");
        // connect view stuff
        tvComment = findViewById(R.id.textView_info_comment);
        tvAmount = findViewById(R.id.textView_info_amount);
        tvPayer = findViewById(R.id.textView_info_payer);
        tvDatestamp = findViewById(R.id.textView_info_datestamp);
        tvTimestamp = findViewById(R.id.textView_info_timestamp);

        ListView tvInvolved = findViewById(R.id.listView_info_involved);
        adapter = new ListSimpleNameAdapter(this, transaction.getInvolved());
        tvInvolved.setAdapter(adapter);

        // set transaction info to screen
        tvComment.setText(transaction.comment);
        tvAmount.setText(Transaction.doubleToString(transaction.amount));
        tvPayer.setText(transaction.payer);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd. MMM. yyyy");
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date(transaction.timestamp);
        String dateAsString = simpleDateFormat.format(date);
        tvDatestamp.setText(dateAsString);
        String timeAsString = simpleTimeFormat.format(date);
        tvTimestamp.setText(timeAsString);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
