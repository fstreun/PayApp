package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by fabio on 11/24/17.
 */

public class ListParticipantsAdapter extends ArrayAdapter<ListParticipantsAdapter.Participant> {

    public static class Participant {
        public String Name;
        public String Account;
    }

    public ListParticipantsAdapter(Context context, ArrayList<Participant> participants){
        super(context, R.layout.list_item_participant, participants);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        Participant participant = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            // TODO: this is not the correct way...
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_participant, parent, false);
        }
        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.textView_name);
        TextView tvAccount = convertView.findViewById(R.id.textView_account);
        // Populate the data into the template view using the data object
        tvName.setText(participant.Name);
        tvAccount.setText(participant.Account);
        // Return the completed view to render on screen
        return convertView;
    }
}
