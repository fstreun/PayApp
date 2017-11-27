package ch.ethz.inf.vs.fstreun.payapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.UUID;

import ch.ethz.inf.vs.fstreun.finance.Group;

public class JoinGroupActivity extends AppCompatActivity {

    public static final String KEY_GROUP_ID = "group_id"; //Not empty unique String out
    public static final String KEY_GROUP_NAME = "group_name"; //Not empty (unique?) String out

    EditText editTextGroupSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        editTextGroupSecret = findViewById(R.id.edittext_group_secret);

        final Button buttonJoin = findViewById(R.id.button_join);
        buttonJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonJoinClicked();
            }
        });
    }

    private void buttonJoinClicked(){
        //TODO: try to join group (sessions ID)
        String groupHint = editTextGroupSecret.getText().toString();
    }

    private String getGroupName(){
        //TODO: group name
        return "group Name";
    }


    private void join(UUID sessionID){
        // TODO: create group with that sessionID
        UUID groupID = UUID.randomUUID();
        Group group = null;// = new Group();

        //TODO: store group into corresponding file (with filehelper)

        //TODO: return groupID and groupName to MainActivity

    }
}
