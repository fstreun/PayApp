package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Random;

import ch.ethz.inf.vs.fstreun.network.SessionPublishService;

public class PublishGroupActivity extends AppCompatActivity {

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
        // create random 4 digit number
        setSecret(Integer.toString(getRandom()));

        textViewGroupID = findViewById(R.id.textView_groupID);
        textViewGroupID.setText(group.groupID.toString());

        // start publish service
        Log.i(TAG, "start service");

        String secret = textViewGroupSecret.getText().toString();

        intentSessionPublishService = new Intent(this, SessionPublishService.class);
        intentSessionPublishService.putExtra("SECRET", secret);
        intentSessionPublishService.putExtra("SIMPLEGROUP", jsonString);
        startService(intentSessionPublishService);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // stop service
        Log.d(TAG, "stop service");
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

                    textViewGroupID = findViewById(R.id.textView_groupID);
                    textViewGroupID.setText(group.groupID.toString());

                    // stop service
                    Log.i(TAG, "stop publish");
                    stopService(intentSessionPublishService);

                    // start publish service
                    Log.i(TAG, "start publish");

                    String secret = textViewGroupSecret.getText().toString();

                    intentSessionPublishService = new Intent(this, SessionPublishService.class);
                    intentSessionPublishService.putExtra("SECRET", secret);
                    intentSessionPublishService.putExtra("SIMPLEGROUP", jsonString);
                    startService(intentSessionPublishService);
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
