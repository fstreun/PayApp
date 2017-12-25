package ch.ethz.inf.vs.fstreun.payapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.finance.SimpleGroup;
import ch.ethz.inf.vs.fstreun.network.SessionPublish.SessionSubscribeService;
import ch.ethz.inf.vs.fstreun.payapp.ListAdapters.ListSimpleGroupAdapter;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.DataService;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class JoinGroupActivity extends AppCompatActivity {

    public static JoinGroupActivity instance;

    DataService mService;
    private String TAG = "JoinGroupActivity";
    boolean mBound;
    private Intent intentSessionSubscribeService;
    public static final String KEY_SIMPLEGROUP = "simple_group";

    EditText editTextGroupSecret;

    List<SimpleGroup> groupList = new ArrayList<SimpleGroup>();
    ListSimpleGroupAdapter adapter;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        //initialize sharedPrefs
        String prefName = getString(R.string.pref_name);
        sharedPref = getSharedPreferences(prefName, Context.MODE_PRIVATE);

        editTextGroupSecret = findViewById(R.id.edittext_group_secret);

        adapter = new ListSimpleGroupAdapter(this, groupList);
        ListView listViewFoundGroup = findViewById(R.id.listView_foundGroups);
        listViewFoundGroup.setAdapter(adapter);

        listViewFoundGroup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                tryJoin(groupList.get(position));
            }
        });

        final Button buttonJoin = findViewById(R.id.button_search);
        buttonJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonJoinClicked();
            }
        });

        // for reference in service
        instance = this;


        // Bind to LocalService
        Intent intent = new Intent(this, DataService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("message");
            Log.w(TAG, "Got message: " + message);
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
      
        if (mBound){
            unbindService(mConnection);
            mBound = false;
        }

        try {
            stopService(intentSessionSubscribeService);
        }catch (Exception e){

        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            DataService.DataServiceBinder binder = (DataService.DataServiceBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };




    private void buttonJoinClicked(){
        // end running
        try {
            stopService(intentSessionSubscribeService);
        }catch (Exception e){

        }

        groupList.clear();
        adapter.notifyDataSetChanged();

        String groupHint = editTextGroupSecret.getText().toString();

        intentSessionSubscribeService = new Intent(this, SessionSubscribeService.class);
        intentSessionSubscribeService.putExtra("SECRET", groupHint);
        startService(intentSessionSubscribeService);
    }


    public void addGroupToList(JSONObject groupJSON){
        Log.d(TAG, "SimpleGroup addGroupToList: " + groupJSON);
        SimpleGroup group = null;
        try {
            group = new SimpleGroup(groupJSON);
            Log.d(TAG, "SimpleGroup added: " + group.toJSON().toString());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create SimpleGroup from JSON.", e);
            return;
        }
        if (group != null) {
            groupList.add(group);
            adapter.notifyDataSetChanged();
        }
    }

    private void showJoinDialog(final SimpleGroup group){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Name");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(group.groupName);
        builder.setView(input);


        // Set up the buttons
        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resultJoinDialog(group, input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resultJoinDialog(group, null);
            }
        });

        AlertDialog dialog = builder.create();

        // show keyboard
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
    }

    private void resultJoinDialog(final SimpleGroup group, String name){
        if (name == null || name.isEmpty()){
            // no name given
            return;
        }

        SimpleGroup newGroup = new SimpleGroup(group.groupID, name, group.sessionID);
        join(newGroup);
    }

    private void tryJoin(SimpleGroup group){

        List<SimpleGroup> groups = loadGroups();
        for (SimpleGroup sp : groups){
            if (sp.groupID.equals(group.groupID)){
                // group already in the list
                Toast.makeText(this, "Group already on the device: " + sp.groupName, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        showJoinDialog(group);
    }

    private void join(SimpleGroup group) {

        String simpleGroupString;
        try {
            simpleGroupString = group.toJSON().toString();
        } catch (JSONException e) {
            Toast.makeText(this, "Could not join Group", Toast.LENGTH_SHORT).show();
            return;
        }


        // create session with group sessionID
        boolean success = mService.createSession(group.sessionID, mService.getUserID());
        if (!success) {
            Toast.makeText(this, "Session already in the list", Toast.LENGTH_SHORT).show();
        }

        // create joined Group
        Group newGroup = new Group(group.sessionID);


        FileHelper fileHelper = new FileHelper(this);
        // create new group file
        fileHelper.writeToFile(getString(R.string.path_groups), group.groupID.toString(),
                newGroup.toString());

        // return informations back to MainActivity
        Intent intent = new Intent();
        intent.putExtra(KEY_SIMPLEGROUP, simpleGroupString);
        setResult(RESULT_OK, intent);
        finish();
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
}
