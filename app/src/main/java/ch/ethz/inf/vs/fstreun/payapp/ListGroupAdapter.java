package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by fabio on 11/24/17.
 */

public class ListGroupAdapter extends ArrayAdapter<SimpleGroup> {

    public ListGroupAdapter(Context context, List<SimpleGroup> content){
        super(context, R.layout.list_item_group, content);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // Get the data item for this position
        String groupName = getItem(position).groupName;
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_group, parent, false);
        }
        // Lookup view for data population
        TextView tvName = (TextView) convertView.findViewById(R.id.textView_groupName);
        // Populate the data into the template view using the data object
        tvName.setText(groupName);
        // Return the completed view to render on screen
        return convertView;

    }
}
