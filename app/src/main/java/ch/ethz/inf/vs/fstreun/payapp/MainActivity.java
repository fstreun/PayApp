package ch.ethz.inf.vs.fstreun.payapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ArrayAdapter<String> adapter;
    List<String> groups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        groups = new ArrayList<String>();
        groups.add("first");
        groups.add("second");
        groups.add("third");

        adapter = new ListGroupAdapter(this, groups);

        ListView listViewGroup = findViewById(R.id.listView_groups);
        listViewGroup.setAdapter(adapter);

        final Button buttonCreate = findViewById(R.id.button_create);
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonCreateClicked();
            }
        });

        final Button buttonJoin = findViewById(R.id.button_join);
        buttonJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonJoinClicked();
            }
        });

    }

    private void buttonCreateClicked(){
        //TODO: create finance group and open GroupActivity
    }

    private void buttonJoinClicked(){
        //TODO: open JoinGroupActivity
    }
}
