package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class FragmentTriumphsSelect extends Fragment {

    Handler handler;
    int TRIUMPHS_DEFINED = 1101;
    int SEALS_DEFINED = 1102;
    int PRESENTATION_NODES = 1103;

    TriumphsAdapter.ViewHolder viewHolder;
    SealsAdapter.ViewHolder sealsViewHolder;

    JSONArray triumphsObjectArray;
    JSONArray sealsObjectArray;

    RecyclerView triumphsRecycler;
    RecyclerView sealsRecycler;

    Data.PresentationNodes nodes = ActivityMain.context.presentationNodes;
    JSONObject presentationNodes = new JSONObject();

    public FragmentTriumphsSelect() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 1101:
                        //Log.d("Triumphs", "Data Loaded");
                        TriumphsAdapter triumphsAdapter = new TriumphsAdapter(ActivityMain.context, triumphsObjectArray);
                        triumphsRecycler.setAdapter(triumphsAdapter);
                        break;
                    case 1102:
                        //Log.d("Seals", "Data Loaded");
                        SealsAdapter sealsAdapter = new SealsAdapter(ActivityMain.context, sealsObjectArray);
                        sealsRecycler.setAdapter(sealsAdapter);
                        break;
                    case 1103:
                        buildTriumphsAndSeals();
                        break;
                }
            }
        };
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInsanceState) {
        View view = inflater.inflate(R.layout.fragment_triumphs_select, container, false);
        sealsRecycler = view.findViewById(R.id.seals_recycler);
        triumphsRecycler = view.findViewById(R.id.triumphs_recycler);
        LinearLayoutManager sealsLayoutManager = new LinearLayoutManager(ActivityMain.context, LinearLayoutManager.HORIZONTAL, false);
        sealsRecycler.setLayoutManager(sealsLayoutManager);
        GridLayoutManager triumphsLayoutManager = new GridLayoutManager(ActivityMain.context, 2);
        triumphsRecycler.setLayoutManager(triumphsLayoutManager);
        return view;
    }

    void buildTriumphsAndSeals () {
        presentationNodes = nodes.getProfilePresentationNodes();

        triumphsObjectArray = new JSONArray();
        sealsObjectArray = new JSONArray();
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    JSONObject triumphs = new JSONObject(ActivityMain.context.defineElement(getResources().getString(R.string.fixed_hash_triumphs), "DestinyPresentationNodeDefinition"));
                    JSONArray childrenTriumphs = triumphs.getJSONObject("children").getJSONArray("presentationNodes");
                    for (int i = 0; i < childrenTriumphs.length(); i++) {

                        String hash = childrenTriumphs.getJSONObject(i).getString("presentationNodeHash");
                        String signedHash = ActivityMain.context.getSignedHash(hash);
                        JSONObject triumphDefinition = new JSONObject (ActivityMain.context.defineElement(signedHash, "DestinyPresentationNodeDefinition"));

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("imageUrl", triumphDefinition.getJSONObject("displayProperties").getString("icon"));
                        jsonObject.put("name", triumphDefinition.getJSONObject("displayProperties").getString("name"));
                        jsonObject.put("hash", triumphDefinition.getString("hash"));
                        triumphsObjectArray.put(jsonObject);
                    }

                    Message message = new Message();
                    message.what = TRIUMPHS_DEFINED;
                    handler.sendMessage(message);
                    //Log.d("Triumphs", triumphsObjectArray.toString());
                } catch (Exception e) {
                    Log.d("Triumphs", e.toString());
                }
            }
        });

        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject seals = new JSONObject(ActivityMain.context.defineElement(getResources().getString(R.string.fixed_hash_seals), "DestinyPresentationNodeDefinition"));
                    JSONArray childrenSeals = seals.getJSONObject("children").getJSONArray("presentationNodes");

                    for (int i = 0; i < childrenSeals.length(); i++) {
                        String hash = childrenSeals.getJSONObject(i).getString("presentationNodeHash");
                        String signedHash = ActivityMain.context.getSignedHash(hash);
                        JSONObject sealsDefinition = new JSONObject (ActivityMain.context.defineElement(signedHash, "DestinyPresentationNodeDefinition"));
                        //Log.d("Seal", sealsDefinition.toString());
                        if (sealsDefinition.getBoolean("redacted")) continue;

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("imageUrl", sealsDefinition.getJSONObject("displayProperties").getString("icon"));
                        jsonObject.put("name", sealsDefinition.getJSONObject("displayProperties").getString("name"));
                        jsonObject.put("hash", sealsDefinition.getString("hash"));
                        sealsObjectArray.put(jsonObject);
                    }

                    Message message = new Message();
                    message.what = SEALS_DEFINED;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    Log.d("Triumphs", e.toString());
                }

            }
        });
    }

    void getRecordsData () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                presentationNodes = nodes.getProfilePresentationNodes();
            }
        });
        Message message = new Message();
        message.what = PRESENTATION_NODES;
        handler.sendMessage(message);
    }

    class SealsAdapter extends RecyclerView.Adapter<SealsAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray dataArray;

        SealsAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.dataArray = data;
        }
        @Override
        @NonNull
        public SealsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.seals_item_layout, parent, false);
            return new SealsAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SealsAdapter.ViewHolder holder, final int position) {
            try {
                sealsViewHolder = holder;
                final String hashId = dataArray.getJSONObject(position).getString("hash");
                String imageUrl = dataArray.getJSONObject(position).getString("imageUrl");

                JSONObject nodeObject = presentationNodes.optJSONObject(hashId);
                if (presentationNodes.has(hashId)) {
                    final String progress = nodeObject.getString("progressValue");
                    final String completion = nodeObject.getString("completionValue");
                    final String sealCompletionText = progress + "/" + completion;

                    holder.textView.setText(sealCompletionText);
                } else {
                    holder.textView.setText(null);
                    Log.d("Hash not found", hashId);
                }

                sealsViewHolder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(ActivityMain.context, ActivitySeals.class);
                            intent.putExtra("hashId", hashId);
                            ActivityMain.context.startActivity(intent);
                        } catch (Exception e) {
                            Log.d("Seals Click", e.toString());
                        }
                    }
                });
                new LoadImages(holder.imageView).execute(imageUrl);
            } catch (Exception e){
                Log.d("onBind", e.toString());
            }
        }
        @Override
        public int getItemCount() {
            return dataArray.length();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            TextView textView;
            ImageView imageView;

            ViewHolder(final View itemView) {
                super(itemView);
                itemView.setTag(this);
                imageView = itemView.findViewById(R.id.triumph_item_image);
                textView = itemView.findViewById(R.id.triumph_item_text);
            }
        }
    }

    class TriumphsAdapter extends RecyclerView.Adapter<TriumphsAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray dataArray;

        TriumphsAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.dataArray = data;
        }

        @Override
        @NonNull
        public TriumphsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.triumph_item_layout, parent, false);
            return new TriumphsAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TriumphsAdapter.ViewHolder holder, final int position) {
            try {
                viewHolder = holder;
                String name = dataArray.getJSONObject(position).getString("name");
                String imageUrl = dataArray.getJSONObject(position).getString("imageUrl");
                final String hashId = dataArray.getJSONObject(position).getString("hash");
                holder.textView.setText(name);
                viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Log.d("Click!", hashId);
                            Intent intent = new Intent(ActivityMain.context, ActivityTriumph.class);
                            intent.putExtra("hashId", hashId);
                            ActivityMain.context.startActivity(intent);
                        } catch (Exception e) {
                            Log.d("Triumph Click", e.toString());
                        }
                    }
                });
                new LoadImages(holder.imageView).execute(imageUrl);
            } catch (Exception e){
                Log.d("onBind", e.toString());
            }
        }
        @Override
        public int getItemCount() {
            return dataArray.length();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            TextView textView;
            ImageView imageView;

            ViewHolder(final View itemView) {
                super(itemView);
                itemView.setTag(this);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) itemView.getTag();
                        Log.d("Click!", String.valueOf(viewHolder.getAdapterPosition()));
                    }
                });
                imageView = itemView.findViewById(R.id.triumph_item_image);
                textView = itemView.findViewById(R.id.triumph_item_text);
            }
        }

        private int getItem(int position) {
            return position;
        }
    }

    static class LoadImages extends AsyncTask<String, Void, Bitmap> {

        private WeakReference<ImageView> imageViewWeakReference;
        private LoadImages (ImageView imageView){
            imageViewWeakReference = new WeakReference<>(imageView);
        }
        @Override
        protected Bitmap doInBackground (String... params) {
            String itemUrl = params[0];
            //String signedHash = ActivityMain.context.getSignedHash(itemHash);
            Bitmap icon;
            try {
                //JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
                //String iconUrl = itemDefinition.getJSONObject("displayProperties").getString("icon");
                String iconUrl = itemUrl.replaceAll("'\'/", "/");

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
                Log.d("LoadInventoryImages", e.getMessage());
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
