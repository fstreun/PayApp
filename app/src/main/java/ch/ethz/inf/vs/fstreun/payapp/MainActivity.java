package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ch.ethz.inf.vs.fstreun.network.SessionPublishService;
import ch.ethz.inf.vs.fstreun.network.SessionSubscribeService;

public class MainActivity extends AppCompatActivity {

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
                groups.get(position);
                Intent intent = new Intent(getApplicationContext(), GroupActivity.class);
                // TODO: add group id and group name to intent
                startActivity(intent);
            }
        });
    }


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
    class Group implements Serializable {
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
