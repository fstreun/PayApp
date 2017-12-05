package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.network.SimpleGroup;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class JoinGroupActivity extends AppCompatActivity {

    DataService mService;
    boolean mBound;

    public static final String KEY_GROUP_ID = "group_id"; //Not empty unique String out
    public static final String KEY_GROUP_NAME = "group_name"; //Not empty (unique?) String out

    EditText editTextGroupSecret;

    List<SimpleGroup> groupList = new ArrayList<SimpleGroup>(3);
    ListSimpleGroupAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        editTextGroupSecret = findViewById(R.id.edittext_group_secret);

        adapter = new ListSimpleGroupAdapter(this, groupList);
        ListView listViewFoundGroup = findViewById(R.id.listView_foundGroups);
        listViewFoundGroup.setAdapter(adapter);

        listViewFoundGroup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                join(groupList.get(position));
            }
        });

        final Button buttonJoin = findViewById(R.id.button_search);
        buttonJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonJoinClicked();
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, DataService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
        mBound = false;
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            DataService.LocalBinder binder = (DataService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };




    private void buttonJoinClicked(){
        groupList.clear();
        adapter.notifyDataSetChanged();

        String groupHint = editTextGroupSecret.getText().toString();

        //TODO: search groups
    }


    public void addGroupToList(JSONObject groupJSON){
        SimpleGroup group = null;
        try {
            group = new SimpleGroup(groupJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        groupList.add(group);
        adapter.notifyDataSetChanged();
    }


    private void join(SimpleGroup group){

        // TODO: check uniqueness of session and group ID !!


        // create session with group sessionID
        boolean success = mService.createSession(group.sessionID, mService.getUserID());
        if (!success){
            Toast.makeText(this, "Could not join Group", Toast.LENGTH_SHORT).show();
            return;
        }

        // create joined Group
        Group newGroup = new Group(group.sessionID);
        FileHelper fileHelper = new FileHelper(this);

        //store newly created group in file
        fileHelper.writeToFile(getString(R.string.path_groups), group.groupID.toString(),
                group.toString());

        // return informations back to MainActivity
        Intent intent = new Intent();
        intent.putExtra(KEY_GROUP_NAME, group.groupName);
        intent.putExtra(KEY_GROUP_ID, group.groupID.toString());
        setResult(RESULT_OK, intent);
        finish();
    }
}
