package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.fstreun.finance.Transaction;

/**
 * Created by anton on 02.12.17.
 */

public class ListTransactionAdapter extends BaseAdapter {

    private final Context context;
    private int count;

    // filter
    private final String type;
    private final String participantName;


    private List<TransactionInList> viewTransactionList;

    public ListTransactionAdapter(Context context, String type, String participantName, List<Transaction> transactions){
        this.context = context;
        this.type = type;
        this.participantName = participantName;
        viewTransactionList = new ArrayList<>();

        //iterate over all transactions to get the necessary values
        TransactionInList til = new TransactionInList();
        for (Transaction t : transactions){
            til.payer = t.getPayer();
            til.amount = String.format("%1$.2f", t.getAmount());
            til.comment = t.getComment();
            viewTransactionList.add(til);
            count++;
        }

    }

    // the necessary values for the listView of transactions
    public class TransactionInList {
        public String payer;
        public String amount;
        public String comment;

    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public TransactionInList getItem(int position) {
        return viewTransactionList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        TransactionInList til = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            // TODO: this is not the correct way...
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_transaction,
                    parent, false);
        }
        // Lookup view for data population
        TextView tvPayer = convertView.findViewById(R.id.textView_payer);
        TextView tvAmount = convertView.findViewById(R.id.textView_amount);
        TextView tvComment = convertView.findViewById(R.id.textView_comment);

        // Populate the data into the template view using the data object
        tvPayer.setText(til.payer);
        tvAmount.setText(til.amount);
        tvComment.setText(til.comment);

        // Return the completed view to render on screen
        return convertView;
    }

    @Nullable
    @Override
    public CharSequence[] getAutofillOptions() {
        return new CharSequence[0];
    }
}
