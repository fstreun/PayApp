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
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Random;

import ch.ethz.inf.vs.fstreun.finance.SimpleGroup;
import ch.ethz.inf.vs.fstreun.network.SessionPublish.Server.GroupPublishService;

public class PublishGroupActivity extends AppCompatActivity {

    boolean bound;
    GroupPublishService.LocalBinder serviceBind;

    private String TAG = "PublishGroupActivity";
    public final static String KEY_SIMPLEGROUP = "key_group";
    private Intent intentSessionPublishService;
    private SimpleGroup group;
    String jsonString;

    TextView textViewGroupSecret;
    TextView textViewGroupID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_group);

        Intent intent = getIntent();

        jsonString = intent.getStringExtra(KEY_SIMPLEGROUP);
        try {
            group = new SimpleGroup(new JSONObject(jsonString));
        } catch (Exception e) {
            Toast.makeText(this, "Can not share Group", Toast.LENGTH_SHORT).show();
            return;
        }

        textViewGroupSecret = findViewById(R.id.textView_groupSecret);

        textViewGroupID = findViewById(R.id.textView_groupID);
        textViewGroupID.setText(group.groupID.toString());

        // start publish service
        Log.i(TAG, "start service");

        // bind service with simplegroup (secret is set with bound service)
        intentSessionPublishService = new Intent(this, GroupPublishService.class);
        startService(intentSessionPublishService);
        bindService(intentSessionPublishService, connection, BIND_AUTO_CREATE);
    }



    // service binding handler
    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service bound: " + name.getClassName());
            if (name.getClassName().equals(GroupPublishService.class.getName())){
                serviceBind = (GroupPublishService.LocalBinder) service;
                bound = true;

                // create random 4 digit number
                setSecret(Integer.toString(getRandom()));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected");
            bound = false;
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unbind service
        Log.d(TAG, "unbind service");
        unbindService(connection);
        // stop service
        stopService(intentSessionPublishService);
    }



    /**
     *
     * @return 4 digit random number
     */
    public int getRandom(){
        Random rnd = new Random();
        return  1000 + rnd.nextInt(9000);
    }

    public void setSecret(String secret){
        if (!bound){
            Log.e(TAG, "Service not yet bound. Cannot set secret");
            return;
        }

        serviceBind.setSecretGroup(secret, jsonString);
        textViewGroupSecret.setText(secret);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_publish_group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                stopService(intentSessionPublishService);
                finish();
                return true;
            case R.id.menu_refreshPublishGroup:
                if (group != null) {
                    // create random 4 digit number
                    setSecret(Integer.toString(getRandom()));
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
