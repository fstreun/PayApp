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
        public String toPay;
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
        result.toPay = String.format("%1$.2f", group.toPay(result.name));
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
        tvToPay.setText(participant.toPay);
        // Return the completed view to render on screen
        return convertView;
    }


}
