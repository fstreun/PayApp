package ch.ethz.inf.vs.fstreun.payapp.ListAdapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import ch.ethz.inf.vs.fstreun.payapp.R;
import ch.ethz.inf.vs.fstreun.finance.SimpleGroup;

/**
 * Created by fabio on 12/5/17.
 */

public class ListSimpleGroupAdapter extends ArrayAdapter<SimpleGroup> {

    public ListSimpleGroupAdapter(@NonNull Context context, @NonNull List<SimpleGroup> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // Get the data item for this position
        final SimpleGroup group = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_simple_group, parent, false);
        }

        if (group == null){
            return convertView;
        }

        // Lookup view for data population
        TextView tvMain = convertView.findViewById(R.id.textView_main);

        tvMain.setText(group.groupID.toString());
        // Return the completed view to render on screen
        return convertView;

    }
}
