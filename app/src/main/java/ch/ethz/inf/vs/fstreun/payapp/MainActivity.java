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

import ch.ethz.inf.vs.fstreun.network.DataSyncPublishService;
import ch.ethz.inf.vs.fstreun.network.DataSyncSubscribeService;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "###MainActivity";

    DataService dataService;
    boolean bound = false;

    ArrayAdapter<SimpleGroup> adapter;
    List<SimpleGroup> groups;

    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //initialize sharedPrefs
        String prefName = getString(R.string.pref_name);
        sharedPref = getSharedPreferences(prefName, Context.MODE_PRIVATE);

        groups = loadGroups();
        adapter = new ListGroupAdapter(this, groups);

        ListView listViewGroup = findViewById(R.id.listView_groups);
        listViewGroup.setAdapter(adapter);
        listViewGroup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SimpleGroup g = groups.get(position);
                Intent intent = new Intent(getApplicationContext(), GroupActivity.class);

                // add group id and group name to intent
                try {
                    intent.putExtra(GroupActivity.KEY_SIMPLE_GROUP, g.toJSON().toString());
                } catch (JSONException e) {
                    return;
                }
                startActivityForResult(intent, RESULT_GROUP);
            }
        });

        Log.d(TAG, "onCreate number of groups = " + groups.size());


        // start services which always run

        // Bind DataService
        Intent intentDataService = new Intent(this, DataService.class);
        startService(intentDataService);
        bindService(intentDataService, connection, BIND_AUTO_CREATE);

        Intent intentSyncPubService = new Intent(this, DataSyncPublishService.class);
        startService(intentSyncPubService);

        Intent intentSyncSubService = new Intent(this, DataSyncSubscribeService.class);
        startService(intentSyncSubService);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        Intent intent = new Intent(this, CreateGroupActivity.class);
        startActivityForResult(intent, RESULT_CREATE);
    }

    private final static int RESULT_JOIN = 2;
    public void buttonJoinClicked(View view){
        Log.d(TAG, "buttonJoinClicked()");
        Intent intent = new Intent(this, JoinGroupActivity.class);
        startActivityForResult(intent, RESULT_JOIN);
    }


    private final static int RESULT_GROUP = 0;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case  RESULT_GROUP:
                if (resultCode == RESULT_OK){
                    int code = data.getIntExtra(GroupActivity.KEY_RESULT_CODE, GroupActivity.CODE_DEFAULT);
                    switch (code){
                        case GroupActivity.CODE_DEFAULT:
                            return;
                        case GroupActivity.CODE_DELETE:
                            String stringSimpleGroup = data.getStringExtra(GroupActivity.KEY_SIMPLE_GROUP);
                            try {
                                JSONObject object = new JSONObject(stringSimpleGroup);
                                SimpleGroup simpleGroup = new SimpleGroup(object);
                                groups.remove(simpleGroup);
                                storeGroups(groups);
                                adapter.notifyDataSetChanged();
                            } catch (JSONException e) {
                                Log.e(TAG, "SimpleGroup JSON Error");
                                return;
                            }
                            return;
                    }

                }
                break;
            case RESULT_CREATE:
                if (resultCode==RESULT_OK) {
                    SimpleGroup simpleGroup;
                    try {
                        JSONObject simpleGroupJSON = new JSONObject(data.getStringExtra(CreateGroupActivity.KEY_SIMPLEGROUP));
                        simpleGroup = new SimpleGroup(simpleGroupJSON);
                    } catch (JSONException e) {
                        //TODO: handel exception
                        return;
                    }
                    groups.add(simpleGroup);
                    storeGroups(groups);
                    adapter.notifyDataSetChanged();
                }
                break;
            case RESULT_JOIN:
                if (resultCode==RESULT_OK){
                    SimpleGroup simpleGroup;
                    try {
                        JSONObject simpleGroupJSON = new JSONObject(data.getStringExtra(CreateGroupActivity.KEY_SIMPLEGROUP));
                        simpleGroup = new SimpleGroup(simpleGroupJSON);
                    } catch (JSONException e) {
                        //TODO: handel exception
                        return;
                    }
                    groups.add(simpleGroup);
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
    private List<SimpleGroup> loadGroups(){
        List<SimpleGroup> groups = new ArrayList<>();
        Set<String> objects = sharedPref.getStringSet(getString(R.string.key_groups), null);
        if (objects != null) {
            for (String o : objects) {
                try {
                    groups.add(new SimpleGroup(new JSONObject(o)));
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
    private void storeGroups(List<SimpleGroup> groups){
        Set<String> objects = new HashSet<>(groups.size());
        for (SimpleGroup g : groups){
            try {
                objects.add(g.toJSON().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(getString(R.string.key_groups), objects);
        editor.apply();
        Log.d(TAG, "putting set of strings in storeGroups()\n" +objects.toString());
    }
}
