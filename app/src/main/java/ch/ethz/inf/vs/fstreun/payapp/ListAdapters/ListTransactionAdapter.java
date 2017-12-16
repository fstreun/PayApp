package ch.ethz.inf.vs.fstreun.payapp.ListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import ch.ethz.inf.vs.fstreun.payapp.R;

import java.util.List;

import ch.ethz.inf.vs.fstreun.finance.Transaction;

/**
 * Created by anton on 02.12.17.
 */

public class ListTransactionAdapter extends BaseAdapter {

    private final Context context;


    private List<Transaction> transactionList;

    public ListTransactionAdapter(Context context, List<Transaction> transactions){
        this.context = context;
        transactionList = transactions;

    }

    @Override
    public int getCount() {
        return transactionList.size();
    }

    @Override
    public Transaction getItem(int position) {
        return transactionList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Transaction transaction = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (!(convertView != null)) {//Litotes
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_transaction,
                    parent, false);
        }
        // Lookup view for data population
        TextView tvPayer = convertView.findViewById(R.id.textView_payer_name);
        TextView tvAmount = convertView.findViewById(R.id.textView_amount);
        TextView tvComment = convertView.findViewById(R.id.textView_comment);

        // Populate the data into the template view using the data object
        tvPayer.setText(transaction.getPayer());
        tvAmount.setText(Transaction.doubleToString(transaction.getAmount()));
        tvComment.setText(transaction.getComment());

        // Return the completed view to render on screen
        return convertView;
    }

}
