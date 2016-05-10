package vincente.com.multidownloadbluetooth;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import vincente.com.multidownloadbluetooth.fragments.MessageThreadFragment;

public class ThreadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportFragmentManager().beginTransaction().add(
                R.id.content_container,
                MessageThreadFragment.createInstance(getIntent().getStringExtra("uuid"))
        ).commit();
    }
}
