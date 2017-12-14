package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ch.ethz.inf.vs.fstreun.finance.Participant;
import ch.ethz.inf.vs.fstreun.finance.Transaction;

/**
 * Created by fabio on 11/24/17.
 */

public class ListParticipantsAdapter extends BaseAdapter{

    private final Context context;
    private final List<Participant> participants;


    public ListParticipantsAdapter(Context context, List<Participant> participants){
        super();

        this.participants = participants;
        this.context = context;
    }

    @Override
    public int getCount() {
        return participants.size();
    }

    @Override
    public Participant getItem(int position) {
        return participants.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        Participant participant = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_participant, parent, false);
        }
        // Lookup view for data population
        TextView tvName = convertView.findViewById(R.id.textView_name);
        TextView tvSpent = convertView.findViewById(R.id.textView_spent);
        TextView tvOwes = convertView.findViewById(R.id.textView_owes);
        TextView tvCredit = convertView.findViewById(R.id.textView_credit);
        // Populate the data into the template view using the data object
        tvName.setText(participant.name);


        // put values into TextViews
        tvSpent.setText(Transaction.doubleToString(participant.getSpent()));
        tvOwes.setText(Transaction.doubleToString(participant.getTotalInvolved()));
        Double credit = participant.getCredit();
        tvCredit.setText(Transaction.doubleToString(credit));
        int compare = credit.compareTo(0.0);
        if (compare < 0){
            // toPay is a negativ number
            tvCredit.setTextColor(context.getResources().getColor(R.color.colorAccent));
        }else if (compare > 0){
            // toPay is a positiv number
            tvCredit.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
        }else {
            // toPay is equals to 0.0
            tvCredit.setTextColor(context.getResources().getColor(R.color.colorGrey));
        }

        // Return the completed view to render on screen
        return convertView;
    }

}
