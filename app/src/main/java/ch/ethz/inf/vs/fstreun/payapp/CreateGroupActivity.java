package ch.ethz.inf.vs.fstreun.payapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;

public class CreateGroupActivity extends AppCompatActivity {

    public static final String KEY_GROUP_ID = "group_id"; //Not empty unique String out
    public static final String KEY_GROUP_NAME = "group_name"; //Not empty (unique?) String out

    EditText editTextGroupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        editTextGroupName = findViewById(R.id.editText_groupName);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group_creation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_save:

                String groupName = editTextGroupName.getText().toString();
                if (groupName.isEmpty()){
                    // invalid group name
                    Toast.makeText(this, "Invalid Group Name", Toast.LENGTH_SHORT);
                    return true;
                }

                // create unique ID for group
                UUID groupID = UUID.randomUUID();

                //TODO: create new session and get sessionID
                UUID sessionID = UUID.randomUUID();

                if (sessionID == null){
                    Toast.makeText(this, "Session Creation Failed", Toast.LENGTH_SHORT);
                    return true;
                }

                //TODO: create group with sessionID
                Group group;// = new Group(sessionID);
                //TODO: store newly created group in file

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
