package bungo.destiny;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class ActivityPGCR extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pgcr);

        try {
            Intent receivedIntent = getIntent();
            String activityId = receivedIntent.getStringExtra("activityData");
            Log.d("PGCR", activityId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
