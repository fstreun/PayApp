package ch.ethz.inf.vs.fstreun.payapp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;
import ch.ethz.inf.vs.fstreun.payapp.filemanager.FileHelper;

public class CreateGroupActivity extends AppCompatActivity {

    DataService mService;
    boolean mBound;

    public static final String KEY_GROUP_ID = "group_id"; //Not empty unique String out
    public static final String KEY_GROUP_NAME = "group_name"; //Not empty (unique?) String out

    EditText editTextGroupName;

    FileHelper fileHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        setTitle("New Group");

        editTextGroupName = findViewById(R.id.editText_groupName);

        fileHelper = new FileHelper(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group_creation, menu);
        return true;
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


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_save:

                if (!mBound){
                    // not bounded to service
                    // storing impossible
                    Toast.makeText(this, "Could not create Session", Toast.LENGTH_SHORT).show();
                    return true;
                }

                UUID sessionID = UUID.randomUUID();
                if (sessionID == null){
                    Toast.makeText(this, "Session Creation Failed", Toast.LENGTH_SHORT).show();
                    return true;
                }

                boolean success = mService.createSession(sessionID, mService.getUserID());

                if (!success){
                    Toast.makeText(this, "Session Creation Failed", Toast.LENGTH_SHORT).show();
                    return true;
                }

                String groupName = editTextGroupName.getText().toString();
                if (groupName.isEmpty()){
                    // invalid group name
                    Toast.makeText(this, "Invalid Group Name", Toast.LENGTH_SHORT).show();
                    return true;
                }

                // create unique ID for group
                UUID groupID = UUID.randomUUID();

                Group group = new Group(sessionID);

                //store newly created group in file
                fileHelper.writeToFile(getString(R.string.path_groups), groupID.toString(),
                        group.toString());
                Intent intent = new Intent();
                intent.putExtra(KEY_GROUP_NAME, groupName);
                intent.putExtra(KEY_GROUP_ID, groupID.toString());
                setResult(RESULT_OK, intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
