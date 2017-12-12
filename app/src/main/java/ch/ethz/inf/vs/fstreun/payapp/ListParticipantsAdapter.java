package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import ch.ethz.inf.vs.fstreun.finance.Group;

/**
 * Created by fabio on 11/24/17.
 */

public class ListParticipantsAdapter extends BaseAdapter{

    private final Context context;
    private final Group group;

    public static class Participant {
        public String name;
        public double toPay;
        public String getToPay(){
            return String.format("%1$.2f", toPay);
        }
    }


    public ListParticipantsAdapter(Context context, Group group){
        super();

        this.group = group;
        this.context = context;
    }

    @Override
    public int getCount() {
        return group.numParticipants();
    }

    @Override
    public Participant getItem(int position) {
        Participant result = new Participant();
        result.name = group.getParticipants().get(position);
        result.toPay = group.toPay(result.name);
        return result;
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
        TextView tvToPay = convertView.findViewById(R.id.textView_toPay);
        // Populate the data into the template view using the data object
        tvName.setText(participant.name);


        // TODO: choose the correct colors
        tvToPay.setText(participant.getToPay());
        Double toPay = participant.toPay;
        int compare = toPay.compareTo(0.0);
        if (compare < 0){
            // toPay is a negativ number
            tvToPay.setTextColor(context.getResources().getColor(R.color.colorAccent));
        }else if (compare > 0){
            // toPay is a positiv number
            tvToPay.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        }else {
            // toPay is equals to 0.0
            tvToPay.setTextColor(context.getResources().getColor(R.color.colorGrey));
        }

        // Return the completed view to render on screen
        return convertView;
    }


}
