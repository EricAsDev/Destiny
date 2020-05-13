package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class ActivityRecords extends AppCompatActivity {

    String recordHashId;
    JSONArray recordsObjectArray = new JSONArray();

    Handler handler;
    RecordsAdapter recordsAdapter;
    int RECORDS_DEFINED = 100;

    Data.Records dataRecords = ActivityMain.context.records;
    JSONObject recordsObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_records);

        Intent receivedIntent = getIntent();
        String intentData = receivedIntent.getStringExtra("hashId");

        try {
            JSONObject jsonObject = new JSONObject(intentData);
            recordHashId = jsonObject.getString("presentationNodeHash");
        } catch (Exception e) {
            Log.d("Intent", e.toString());
        }

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                if (inputMessage.what == RECORDS_DEFINED) {
                    recordsAdapter.notifyDataSetChanged();
                }
            }
        };

        try {
            String signedHashId = ActivityMain.context.getSignedHash(recordHashId);
            JSONObject recordDefinition = new JSONObject(ActivityMain.context.defineElement(signedHashId, "DestinyPresentationNodeDefinition"));

            String title = recordDefinition.getJSONObject("displayProperties").getString("name");
            TextView recordTitle = findViewById(R.id.record_title);
            recordTitle.setText(title);

            //String icon = recordDefinition.getJSONObject("displayProperties").getString("icon");
            //ImageView recordIcon = findViewById(R.id.record_icon);
            //new LoadInventoryImages(recordIcon).execute(icon);
        } catch (Exception e) {
            Log.d("Record Definition", e.toString());
        }

        RecyclerView recyclerView = findViewById(R.id.records_recycler);
        RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityRecords.this);
        recordsAdapter = new RecordsAdapter(ActivityRecords.this, recordsObjectArray);
        recyclerView.setLayoutManager(layOutManager);
        recyclerView.setAdapter(recordsAdapter);
        recordsAdapter.notifyDataSetChanged();

        defineRecords(recordHashId);
        recordsObject = dataRecords.getRecords();
    }

    void defineRecords(final String recordHash) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String signedTriumphHash = ActivityMain.context.getSignedHash(recordHash);
                    JSONObject records = new JSONObject(ActivityMain.context.defineElement(signedTriumphHash, "DestinyPresentationNodeDefinition"));

                    JSONArray recordsChildren = records.getJSONObject("children").getJSONArray("records");
                    //Log.d("records Children", recordsChildren.toString());
                    for (int i = 0; i < recordsChildren.length(); i++) {
                        String hash = recordsChildren.getJSONObject(i).getString("recordHash");
                        String signedHash = ActivityMain.context.getSignedHash(hash);
                        JSONObject recordDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyRecordDefinition"));
//todo using a lack of record name to identify its existence? ew.
                        if (!recordDefinition.getJSONObject("displayProperties").getString("name").isEmpty()) {
                            Log.d("Adding Record", recordDefinition.toString());
                            recordsObjectArray.put(recordDefinition);
                        }
                    }
                    Message message = new Message();
                    message.what = RECORDS_DEFINED;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    Log.d("Records", e.toString());
                }
            }
        });
    }

    class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        ViewHolder viewHolder;
        JSONArray records;

        RecordsAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.records = data;
        }

        @Override
        @NonNull
        public RecordsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.record_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                viewHolder = holder;
                JSONObject jsonObject = records.getJSONObject(position);
                String title = jsonObject.getJSONObject("displayProperties").getString("name");
                String description = jsonObject.getJSONObject("displayProperties").getString("description");
                String iconUrl = jsonObject.getJSONObject("displayProperties").optString("icon");
                //int scope = jsonObject.getInt("scope");

                holder.titleTextView.setText(title);
                holder.descriptionTextView.setText(description);

                String hashId = jsonObject.getString("hash");

                if (recordsObject.has("records") && recordsObject.getJSONObject("records").has(hashId)) {
                    JSONObject record = recordsObject.getJSONObject("records").getJSONObject(hashId);
                    Log.d("Record " + hashId, record.toString());

                    JSONArray objectivesArray = record.getJSONArray("objectives");
                    int progressMax = 0;
                    int progress = 0;
                    int completions = 0;

                    for (int i = 0; i < objectivesArray.length(); i++) {
                        boolean isComplete = record.getJSONArray("objectives").getJSONObject(i).getBoolean("complete");
                        if (isComplete) {
                            completions++;
                        }
                    }

                    if (completions == objectivesArray.length()) {
                        String completeText = holder.titleTextView.getText() + " (Completed)";
                        holder.titleTextView.setText(completeText);
                        holder.tint.setBackgroundColor(Color.parseColor("#d04286f4"));

                    } else {
                        holder.tint.setBackgroundColor(Color.parseColor("#008B8B8B"));

                    }

                    if (objectivesArray.length() == 1) {
                        progressMax = record.getJSONArray("objectives").getJSONObject(0).getInt("completionValue");
                        progress = record.getJSONArray("objectives").getJSONObject(0).getInt("progress");

                    } else {
                        progressMax = objectivesArray.length();
                        progress = completions;

                    }

                    holder.progressBar.setMax(progressMax);
                    holder.progressBar.setProgress(progress);

                } else {
                    Log.d("Record Data not found", jsonObject.toString());
                    holder.tint.setBackgroundColor(Color.parseColor("#008B8B8B"));
                    holder.progressBar.setMax(1);
                    holder.progressBar.setProgress(0);

                }

                new LoadInventoryImages(holder.iconImageView).execute(iconUrl);

            } catch (Exception e){
                Log.d("onBind", e.toString());
            }
        }
        @Override
        public int getItemCount() {
            return records.length();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            TextView titleTextView;
            TextView descriptionTextView;
            ProgressBar progressBar;
            ImageView iconImageView;
            ImageView tint;

            ViewHolder(final View itemView) {
                super(itemView);
                itemView.setTag(this);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) itemView.getTag();
                        try {
                            String hashId = records.getJSONObject(viewHolder.getAdapterPosition()).getString("hash");
                            Log.d("Opening Record", hashId);
                            Intent intent = new Intent(ActivityRecords.this, ActivityRecordItem.class);
                            intent.putExtra("recordHash", hashId);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.d("Record Click", e.toString());
                        }
                    }
                });
                titleTextView = itemView.findViewById(R.id.record_item_title);
                descriptionTextView = itemView.findViewById(R.id.record_item_description);
                progressBar = itemView.findViewById(R.id.record_item_progress);
                iconImageView = itemView.findViewById(R.id.record_item_icon);
                tint = itemView.findViewById(R.id.record_tint);
            }
        }

        private int getItem(int position) {
            return position;
        }
    }

    static class LoadInventoryImages extends AsyncTask<String, Void, Bitmap> {

        private WeakReference<ImageView> imageViewWeakReference;
        private LoadInventoryImages (ImageView imageView){
            imageViewWeakReference = new WeakReference<>(imageView);
        }
        @Override
        protected Bitmap doInBackground (String... params) {
            String iconUrl = params[0];
            Bitmap icon;
            try {
                iconUrl = iconUrl.replaceAll("'\'/", "/");

                String iconPath = iconUrl.substring(iconUrl.lastIndexOf("/") + 1);
                Bitmap iconFromMemory = ActivityMain.context.getImage(iconPath);
                if (iconFromMemory != null) {
                    return  iconFromMemory;
                } else {
                    InputStream in = new URL("https://www.bungie.net" + iconUrl).openStream();
                    icon = BitmapFactory.decodeStream(in);
                    in.close();

                    ActivityMain.context.storeImage(icon, iconPath);
                    return icon;
                }

            } catch (Exception e) {
                Log.d("LoadRecordImages", e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewWeakReference != null) {
                ImageView imageView = imageViewWeakReference.get();
                if(imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

}