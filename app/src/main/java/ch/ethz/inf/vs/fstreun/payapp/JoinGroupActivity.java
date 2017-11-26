package ch.ethz.inf.vs.fstreun.payapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class JoinGroupActivity extends AppCompatActivity {

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
}
