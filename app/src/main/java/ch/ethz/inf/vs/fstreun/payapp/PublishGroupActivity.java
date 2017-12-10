package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Random;

import ch.ethz.inf.vs.fstreun.network.SessionPublishService;

public class PublishGroupActivity extends AppCompatActivity {

    private String TAG = "PublishGroupActivity";
    public final static String KEY_SIMPLEGROUP = "key_group";
    private Intent mIntent;
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

        startPublish();
    }

    public final void startPublish(){
        Log.i(TAG, "startPublish()");

        String secret = textViewGroupSecret.getText().toString();
        String group = jsonString;

        // TODO: publish secret with group
        mIntent = new Intent(this, SessionPublishService.class);
        mIntent.putExtra("SECRET", secret);
        mIntent.putExtra("SIMPLEGROUP", jsonString);
        startService(mIntent);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                stopService(mIntent);
                finish();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
}
