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

import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ActivityItem extends AppCompatActivity {

    JSONArray perks;
    JSONObject stats;
    JSONArray sockets;
    JSONObject plugStates;

    RecyclerView.LayoutManager statsLayoutManager;
    StatsAdapter statsAdapter;

    Handler handler;
    int STATS_COMPLETE = 100;
    int PERKS_COMPLETE = 101;
    int INSTANCES_COMPLETE = 102;

    String itemHash;

    JSONArray statsDisplay;
    JSONObject instanceData;

    private List<StatsObject> statsObjectList;
    private List<String> itemCategoryList = new ArrayList<>(Arrays.asList(
            "1", "18", "19", "20", "34", "35", "39", "40",
            "41", "42", "43", "51", "55", "56", "57", "59"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_inspect);

        itemHash = "";
        Intent receivedIntent = getIntent();
        if (receivedIntent.hasExtra("itemHash")) {
            itemHash = receivedIntent.getStringExtra("itemHash");
        } else {
            try {
                JSONObject instanceString = new JSONObject(receivedIntent.getStringExtra("instanceString"));
                getInstanceData(instanceString.getString("itemInstanceId"));
                itemHash = instanceString.getString("itemHash");
            } catch (Exception e) {
                Log.d("Instance String", Log.getStackTraceString(e));
            }
        }

        try {
            String signedHash = ActivityMain.context.getSignedHash(itemHash);
            JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
            JSONArray itemCategoryHashes = itemDefinition.getJSONArray("itemCategoryHashes");
            List<String> itemCategories = new ArrayList<>();
            for (int i = 0; i < itemCategoryHashes.length(); i++) {
                itemCategories.add(itemCategoryHashes.getString(i));
            }
            itemCategoryList.retainAll(itemCategories);
            Log.d("Category", itemCategoryList.toString());

            LoadTitle(itemDefinition);

            if (itemCategoryList.get(0).equals("1")) {
                findViewById(R.id.armor_weapon).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_weapon).setVisibility(View.VISIBLE);
                //LoadBasicStats(itemDefinition);//this should be capable of instance data
                //load stats
                //load perks
            } else if (itemCategoryList.get(0).equals("20")) {
                findViewById(R.id.armor_weapon).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_armor).setVisibility(View.VISIBLE);
                //LoadBasicStats(itemDefinition);//this should be capable of instance data
                //load stats
                //load perks
            }

        } catch (Exception e) {
            Log.d("Define Item", e.toString());
        }


        //statsDisplay = new JSONArray();
        //String state;

        /*
        final RecyclerView statsRecycler = findViewById(R.id.stats);
        RecyclerView perksRecycler = findViewById(R.id.perks);
        ImageView icon = findViewById(R.id.icon);
        ImageView element = findViewById(R.id.element);
        ImageView ammoType = findViewById(R.id.ammo_type);
        TextView name = findViewById(R.id.name);
        TextView displayType = findViewById(R.id.display_type);
        TextView lightLevel = findViewById(R.id.light_level);
        TextView descriptionText = findViewById(R.id.description);

        statsLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        statsRecycler.setLayoutManager(statsLayoutManager);

         */
/*
        try {
            JSONObject inventoryItem = new JSONObject(instanceString);

            itemHash = inventoryItem.getString("itemHash");
            itemInstanceId = inventoryItem.getString("itemInstanceId");
            state = inventoryItem.getString("state");
            String bucketHash = inventoryItem.getString("bucketHash");
            String signedBucketHash = ActivityMain.context.getSignedHash(bucketHash);
            JSONObject bucketObject = new JSONObject(ActivityMain.context.defineElement(signedBucketHash, "DestinyInventoryBucketDefinition"));
            Log.d("Bucket Location", bucketObject.getString("location"));

            String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
            JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition"));
            Log.d("Item Definition", itemDefinition.names().toString());

            String nameText = itemDefinition.getJSONObject("displayProperties").getString("name");
            String iconText = itemDefinition.getJSONObject("displayProperties").getString("icon");
            String itemDisplayName = itemDefinition.getString("itemTypeDisplayName");

            JSONObject instanceData;
            if (bucketObject.getString("location").equals("1")) {
                instanceData = ActivityMain.context.character.getItemInstances()
                        .getJSONObject(itemInstanceId);
            } else {
                instanceData = ActivityMain.context.profile.getItemComponents()
                        .getJSONObject("instances")
                        .getJSONObject("data")
                        .optJSONObject(itemInstanceId);
            }
            String damageTypeHash = instanceData.optString("damageTypeHash");
            if (!damageTypeHash.isEmpty()) {
                String signedDamageTypeHash = ActivityMain.context.getSignedHash(damageTypeHash);
                JSONObject damageTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedDamageTypeHash, "DestinyDamageTypeDefinition"));
                String elementIcon = damageTypeDefinition.getJSONObject("displayProperties").getString("icon");
                //int state = equippedItems.getJSONObject(i).getInt("state");
                int ammo = itemDefinition.getJSONObject("equippingBlock").getInt("ammoType");
                ammoType.setImageBitmap(getAmmoIcon(ammo));
                new LoadInventoryImages(element).execute(elementIcon);

            }
            String itemPower = "";
            if (instanceData.has("primaryStat")) itemPower = instanceData.getJSONObject("primaryStat").getString("value");
            String description = itemDefinition.getJSONObject("displayProperties").getString("description");

            name.setText(nameText);
            displayType.setText(itemDisplayName);
            lightLevel.setText(itemPower);
            descriptionText.setText(description);
            new LoadInventoryImages(icon).execute(iconText);

            getInstanceData(itemInstanceId);
        } catch (Exception e) {
            Log.d("Item - definition", e.toString());
        }
*/
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 1:
                        break;
                    case 100:
                        statsObjectList = new Gson().fromJson(statsDisplay.toString(), new TypeToken<List<StatsObject>>() {}.getType());

                        Collections.sort(statsObjectList, new Comparator<StatsObject>() {
                            @Override
                            public int compare(StatsObject o1, StatsObject o2) {
                                return Integer.compare( o1.getIndex(), o2.getIndex());
                            }
                        });
                        Collections.sort(statsObjectList, new Comparator<StatsObject>() {
                            @Override
                            public int compare(StatsObject o1, StatsObject o2) {
                                return  Boolean.compare(o1.isDisplayAsNumeric(), o2.isDisplayAsNumeric());
                            }
                        });

                        statsAdapter = new StatsAdapter(getApplicationContext(), statsObjectList);
                        //statsRecycler.setAdapter(statsAdapter);
                        statsAdapter.notifyDataSetChanged();
                        break;
                    case 101:
                        break;
                    case 102:
                        getItemInformation(itemHash);
                        break;
                }
            }
        };
    }

    private void LoadWeaponStats () {

    }

    private void LoadTitle (JSONObject itemDefinition) {

        try {
            View view = findViewById(R.id.include_layout);
            ImageView iconImage = view.findViewById(R.id.inventory_item_image);
            TextView nameText= findViewById(R.id.text_title);
            TextView typeText = findViewById(R.id.type);
            TextView descriptionText = findViewById(R.id.description);
            View background = findViewById(R.id.layout_title);

            String name = itemDefinition.getJSONObject("displayProperties").getString("name");
            String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
            String type = itemDefinition.getString("itemTypeDisplayName");
            int tierType = itemDefinition.getJSONObject("inventory").getInt("tierType");
            String description = itemDefinition.getJSONObject("displayProperties").getString("description");

            //determine masterwork

            nameText.setText(name);
            typeText.setText(type);
            descriptionText.setText(description);

            int colorCode;

            switch (tierType) {
                case 6:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.exotic, null);
                    break;
                case 5:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.legendary, null);
                    break;
                case 4:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.rare, null);
                    break;
                case 3:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.common, null);
                    break;
                case 2:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.basic, null);
                    break;
                default:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.no_tier, null);
                    break;
            }
            background.setBackgroundColor(colorCode);
            new LoadInventoryImages(iconImage).execute(icon);

        } catch (Exception e) {
            Log.d("Title Data", Log.getStackTraceString(e));
        }
    }

    private void LoadBasicWeaponData () {

    }

    private void LoadInstancedWeaponData () {

    }

    void getItemInformation (final String itemHash) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                boolean displayAsNumeric;
                String statHash;
                String name;
                String description;
                int index;
                int value;
                int maximumValue;
                boolean isHidden;

                try {
                    String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                    JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition"));

                    String statGroupHash = itemDefinition.getJSONObject("stats").getString("statGroupHash");
                    String signedStatGroupHash = ActivityMain.context.getSignedHash(statGroupHash);
                    JSONObject statGroupDefinition = new JSONObject(ActivityMain.context.defineElement(signedStatGroupHash, "DestinyStatGroupDefinition"));

                    JSONArray scaledStats = statGroupDefinition.getJSONArray("scaledStats");
                    List<String> scaledStatsHashes = new ArrayList<>();
                    for (int i = 0; i < scaledStats.length(); i++ ) {
                        scaledStatsHashes.add(scaledStats.getJSONObject(i).getString("statHash"));
                    }

                    JSONObject weaponStats = itemDefinition.getJSONObject("stats").getJSONObject("stats");

                    for (int i = 0; i < weaponStats.length(); i++) {
                        statHash = weaponStats.names().getString(i);
                        String signedStatHash = ActivityMain.context.getSignedHash(statHash);


                        JSONObject statDefinition = new JSONObject(ActivityMain.context.defineElement(signedStatHash, "DestinyStatDefinition"));
                        //Log.d(signedStatHash, statDefinition.toString());
                        JSONObject displayProperties = statDefinition.getJSONObject("displayProperties");
                        Log.d("Stat", displayProperties.getString("name"));
                        JSONObject statObject = weaponStats.getJSONObject(statHash);
/*
                        int statCategory = statDefinition.getInt("statCategory");
                        if (statCategory == 3) {
                            continue;
                        }

                        index = statDefinition.getInt("index");
                        name = displayProperties.getString("name");
                        description = displayProperties.getString("description");
                        maximumValue = 100;

                        value = statObject.getInt("value");
                        int maximum = statObject.getInt("maximum");
                        //int minimum = statObject.getInt("minimum");

                        //Log.d(name, value + " / " + maximum);
                        if (value <= 0) {
                            continue;
                        }
                        if (!scaledStatsHashes.contains(statHash)) { //means it is hidden
                            displayAsNumeric = true;
                            isHidden = true;

                        } else {
                            int scaledStatHashIndex = scaledStatsHashes.indexOf(statHash);
                            displayAsNumeric = scaledStats.getJSONObject(scaledStatHashIndex).getBoolean("displayAsNumeric");
                            maximumValue = scaledStats.getJSONObject(scaledStatHashIndex).getInt("maximumValue");
                            isHidden = false;
                        }

                        JSONObject statsObject = new JSONObject();
                        statsObject.put("displayAsNumeric", displayAsNumeric);
                        statsObject.put("statHash", statHash);
                        statsObject.put("name", name);
                        statsObject.put("description", description);
                        statsObject.put("index", index);
                        statsObject.put("value", value);
                        statsObject.put("maximumValue", maximumValue);
                        statsObject.put("isHidden", isHidden);

                        statsDisplay.put(statsObject);

 */
                    }
/*
                    Message message = new Message();
                    message.what = STATS_COMPLETE;
                    handler.sendMessage(message);
*/
                } catch (Exception e) {
                    Log.d("Item Information", Log.getStackTraceString(e));
                }
            }
        });
    }

    void getInstanceData (final String itemHash) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject response = new DestinyAPI().getItemInstance(itemHash);
                    if (response.getInt("ErrorCode") == 1 ) {
                        instanceData = response.getJSONObject("Response");

                        Log.d("Activity Response", response.getJSONObject("Response").names().toString());
                        //perks = response.getJSONObject("Response").getJSONObject("perks").getJSONObject("data").getJSONArray("perks");
                        Log.d("Perks Object", response.getJSONObject("Response").getJSONObject("perks").toString());
                        //sockets = response.getJSONObject("Response").getJSONObject("sockets").getJSONObject("data").getJSONArray("sockets");
                        Log.d("Sockets Object", response.getJSONObject("Response").getJSONObject("sockets").toString());
                        stats = response.getJSONObject("Response").getJSONObject("stats").getJSONObject("data").getJSONObject("stats");

                        //Log.d("stats", stats.names().toString());
                        for (int i = 0; i < stats.names().length(); i++) {
                            String signedStat = ActivityMain.context.getSignedHash(stats.names().getString(i));
                            JSONObject statDefinition = new JSONObject(ActivityMain.context.defineElement(signedStat, "DestinyStatDefinition"));
                            JSONObject displayProperties = statDefinition.getJSONObject("displayProperties");
                            Log.d("instance Stat", displayProperties.getString("name"));
                            //Log.d(signedStat, statDefinition.toString());
                        }

                        //Perks commonly found in the hover over an item
                        /*
                        for (int perk = 0; perk < perks.length(); perk++) {
                            String signedHash = ActivityMain.context.getSignedHash(perks.getJSONObject(perk).getString("perkHash"));
                            JSONObject perkDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinySandboxPerkDefinition"));
                            Log.d("Perk", perkDefinition.getJSONObject("displayProperties").optString("name"));
                            //Log.d("Perk", perkDefinition.toString());

                        }
                        */
                        //Sockets are found when configuring the item
                        /*
                        for (int socket = 0; socket < sockets.length(); socket++) {
                            if (!sockets.getJSONObject(socket).has("plugHash")) continue;
                            String signedHash = ActivityMain.context.getSignedHash(sockets.getJSONObject(socket).getString("plugHash"));
                            JSONObject plugDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
                            Log.d("Plug", plugDefinition.getJSONObject("displayProperties").getString("name"));
                        }
                        */
                        //Stats show the level. todo find the hidden stats too
                    } else {
                        String errorMessage = response.getString("Message");
                        Log.d("Error", errorMessage);
                    }
                } catch (Exception e) {
                    Log.d("getItemInstance", e.toString());
                }
                Message message = new Message();
                message.what = INSTANCES_COMPLETE;
                handler.sendMessage(message);
            }});
    }

    static class StatsObject {

        boolean displayAsNumeric;
        boolean isHidden;
        String statHash;
        String name;
        String description;
        int index;
        int value;
        int maximumValue;

        private boolean isDisplayAsNumeric() {
            return displayAsNumeric;
        }

        public String getStatHash() {
            return statHash;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        private int getIndex() {
            return index;
        }

        public int getValue() {
            return value;
        }

        private int getMaximumValue() {
            return maximumValue;
        }

        private boolean getIsHidden () { return isHidden; }
    }

    static class StatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private LayoutInflater layoutInflater;
        private List<StatsObject> data;

        public StatsAdapter(Context context, List<StatsObject> data){
            this.layoutInflater = LayoutInflater.from(context);
            this.data = data;
        }

        static class ViewHolderBar extends RecyclerView.ViewHolder {
            TextView nameView;
            ProgressBar valueView;
            private ViewHolderBar (View itemView) {
                super(itemView);
                nameView = itemView.findViewById(R.id.stat_title);
                valueView = itemView.findViewById(R.id.stat_progress);
            };

        }
        static class ViewHolderNumber extends  RecyclerView.ViewHolder {
            TextView nameView;
            TextView valueView;
            private ViewHolderNumber (View itemView) {
                super(itemView);
                nameView = itemView.findViewById(R.id.stat_title);
                valueView = itemView.findViewById(R.id.stat_progress);
            };
        }

        @Override
        public int getItemViewType(int position) {
            boolean type = data.get(position).displayAsNumeric;
            if (type) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View viewBar = layoutInflater.inflate(R.layout.item_stat_layout_bar, parent, false);
            final View viewNumber = layoutInflater.inflate(R.layout.item_stat_layout_number, parent, false);
            switch (viewType) {
                case 0:
                    return new ViewHolderBar(viewBar);
                case 1:
                    return new ViewHolderNumber(viewNumber);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            switch (holder.getItemViewType()) {
                case 0:
                    ViewHolderBar viewHolderBar = (ViewHolderBar) holder;
                    try {
                        viewHolderBar.nameView.setText(data.get(position).getName());
                        viewHolderBar.valueView.setMax(data.get(position).getMaximumValue());
                        viewHolderBar.valueView.setProgress(data.get(position).getValue());
                        if (data.get(position).getIsHidden()) {
                            viewHolderBar.nameView.setTextColor(Color.DKGRAY);
                        } else {
                            viewHolderBar.nameView.setTextColor(Color.WHITE);
                        }

                    } catch (Exception e) {
                        Log.d("ViewHolderBar", e.toString());
                    }
                    break;

                case 1:
                    ViewHolderNumber viewHolderNumber = (ViewHolderNumber) holder;
                    try {
                        viewHolderNumber.nameView.setText(data.get(position).getName());
                        viewHolderNumber.valueView.setText(String.valueOf(data.get(position).getValue()));
                        if (data.get(position).getIsHidden()) {
                            viewHolderNumber.nameView.setTextColor(Color.DKGRAY);
                        } else {
                            viewHolderNumber.nameView.setTextColor(Color.WHITE);
                        }

                    } catch (Exception e) {
                        Log.d("ViewHolderNumber", e.toString());
                    }
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
/*
    private Bitmap getAmmoIcon (int ammoType) {
        switch (ammoType){
            case 1:
                return icon_primary;
            case 2:
                return icon_special;
            case 3:
                return icon_heavy;
        }
        return null;
    }
*/
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
