package ch.ethz.inf.vs.fstreun.payapp.ListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.payapp.R;

/**
 * Created by fabio on 12/28/17.
 */

public class ListSettleTransactionAdapter extends BaseAdapter {

    List<Transaction> data;
    Context context;

    /**
     *
     * @param data: List of transactions with exactly one person involved
     * @param context
     */
    public ListSettleTransactionAdapter(List<Transaction> data, Context context){
        this.data = data;
        this.context = context;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Transaction getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Transaction transaction = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_settle_transaction,
                    parent, false);
        }
        // Lookup view for data population
        TextView tvPayer = convertView.findViewById(R.id.textView_payer);
        TextView tvReceiver = convertView.findViewById(R.id.textView_receiver);
        TextView tvAmount = convertView.findViewById(R.id.textView_amount);

        // Populate the data into the template view using the data object
        tvPayer.setText(transaction.getPayer());
        tvReceiver.setText(transaction.getInvolved().get(0));
        tvAmount.setText(Transaction.doubleToString(transaction.getAmount()));

        // Return the completed view to render on screen
        return convertView;
    }
}
