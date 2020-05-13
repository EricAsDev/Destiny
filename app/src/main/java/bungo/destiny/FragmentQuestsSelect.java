package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.UnknownHostException;

public class FragmentQuestsSelect extends Fragment {

    String characterId;
    Handler handler;
    int PURSUITS_COMPLETE = 1;
    int MILESTONES_COMPLETE = 2;

    JSONArray pursuitsData;

    ItemAdapter.ViewHolder viewHolder;
    RecyclerView pursuitsRecycler;
    ItemAdapter pursuitsAdapter;

    View view;

    public FragmentQuestsSelect() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 1:
                        pursuitsAdapter.notifyDataSetChanged();
                        //todo add to pursuitsRecycler
                        break;
                    case 2:
                        pursuitsAdapter.notifyDataSetChanged();
                        //todo add to pursuitsRecycler
                        break;
                }
            }
        };
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_pursuits_select, container, false);
        return view;
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        pursuitsRecycler = view.findViewById(R.id.pursuits_recycler);
        this.view = view;

        pursuitsData = new JSONArray();
        RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityCharacter.context);
        pursuitsRecycler.setLayoutManager(layOutManager);
        pursuitsAdapter = new ItemAdapter(ActivityCharacter.context, pursuitsData);
        pursuitsRecycler.setAdapter(pursuitsAdapter);

        buildPursuits();

    }

    private void buildPursuits () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    characterId = ActivityCharacter.context.characterId;
                    JSONObject characterInventories = ActivityMain.context.character.getCharacterInventories();
                    JSONArray characterInventoryArray = characterInventories.getJSONObject("data").getJSONObject(characterId).getJSONArray("items");
                    //Log.d("Pursuits Definition", ActivityMain.context.defineElement(getResources().getString(R.string.fixed_hash_pursuits), "DestinyInventoryBucketDefinition"));
                    //Log.d("Inventories", characterInventoryArray.toString());
                    //Log.d("ch inventory array", String.valueOf(characterInventoryArray.length()));
                    for (int i = 0; i < characterInventoryArray.length(); i++) {
                        JSONObject inventoryItem = characterInventoryArray.getJSONObject(i);
                        String bucketHash = inventoryItem.getString("bucketHash");
                        JSONObject pursuitItem = new JSONObject();
                        if (bucketHash.equals(getResources().getString(R.string.fixed_hash_pursuits))) {
                            //Log.d("Pursuit", inventoryItem.toString());

                            //Log.d("Array Item", characterInventoryArray.getJSONObject(i).toString());

                            String itemHash = characterInventoryArray.getJSONObject(i).getString("itemHash");
                            String signedHash = ActivityMain.context.getSignedHash(itemHash);
                            JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));

                            //itemType 12 = quest step
                            //itemType 26 = bounty
                            if (itemDefinition.getString("itemType").equals("26")) {
                                Log.d("Item type", "Bounty");
                                continue;
                            }

                            int state = inventoryItem.getInt("state");
                            String quantity = inventoryItem.getString("quantity");
                            boolean isExotic = false;
                            //tierType 5 is Legendary
                            //tierType 6 is Exotic
                            if (itemDefinition.getJSONObject("inventory").getString("tierType").equals("6")) isExotic = true ;
                            pursuitItem.put("inventoryItem", inventoryItem);
                            pursuitItem.put("itemHash",itemHash);
                            if (itemDefinition.has("setData")) {
                                pursuitItem.put("name", itemDefinition.getJSONObject("setData").getString("questLineName"));
                            } else {
                                pursuitItem.put("name",itemDefinition.getJSONObject("displayProperties").getString("name"));
                            }
                            pursuitItem.put("description", itemDefinition.getJSONObject("displayProperties").getString("description"));
                            pursuitItem.put("icon", itemDefinition.getJSONObject("displayProperties").getString("icon"));
                            pursuitItem.put("quantity" ,quantity);
                            pursuitItem.put("isExotic", isExotic);

                            if (itemDefinition.getBoolean("redacted")) {
                                Log.d("Classified", itemDefinition.toString());
                                continue;
                                //todo deal with classified items
                            }

                            if (itemDefinition.getJSONObject("inventory").getBoolean("isInstanceItem")) {
                                String itemInstanceId = inventoryItem.optString("itemInstanceId");
                                pursuitItem.put("itemInstanceId", itemInstanceId);
                                JSONObject objectives = ActivityMain.context.character.getItemObjectives().optJSONObject(itemInstanceId);
                                if (objectives.getJSONArray("objectives").length() == 1) {
                                    String progress = objectives.getJSONArray("objectives").getJSONObject(0).getString("progress");
                                    String completionValue = objectives.getJSONArray("objectives").getJSONObject(0).getString("completionValue");
                                    pursuitItem.put("progress", progress);
                                    pursuitItem.put("completionValue", completionValue);
                                    if (objectives.getJSONArray("objectives").getJSONObject(0).getBoolean("complete")) {
                                        //This should return no data, right? if its complete, why would it still be here??
                                        pursuitItem.put("complete", true);
                                        Log.d("Quest", "Complete");
                                    } else {
                                        pursuitItem.put("complete", false);
                                    }
                                } else if (objectives.getJSONArray("objectives").length() > 1) {
                                    int counter = 0;
                                    for (int j = 0; j < objectives.getJSONArray("objectives").length(); j++) {
                                        if (objectives.getJSONArray("objectives").getJSONObject(j).getBoolean("complete")) {
                                            counter++;
                                        }
                                    }
                                    pursuitItem.put("progress", counter);
                                    pursuitItem.put("completionValue", objectives.getJSONArray("objectives").length());
                                    if (counter == objectives.getJSONArray("objectives").length()) {
                                        pursuitItem.put("complete", true);
                                        Log.d("Pursuit Item", "Complete");
                                    } else {
                                        pursuitItem.put("complete", false);
                                    }
                                }
/*
                                if (itemHash.equals("1472484362")) {
                                    Log.d("Small Gift", inventoryItem.toString());
                                    Log.d("Definition", itemDefinition.toString());
                                    Log.d("Objectives", objectives.toString());
                                }

 */
                                if (state == 2) { //tracked pursuit
                                    Log.d("Tracked", inventoryItem.toString());
                                    loadTrackedPursuit(pursuitItem);
                                    continue;
                                } else if (state == 4) { //super special state... like the Chalice
                                    Log.d("Special Shit", inventoryItem.toString());
                                }
                            }
                            pursuitsData.put(pursuitItem);
                        }

                        Message message = new Message();
                        message.what = PURSUITS_COMPLETE;
                        handler.sendMessage(message);
                    }

                } catch (Exception e) {
                    Log.d("Pursuits Error", e.toString());
                }
            }
        });
    }

    void loadTrackedPursuit (JSONObject pursuitItem) {

        view.findViewById(R.id.no_tracked_items).setVisibility(View.GONE);
        ViewStub viewStub = view.findViewById(R.id.viewStub);
        View inflated = viewStub.inflate();

        TextView titleTextView = inflated.findViewById(R.id.pursuit_title);
        TextView descriptionTextView = inflated.findViewById(R.id.description);
        TextView quantityTextView = inflated.findViewById(R.id.inventory_item_number);
        ProgressBar progressBar = inflated.findViewById(R.id.pursuit_progress);
        ImageView completeImageView = inflated.findViewById(R.id.completed);
        ImageView iconImageView = inflated.findViewById(R.id.inventory_item_image);

        try {
            String itemHash = pursuitItem.getString("itemHash");
            String name = pursuitItem.getString("name");
            String description = pursuitItem.getString("description");
            String icon = pursuitItem.getString("icon");
            int quantity = pursuitItem.getInt("quantity");
            int progress = pursuitItem.optInt("progress");
            int completionValue = pursuitItem.getInt("completionValue");
            boolean complete = pursuitItem.getBoolean("complete");

            titleTextView.setText(name);
            descriptionTextView.setText(description);
            new LoadImages(iconImageView).execute(icon);

            progressBar.setMax(completionValue);
            progressBar.setProgress(completionValue/2);
            if (complete) {
                completeImageView.setVisibility(View.VISIBLE);
            } else {
                completeImageView.setVisibility(View.INVISIBLE);
            }

            if (quantity > 1) {
                quantityTextView.setText(String.valueOf(quantity));
            }

        } catch (Exception e) {
            Log.d("Tracked Pursuit", e.toString());
        }
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray dataArray;

        ItemAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.dataArray = data;
        }

        @Override
        @NonNull public ItemAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.quest_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                viewHolder = holder;

                boolean isExotic = dataArray.getJSONObject(position).getBoolean("isExotic");
                if (isExotic) {
                    int[] colors = {Color.parseColor("#aaa9a9a9"), Color.parseColor("#d4af37")};
                    GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
                    gradientDrawable.setStroke(5, Color.parseColor("#d4af37"));
                    viewHolder.gearTint.setBackground(gradientDrawable);
                } else {
                    viewHolder.gearTint.setBackground(getResources().getDrawable(R.drawable.customborder));
                }

                viewHolder.iconImageView.setImageDrawable(getResources().getDrawable(R.drawable.missing_icon_d2, null));
                if (dataArray.getJSONObject(viewHolder.getAdapterPosition()).optBoolean("complete")){viewHolder.completeImageView.setVisibility(View.VISIBLE);}
                if (dataArray.getJSONObject(position).has("itemInstanceId")) {
                    int progress = dataArray.getJSONObject(viewHolder.getAdapterPosition()).optInt("progress");
                    int completionValue = dataArray.getJSONObject(viewHolder.getAdapterPosition()).optInt("completionValue");
                    viewHolder.progressBar.setMax(completionValue);
                    viewHolder.progressBar.setProgress(progress);
                    viewHolder.progressBar.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.progressBar.setVisibility(View.INVISIBLE);
                }
                int quantity = dataArray.getJSONObject(position).getInt("quantity");
                if (quantity > 1) {
                    viewHolder.quantityTextView.setText(String.valueOf(quantity));
                } else {
                    viewHolder.quantityTextView.setText(null);
                }
                viewHolder.titleTextView.setText(dataArray.getJSONObject(position).getString("name"));
                viewHolder.descriptionTextView.setText(dataArray.getJSONObject(position).getString("description"));
                String icon = dataArray.getJSONObject(position).getString("icon");
                new LoadImages(viewHolder.iconImageView).execute(icon);
            } catch (Exception e){
                Log.d("onBind", e.toString());
            }
        }
        @Override
        public int getItemCount() {
            return dataArray.length();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            TextView titleTextView;
            TextView descriptionTextView;
            TextView quantityTextView;
            ProgressBar progressBar;
            ImageView completeImageView;
            ImageView iconImageView;
            ImageView gearTint;

            Drawable background;

            ViewHolder(final View itemView) {
                super(itemView);

                titleTextView = itemView.findViewById(R.id.pursuit_title);
                descriptionTextView = itemView.findViewById(R.id.description);
                quantityTextView = itemView.findViewById(R.id.inventory_item_number);
                progressBar = itemView.findViewById(R.id.pursuit_progress);
                completeImageView = itemView.findViewById(R.id.completed);
                iconImageView = itemView.findViewById(R.id.inventory_item_image);
                gearTint = itemView.findViewById(R.id.gear_tint);
                background = gearTint.getBackground();

                itemView.setTag(this);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) itemView.getTag();
                        try {
                            Intent intent = new Intent(ActivityMain.context, ActivityQuestDetail.class);
                            intent.putExtra("item", dataArray.getJSONObject(viewHolder.getAdapterPosition()).toString());
                            intent.putExtra("characterId", characterId);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.d("pursuit error", e.toString());
                        }
                    }
                });
            }
        }

        private int getItem(int position) {
            return position;
        }
    }

    static class LoadImages extends AsyncTask<String, Void, Bitmap> {

        private WeakReference<ImageView> imageViewWeakReference;

        private LoadImages(ImageView imageView) {
            imageViewWeakReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String iconUrl = params[0];
            Bitmap icon;
            try {
                //Log.d("Icon Url", iconUrl);
                String iconPath = iconUrl.substring(iconUrl.lastIndexOf("/") + 1);
                Bitmap iconFromMemory = ActivityMain.context.getImage(iconPath);

                if (iconFromMemory != null) {
                    return iconFromMemory;
                } else {
                    URL url = new URL("https://www.bungie.net" + iconUrl);
                    InputStream in = url.openStream();
                    icon = BitmapFactory.decodeStream(in);
                    in.close();

                    ActivityMain.context.storeImage(icon, iconPath);
                    return icon;
                }

            }  catch (UnknownHostException unknownHost) {
                Log.d("URL", iconUrl);
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
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
}

/*
    class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        ViewHolder viewHolder;

        RecordsAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
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
                JSONObject jsonObject = recordsObjectArray.getJSONObject(position);
                String title = jsonObject.getJSONObject("displayProperties").getString("name");
                String description = jsonObject.getJSONObject("displayProperties").getString("description");
                String iconUrl = jsonObject.getJSONObject("displayProperties").optString("icon");

                holder.titleTextView.setText(title);
                holder.descriptionTextView.setText(description);

                String hashId = jsonObject.getString("hash");
                JSONObject record =  recordsObject.getJSONObject("records").optJSONObject(hashId);
                if (record != null) {
                    boolean isComplete = record.getJSONArray("objectives").getJSONObject(0).optBoolean("complete");
                    if (isComplete) {
                        holder.titleTextView.setText(holder.titleTextView.getText() + " (Completed)");
                        //    holder.titleTextView.setTextColor(Color.parseColor("#ffd4992a"));
                        //    holder.descriptionTextView.setTextColor(Color.parseColor("#ffd4992a"));
                        //    Log.d("Complete", record.getJSONArray("objectives").getJSONObject(0).toString());
                        holder.tint.setBackgroundColor(Color.parseColor("#d04286f4"));
                    } else {
                        holder.tint.setBackgroundColor(Color.parseColor("#008B8B8B"));
                    }
                    //String objectiveHash = record.getJSONArray("objectives").getJSONObject(0).getString("objectiveHash");
                    //String signedObjectiveHash = ActivityMain.context.getSignedHash(objectiveHash);
                    //JSONObject objective = new JSONObject(ActivityMain.context.defineElement(signedObjectiveHash, "DestinyObjectiveDefinition"));
                    int progressMax = record.getJSONArray("objectives").getJSONObject(0).getInt("completionValue");
                    int progress = record.getJSONArray("objectives").getJSONObject(0).getInt("progress");

                    holder.progressBar.setMax(progressMax);
                    holder.progressBar.setProgress(progress);



                    //Log.d(String.valueOf(position) + "/" + recordsObjectArray.length(), String.valueOf(progress) + "/" + String.valueOf(progressMax));
                }

                new LoadImages(holder.iconImageView).execute(iconUrl);

            } catch (Exception e){
                Log.d("onBind", e.toString());
            }
        }
        @Override
        public int getItemCount() {
            return recordsObjectArray.length();
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
                            String hashId = recordsObjectArray.getJSONObject(viewHolder.getAdapterPosition()).getString("hash");
                            Log.d("Click!", hashId);
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
                //layout = itemView.findViewById(R.id.record_item_layout);
            }
        }

        private int getItem(int position) {
            return position;
        }
    }
*//*
    public class PursuitsAdapter extends RecyclerView.Adapter<PursuitsAdapter.PursuitsViewHolder> {

        private JSONArray pursuitsRecyclerData;

        class PursuitsViewHolder extends RecyclerView.ViewHolder {
            private TextView titleText;
            private RecyclerView recyclerView;
            private PursuitsViewHolder (View view) {
                super(view);
                titleText = view.findViewById(R.id.pursuit_recycler_title);
                recyclerView = view.findViewById(R.id.pursuit_recycler_recyclerView);
            }
        }

        PursuitsAdapter(JSONArray dataObject) {
            pursuitsRecyclerData = dataObject;
        }

        @Override
        public @NonNull PursuitsViewHolder onCreateViewHolder (@NonNull ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.pursuit_recycler_layout, viewGroup, false);
            return new PursuitsViewHolder(view);
        }

        @Override
        public void onBindViewHolder (@NonNull PursuitsViewHolder pursuitsViewHolder, final int position) {
            //todo add sub-recyclers
            try {
                pursuitsViewHolder.titleText.setText(pursuitsRecyclerData.names().getString(position));
                ItemAdapter itemAdapter = new ItemAdapter(ActivityCharacter.context, pursuitsRecyclerData.getJSONArray(pursuitsRecyclerData.names().getString(position)));
                pursuitsViewHolder.recyclerView.setLayoutManager(new GridLayoutManager(ActivityCharacter.context, 4));
                pursuitsViewHolder.recyclerView.setAdapter(itemAdapter);
            } catch (Exception e) {
                Log.d("PursuitsViewHolder", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return pursuitsRecyclerData.length();
        }

    }
*/