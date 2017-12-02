package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    DataService dataService;
    boolean bound = false;

    ArrayAdapter<Group> adapter;
    List<Group> groups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        groups = loadGroups();

        adapter = new ListGroupAdapter(this, groups);

        ListView listViewGroup = findViewById(R.id.listView_groups);
        listViewGroup.setAdapter(adapter);
        listViewGroup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Group g = groups.get(position);
                Intent intent = new Intent(getApplicationContext(), GroupActivity.class);

                // add group id and group name to intent
                intent.putExtra(GroupActivity.KEY_GROUP_ID, g.groupID.toString());
                intent.putExtra(GroupActivity.KEY_GROUP_NAME, g.groupName);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind DataService
        Intent intent = new Intent(this, DataService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from service
        if (bound){
            unbindService(connection);
            bound = false;
        }
    }

    ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (name.getClassName().equals(DataService.class.getName())){
                DataService.LocalBinder binder = (DataService.LocalBinder) service;
                dataService = binder.getService();
                bound = true;
                Log.d(TAG, "onServiceConnected: " + name.getClassName());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected");
            bound = false;
        }
    };


    private final static int RESULT_CREATE = 1;
    public void buttonCreateClicked(View view){
        Log.d("MainActivity", "buttonCreateClicked()");
        //Intent intent = new Intent(this, SessionPublishService.class);
        //startService(intent);

        Intent intent = new Intent(this, CreateGroupActivity.class);
        startActivityForResult(intent, RESULT_CREATE);
    }

    private final static int RESULT_JOIN = 2;
    public void buttonJoinClicked(View view){
        //TODO: open JoinGroupActivity for result
        Log.d("MainActivity", "buttonJoinClicked()");
        //Intent intent = new Intent(this, SessionSubscribeService.class);
        //startService(intent);

        Intent intent = new Intent(this, JoinGroupActivity.class);
        startActivityForResult(intent, RESULT_JOIN);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case RESULT_CREATE:
                if (resultCode==RESULT_OK){
                    String groupName = data.getStringExtra(CreateGroupActivity.KEY_GROUP_NAME);
                    UUID groupID = UUID.fromString(data.getStringExtra(CreateGroupActivity.KEY_GROUP_ID));
                    Group group = new Group(groupID, groupName);
                    groups.add(group);
                    storeGroups(groups);
                    adapter.notifyDataSetChanged();
                }
                break;
            case RESULT_JOIN:
                if (resultCode==RESULT_OK){
                    String groupName = data.getStringExtra(JoinGroupActivity.KEY_GROUP_NAME);
                    UUID groupID = UUID.fromString(data.getStringExtra(JoinGroupActivity.KEY_GROUP_ID));
                    Group group = new Group(groupID, groupName);
                    groups.add(group);
                    storeGroups(groups);
                    adapter.notifyDataSetChanged();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * loads group objects from the shared preferences with the key defined in key_groups
     * @return List of all groups stored in shared preferences
     */
    private List<Group> loadGroups(){
        List<Group> groups = new ArrayList<>();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        Set<String> objects = sharedPref.getStringSet(getString(R.string.key_groups), null);
        if (objects != null) {
            for (String o : objects) {
                try {
                    groups.add(new Group(new JSONObject(o)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return groups;
    }

    /**
     * stores groups to shared preferences with the key defined in key_groups
     * @param groups to be stored
     */
    private void storeGroups(List<Group> groups){
        Set<String> objects = new HashSet<>(groups.size());
        for (Group g : groups){
            try {
                objects.add(g.toJSON().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(getString(R.string.key_groups), objects);
        editor.apply();
    }

    /**
     * minimal class definition of groups
     */
    class Group {
        // identifies group
        final static String KEY_UUID = "key_id";
        final UUID groupID;
        // name of group
        final static String KEY_NAME = "key_name";
        final String groupName;

        Group(UUID groupID, String groupName){
            this.groupID = groupID;
            this.groupName = groupName;
        }

        Group(JSONObject object) throws JSONException {
            groupID = UUID.fromString(object.getString(KEY_UUID));
            groupName = object.getString(KEY_NAME);
        }

        JSONObject toJSON() throws JSONException {
            JSONObject object = new JSONObject();
            object.put(KEY_UUID, groupID.toString());
            object.put(KEY_NAME, groupName);
            return object;
        }


    }
}
