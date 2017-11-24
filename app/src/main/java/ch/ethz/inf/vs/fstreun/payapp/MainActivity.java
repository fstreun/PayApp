package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.fstreun.network.SessionPublishService;
import ch.ethz.inf.vs.fstreun.network.SessionSubscribeService;

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
    }

    public void buttonCreateClicked(View view){
        //TODO: create finance group and open GroupActivity
        Log.d("MainActivity", "buttonCreateClicked()");
        Intent intent = new Intent(this, SessionPublishService.class);
        startService(intent);
    }

    public void buttonJoinClicked(View view){
        //TODO: open JoinGroupActivity
        Log.d("MainActivity", "buttonJoinClicked()");
        Intent intent = new Intent(this, SessionSubscribeService.class);
        startService(intent);
    }
}
