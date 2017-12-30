package ch.ethz.inf.vs.fstreun.payapp.ListAdapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.fstreun.finance.Transaction;
import ch.ethz.inf.vs.fstreun.payapp.R;

/**
 * Created by fabio on 12/28/17.
 */

public class ListSettleTransactionAdapter extends BaseAdapter {

    public static class CheckTransaction{
        public final Transaction transaction;
        public boolean checked;

        public CheckTransaction(Transaction transaction){
            this.transaction = transaction;
            this.checked = false;
        }
        public CheckTransaction(Transaction transaction, boolean checked){
            this.transaction = transaction;
            this.checked = checked;
        }
    }

    List<CheckTransaction> data;
    Context context;

    /**
     *
     * @param data: List of transactions with exactly one person involved
     * @param context
     */
    public ListSettleTransactionAdapter(List<CheckTransaction> data, Context context){
        this.data = data;
        this.context = context;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public CheckTransaction getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final CheckTransaction checkTransaction = getItem(position);
        Transaction transaction = checkTransaction.transaction;

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_settle_transaction,
                    parent, false);
        }

        // set checkbox listener
        CheckBox checkBox = convertView.findViewById(R.id.checkBox);
        checkBox.setChecked(checkTransaction.checked);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkTransaction.checked = isChecked;
            }
        });


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
