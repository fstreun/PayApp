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
 * Created by fabio on 12/6/17.
 */

public class ListSimpleNameAdapter extends ArrayAdapter<String> {
    public ListSimpleNameAdapter(@NonNull Context context, @NonNull List<String> objects) {
        super(context, R.layout.list_item_simple_name, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        final String string = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_simple_name, parent, false);
        }

        // Lookup view for data population
        TextView tvMain = convertView.findViewById(R.id.textView_main);

        tvMain.setText(string);
        // Return the completed view to render on screen
        return convertView;
    }
}
