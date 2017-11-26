package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by fabio on 11/26/17.
 *
 */

public class ListParticipantsCheckAdapter extends ArrayAdapter<ListParticipantsCheckAdapter.ParticipantCheck>{

    public static class ParticipantCheck{
        public String name;
        public boolean checked = false;

        public ParticipantCheck(String name){
            this.name = name;
        }

        public void changeCheck(){
            checked = !checked;
        }
    }

    public ListParticipantsCheckAdapter(Context context, ArrayList<ParticipantCheck> participants){
        super(context, R.layout.list_item_participant_check, participants);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        ListParticipantsCheckAdapter.ParticipantCheck participant = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            // TODO: this is not the correct way...
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_participant_check, parent, false);
        }
        // Lookup view for data population
        TextView tvName = convertView.findViewById(R.id.textView_name);
        CheckBox checkBox = convertView.findViewById(R.id.checkBox);
        // Populate the data into the template view using the data object
        tvName.setText(participant.name);
        checkBox.setChecked(participant.checked);
        // Return the completed view to render on screen
        return convertView;
    }

}