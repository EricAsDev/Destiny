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

import android.text.LoginFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ActivityItem extends AppCompatActivity {

    JSONObject stats;
    JSONObject type3Stats = new JSONObject();

    JSONArray mods;
    JSONArray sockets;
    JSONArray socketCategories;
    JSONArray socketEntries;
    JSONArray ammoTypes;

    RecyclerView.LayoutManager statsLayoutManager;
    StatsAdapter statsAdapter;
    RecyclerView statsRecycler;

    RecyclerView.LayoutManager categoryLayoutManager;
    SocketCategory socketsCategories;
    RecyclerView socketCategoryRecycler;

    SocketAdapter socketAdapter;

    Handler handler;
    final int STATS_COMPLETE = 100;
    final int SOCKETS_COMPLETE = 101;
    final int INSTANCES_COMPLETE = 102;
    final int UPDATE_STAT = 103;
    final int UPDATE_PLUGS = 104;

    String itemHash;
    String kineticURL = "/img/destiny_content/ammo_types/primary.png";
    String specialURL = "/img/destiny_content/ammo_types/special.png";
    String heavyURL = "/img/destiny_content/ammo_types/heavy.png";

    JSONArray statsDisplay;
    JSONObject instanceData;

    public Context context;

    private List<StatsObject> statsObjectList;
    private List<PlugObject> plugObjectList = new ArrayList<>();
    private List<String> itemCategoryList = new ArrayList<>(Arrays.asList(
            "1", "18", "19", "20", "34", "35", "39", "40",
            "41", "42", "43", "51", "55", "56", "57", "59"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_inspect);

        context = this.getApplicationContext();

        itemHash = "";
        Intent receivedIntent = getIntent();
        if (receivedIntent.hasExtra("itemHash")) {
            itemHash = receivedIntent.getStringExtra("itemHash");
        } else {
            try {
                JSONObject instanceString = new JSONObject(receivedIntent.getStringExtra("instanceString"));
                itemHash = instanceString.getString("itemHash");
                instanceData = instanceString;
                Log.d("Instance String", instanceString.toString());
                int state = instanceData.getInt("state");
                ImageView inventoryItemBackground = findViewById(R.id.inventory_item_background);
                if (state == 4) {
                    inventoryItemBackground.setColorFilter(Color.parseColor("#a0f5c242"));
                }
            } catch (Exception e) {
                Log.d("Instance String", Log.getStackTraceString(e));
            }
        }

        try {
            ammoTypes = new JSONArray();
            ammoTypes.put(null);
            ammoTypes.put(kineticURL);
            ammoTypes.put(specialURL);
            ammoTypes.put(heavyURL);
            ammoTypes.put(null);

            String signedHash = ActivityMain.context.getSignedHash(itemHash);
            JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
            JSONArray itemCategoryHashes = itemDefinition.getJSONArray("itemCategoryHashes");
            List<String> itemCategories = new ArrayList<>();

            for (int i = 0; i < itemCategoryHashes.length(); i++) {
                itemCategories.add(itemCategoryHashes.getString(i));
            }
            itemCategoryList.retainAll(itemCategories);

            if (itemCategoryList.get(0).equals("1")) {
                findViewById(R.id.armor_weapon).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_weapon).setVisibility(View.VISIBLE);
                getDefaultStats(itemDefinition);
                getSocketCategories(itemHash);

            } else if (itemCategoryList.get(0).equals("20")) {
                findViewById(R.id.armor_weapon).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_weapon).setVisibility(View.VISIBLE);
                getDefaultStats(itemDefinition);
                getSocketCategories(itemHash);
            }
            LoadTitle(itemDefinition);

        } catch (Exception e) {
            Log.d("Define Item", e.toString());
        }

        statsDisplay = new JSONArray();
        //String state;

        /*
        RecyclerView perksRecycler = findViewById(R.id.perks);
        ImageView icon = findViewById(R.id.icon);
        ImageView element = findViewById(R.id.element);
        ImageView ammoType = findViewById(R.id.ammo_type);
        TextView name = findViewById(R.id.name);
        TextView displayType = findViewById(R.id.display_type);
        TextView lightLevel = findViewById(R.id.light_level);
        TextView descriptionText = findViewById(R.id.description);
         */

        statsRecycler = findViewById(R.id.stats);
        statsLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        statsRecycler.setLayoutManager(statsLayoutManager);

        socketCategoryRecycler = findViewById(R.id.socket_categories);
        categoryLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        socketCategoryRecycler.setLayoutManager(categoryLayoutManager);

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
                    case STATS_COMPLETE:
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
                        statsRecycler.setAdapter(statsAdapter);
                        statsAdapter.notifyDataSetChanged();

                        try {
                            getInstanceData(instanceData.getString("itemInstanceId"));
                        } catch (Exception e) {
                            Log.d("STATS_COMPLETE", Log.getStackTraceString(e));
                        }
                        break;

                    case SOCKETS_COMPLETE:
                        socketsCategories = new SocketCategory(getApplicationContext(), socketCategories);
                        socketCategoryRecycler.setAdapter(socketsCategories);
                        socketsCategories.notifyDataSetChanged();
                        break;

                    case INSTANCES_COMPLETE:
                        break;

                    case UPDATE_STAT:
                        statsAdapter.notifyDataSetChanged();
                        break;

                    case UPDATE_PLUGS:
                        socketsCategories.notifyDataSetChanged();
                        break;

                }
            }
        };
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

    void getDefaultDetails (JSONObject itemDefinition) {
        try {
            ImageView ammoTypeIV = findViewById(R.id.ammo_type);
            int ammoType = itemDefinition.getJSONObject("equippingBlock").getInt("ammoType");
            if (ammoType >0 && ammoType < 4) {
                String ammoURL = ammoTypes.getString(ammoType);
                new LoadInventoryImages(ammoTypeIV).execute(ammoURL);
            }
            String defaultDamageTypeHash = itemDefinition.optString("defaultDamageTypeHash");
            if (defaultDamageTypeHash != null) {
                String signedHash = ActivityMain.context.getSignedHash(defaultDamageTypeHash);
                JSONObject damageTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyDamageTypeDefinition"));
                String icon = damageTypeDefinition.getJSONObject("displayProperties").getString("icon");
                ImageView element = findViewById(R.id.element_icon);
                new LoadInventoryImages(element).execute(icon);
            }

        } catch (Exception e) {
            Log.d("Default Details", Log.getStackTraceString(e));
        }

    }

    void getDefaultStats (final JSONObject itemDefinition) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                String statHash = "";
                String name = "";
                String description = "";
                int index = 0;
                int value = 0;
                int maximumValue = 100;
                boolean isHidden = false;
                boolean displayAsNumeric = true;

                try {
                    JSONObject stats = itemDefinition.getJSONObject("stats").getJSONObject("stats");
                    String statGroupHash = itemDefinition.getJSONObject("stats").getString("statGroupHash");

                    String signedStatGroupHash = ActivityMain.context.getSignedHash(statGroupHash);
                    JSONObject statGroupDefinition = new JSONObject(ActivityMain.context.defineElement(signedStatGroupHash, "DestinyStatGroupDefinition"));
                    JSONArray scaledStats = statGroupDefinition.getJSONArray("scaledStats");

                    for (int i = 0; i < scaledStats.length(); i++) {
                        statHash = scaledStats.getJSONObject(i).getString("statHash");
                        displayAsNumeric = scaledStats.getJSONObject(i).getBoolean("displayAsNumeric");
                        maximumValue = scaledStats.getJSONObject(i).getInt("maximumValue");

                        String signedStatHash = ActivityMain.context.getSignedHash(statHash);
                        JSONObject statDefinition = new JSONObject(ActivityMain.context.defineElement(signedStatHash, "DestinyStatDefinition"));
                        JSONObject displayProperties = statDefinition.getJSONObject("displayProperties");

                        name = displayProperties.getString("name");
                        description = displayProperties.getString("description");
                        index = statDefinition.getInt("index");

                        if (stats.has(statHash)) {
                            JSONObject statObject = stats.getJSONObject(statHash);
                            value = statObject.getInt("value");
                            maximumValue = statObject.getInt("displayMaximum");
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
                    }

                    //process any hidden stats
                    ArrayList<String> listOne = new ArrayList<String>();
                    for (int i = 0; i < stats.names().length(); i++) {
                        listOne.add(stats.names().getString(i));
                    }
                    ArrayList<String> listTwo = new ArrayList<String>();
                    for (int i = 0; i < scaledStats.length(); i++) {
                        listTwo.add(scaledStats.getJSONObject(i).getString("statHash"));
                    }
                    listOne.removeAll(listTwo);

                    for (int i = 0; i < listOne.size(); i++) {
                        String hiddenStatHash = listOne.get(i);
                        JSONObject hiddenStat = stats.getJSONObject(hiddenStatHash);
                        value = hiddenStat.getInt("value");
                        maximumValue = hiddenStat.getInt("displayMaximum");

                        String signedStatHash = ActivityMain.context.getSignedHash(hiddenStatHash);
                        JSONObject statDefinition = new JSONObject(ActivityMain.context.defineElement(signedStatHash, "DestinyStatDefinition"));
                        int statCategory = statDefinition.getInt("statCategory");
                        if (statCategory == 3) {
                            Log.d("Type 3 Stat", statDefinition.toString());
                            type3Stats.put(hiddenStatHash, hiddenStat);
                            continue;
                        }
                        JSONObject displayProperties = statDefinition.getJSONObject("displayProperties");

                        name = displayProperties.getString("name");
                        description = displayProperties.getString("description");
                        index = statDefinition.getInt("index");

                        JSONObject statsObject = new JSONObject();
                        statsObject.put("displayAsNumeric", true);
                        statsObject.put("statHash", hiddenStatHash);
                        statsObject.put("name", name);
                        statsObject.put("description", description);
                        statsObject.put("index", index);
                        statsObject.put("value", value);
                        statsObject.put("maximumValue", maximumValue);
                        statsObject.put("isHidden", true);

                        statsDisplay.put(statsObject);
                    }

                    Message message = new Message();
                    message.what = STATS_COMPLETE;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    Log.d("Item Information", Log.getStackTraceString(e));
                }
            }
        });
    }

    void getSocketCategories (final String itemHash) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                    JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition"));
                    if (itemDefinition.has("sockets")) {
                        socketCategories = itemDefinition.getJSONObject("sockets").getJSONArray("socketCategories");
                        socketEntries = itemDefinition.getJSONObject("sockets").getJSONArray("socketEntries");

                        for (int i = 0; i < socketEntries.length(); i++) {
                            JSONObject socketEntry = socketEntries.getJSONObject(i);
                            String singleInitialItemHash = socketEntry.getString("singleInitialItemHash");

                            //omitting these things because of crashes
                            if (!socketEntry.getBoolean("defaultVisible") || singleInitialItemHash.equals("0")) {
                                PlugObject plugObject = new PlugObject();
                                plugObject.defaultVisible = socketEntry.getBoolean("defaultVisible");
                                plugObjectList.add(plugObject);
                                continue;
                            };

                            String signedSingleInitialItemHash = ActivityMain.context.getSignedHash(singleInitialItemHash);
                            Log.d(singleInitialItemHash, signedSingleInitialItemHash);
                            JSONObject socketDefinition = new JSONObject(ActivityMain.context.defineElement(signedSingleInitialItemHash, "DestinyInventoryItemDefinition"));
                            Log.d("Socket Definition", socketDefinition.toString());
                            String icon = socketDefinition.getJSONObject("displayProperties").getString("icon");
                            String name = socketDefinition.getJSONObject("displayProperties").getString("name");
                            String description = socketDefinition.getJSONObject("displayProperties").getString("description");
                            String socketTypeHash = socketEntry.getString("socketTypeHash");
                            String signedSocketTypeHash = ActivityMain.context.getSignedHash(socketTypeHash);
                            JSONObject socketTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedSocketTypeHash, "DestinySocketTypeDefinition"));
                            JSONArray whiteList = socketTypeDefinition.getJSONArray("plugWhitelist");

                            //TODO add for gains

                            PlugObject plugObject = new PlugObject();
                            plugObject.name = name;
                            plugObject.description = description;
                            plugObject.icon = icon;
                            plugObject.whiteList = whiteList;

                            plugObjectList.add(plugObject);
                        };

                        Message message = new Message();
                        message.what = SOCKETS_COMPLETE;
                        handler.sendMessage(message);

                    }
                } catch (Exception e) {
                    Log.d("Socket Categories", Log.getStackTraceString(e));
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
                        mods = response.getJSONObject("Response").getJSONObject("perks").getJSONObject("data").getJSONArray("perks");
                        Log.d("Perks Object", response.getJSONObject("Response").getJSONObject("perks").toString());
                        sockets = response.getJSONObject("Response").getJSONObject("sockets").getJSONObject("data").getJSONArray("sockets");
                        Log.d("Sockets Object", response.getJSONObject("Response").getJSONObject("sockets").toString());
                        stats = response.getJSONObject("Response").getJSONObject("stats").getJSONObject("data").getJSONObject("stats");
                        Log.d("Stats Object", stats.toString());

                        for (int i = 0; i < stats.names().length(); i++) {
                            String statHash = stats.names().getString(i);

                            for (int j = 0; j < statsObjectList.size(); j++) {
                                String defaultStat = statsObjectList.get(j).getStatHash();
                                if (defaultStat.equals(statHash)) {
                                    int instanceStatValue = stats.getJSONObject(stats.names().getString(i)).getInt("value");
                                    statsObjectList.get(j).value = instanceStatValue;

                                    Message message = new Message();
                                    message.what = UPDATE_STAT;
                                    handler.sendMessage(message);
                                    break;
                                }
                            }
                        }

                        //Perks commonly found in the hover over an item

                        //for (int perk = 0; perk < mods.length(); perk++) {
                            //String signedHash = ActivityMain.context.getSignedHash(mods.getJSONObject(perk).getString("perkHash"));
                            //JSONObject perkDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinySandboxPerkDefinition"));
                            //Log.d("Perk", perkDefinition.getJSONObject("displayProperties").optString("name"));
                        //}

                        //Sockets are found when configuring the item

                        for (int i = 0; i < plugObjectList.size(); i++) {
                            Log.d("Old Plug", plugObjectList.get(i).getName());
                        }

                        for (int position = 0; position < sockets.length(); position++) {
                            if (!sockets.getJSONObject(position).has("plugHash")) continue;
                            String signedHash = ActivityMain.context.getSignedHash(sockets.getJSONObject(position).getString("plugHash"));
                            JSONObject plugDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));

                            Log.d("Plug Definition", plugDefinition.toString());

                            //String plugCategoryHash = plugDefinition.getJSONObject("plug").getString("plugCategoryHash");
                            //Log.d(plugCategoryHash, plugObjectList.get(position).getWhiteList().toString());
                            String name = plugDefinition.getJSONObject("displayProperties").getString("name");
                            String description = plugDefinition.getJSONObject("displayProperties").getString("description");
                            String icon = plugDefinition.getJSONObject("displayProperties").optString("icon");

                            plugObjectList.get(position).name = name;
                            plugObjectList.get(position).description = description;
                            plugObjectList.get(position).icon = icon;

                            Message message = new Message();
                            message.what = UPDATE_PLUGS;
                            handler.sendMessage(message);

                        }

                        for (int i = 0; i < plugObjectList.size(); i++) {
                            Log.d("New Plug", plugObjectList.get(i).getName());
                        }
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

    static class PlugObject {
        String name;
        String description;
        String icon;
        String socketTypeHash;
        Boolean defaultVisible;
        JSONArray whiteList;
        JSONArray gains;

        private String getName() {
            return name;
        }

        private String getDescription() {
            return description;
        }

        private String getIcon() {
            return icon;
        }

        private String getSocketTypeHash() {
            return socketTypeHash;
        }

        private JSONArray getWhiteList() {
            return whiteList;
        }

        private JSONArray getGains() {
            return gains;
        }

        private boolean getDefaultVisible() {
            return defaultVisible;
        }
    }

    static class StatsObject {

        boolean displayAsNumeric;
        boolean isHidden;
        String statHash;
        String name;
        String description;
        int index;
        int value;
        int secondaryValue;
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

        public int getSecondaryValue() { return secondaryValue; }

        private int getMaximumValue() {
            return maximumValue;
        }

        private boolean getIsHidden () { return isHidden; }
    }

    public class SocketCategory extends RecyclerView.Adapter<SocketCategory.ViewHolder> {
        JSONArray socketCategories;
        LayoutInflater layoutInflater;

        SocketCategory(Context context, JSONArray socketCategories) {
            try {
                this.socketCategories = new JSONArray();
                this.socketCategories = socketCategories;
            } catch (Exception e) {
                Log.d("SocketCategoryAdapter", Log.getStackTraceString(e));
            }
            this.layoutInflater = LayoutInflater.from(context);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView category;
            RecyclerView socketsRecycler;
            private ViewHolder (View itemView) {
                super(itemView);
                category = itemView.findViewById(R.id.socket_category);
                socketsRecycler = itemView.findViewById(R.id.socket_list);
            };
        }

        @Override
        @NonNull
        public SocketCategory.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            View view = layoutInflater.inflate(R.layout.socket_group, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            try {
                int holderPosition = holder.getAdapterPosition();
                String socketCategoryHash = socketCategories.getJSONObject(holderPosition).getString("socketCategoryHash");
                String signedCategoryHash = ActivityMain.context.getSignedHash(socketCategoryHash);
                JSONObject socketCategoryDefinition = new JSONObject(ActivityMain.context.defineElement(signedCategoryHash, "DestinySocketCategoryDefinition"));
                String name = socketCategoryDefinition.getJSONObject("displayProperties").getString("name");
                holder.category.setText(name);

                JSONArray socketIndexes = socketCategories.getJSONObject(holderPosition).getJSONArray("socketIndexes");

                JSONArray displaySockets = new JSONArray();
                List<PlugObject> displayPlugs = new ArrayList<>();
                for (int i = 0; i < socketIndexes.length(); i++) {
                    //JSONObject socketEntry = socketEntries.getJSONObject(socketIndexes.getInt(i));
                    //Log.d("Socket Entry", socketEntry.toString());
                    displaySockets.put(socketEntries.getJSONObject(socketIndexes.getInt(i)));
                    PlugObject plugObject = new PlugObject();

                    plugObject.name = plugObjectList.get(socketIndexes.getInt(i)).getName();
                    plugObject.description = plugObjectList.get(socketIndexes.getInt(i)).getDescription();
                    plugObject.icon = plugObjectList.get(socketIndexes.getInt(i)).getIcon();

                    displayPlugs.add(plugObject);

                }

                Log.d("Sockets", displaySockets.toString());
                RecyclerView.LayoutManager socketLayoutManager = new LinearLayoutManager(getApplicationContext());
                holder.socketsRecycler.setLayoutManager(socketLayoutManager);

                socketAdapter = new SocketAdapter(getApplicationContext(), displaySockets, displayPlugs);
                holder.socketsRecycler.setAdapter(socketAdapter);
                socketAdapter.notifyDataSetChanged();

            } catch (Exception e) {
                Log.d("SocketCategory Adapter", Log.getStackTraceString(e));
            }
        }

        @Override
        public int getItemCount() {
            return this.socketCategories.length();
        }

    }

    static class SocketAdapter extends RecyclerView.Adapter<SocketAdapter.ViewHolder> {
        JSONArray sockets;
        List<PlugObject> plugObjects;
        LayoutInflater layoutInflater;

        SocketAdapter(Context context, JSONArray sockets, List<PlugObject> plugs) {
            this.plugObjects = plugs;
            this.sockets = sockets;
            this.layoutInflater = LayoutInflater.from(context);

        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name;
            TextView description;
            RecyclerView gains;
            View item;

            private ViewHolder (View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon);
                name = itemView.findViewById(R.id.name);
                description = itemView.findViewById(R.id.description);
                gains = itemView.findViewById(R.id.gains);
                item = itemView;
            };
        }

        @Override
        @NonNull
        public SocketAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            View view = layoutInflater.inflate(R.layout.socket_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            try {
                final int holderPosition = holder.getAdapterPosition();

                String singleInitialItemHash = sockets.getJSONObject(holderPosition).getString("singleInitialItemHash");
                String signedSingleInitialItemHash = ActivityMain.context.getSignedHash(singleInitialItemHash);
                JSONObject socketDefinition = new JSONObject(ActivityMain.context.defineElement(signedSingleInitialItemHash, "DestinyInventoryItemDefinition"));
                //String icon = socketDefinition.getJSONObject("displayProperties").getString("icon");
                //String name = socketDefinition.getJSONObject("displayProperties").getString("name");
                //String description = socketDefinition.getJSONObject("displayProperties").getString("description");

                String icon = plugObjects.get(holderPosition).getIcon();
                String name = plugObjects.get(holderPosition).getName();
                String description = plugObjects.get(holderPosition).getDescription();

                holder.name.setText(name);
                holder.description.setText(description);
                //todo i don't like this as a way to not show the icon. why isn't it there?
                if (icon!=null) {
                    new LoadInventoryImages(holder.icon).execute(icon);
                }

                holder.item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("Plug list " + holderPosition, plugObjects.get(holderPosition).getName());
                    }
                });

            } catch (Exception e) {
                Log.d("Socket Adapter Adapter", Log.getStackTraceString(e));
            }
        }

        @Override
        public int getItemCount() {
            return sockets.length();
        }

    }

    static class StatsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private LayoutInflater layoutInflater;
        private List<StatsObject> data;
        Context context;

        public StatsAdapter(Context context, List<StatsObject> data){
            this.layoutInflater = LayoutInflater.from(context);
            this.data = data;
            this.context = context;
        }

        static class ViewHolderBar extends RecyclerView.ViewHolder {
            TextView nameView;
            ProgressBar valueView;
            View view;
            private ViewHolderBar (View itemView) {
                super(itemView);
                view = itemView;
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
                        //viewHolderBar.valueView.setProgress(data.get(position).getSecondaryValue());
                        //viewHolderBar.valueView.setSecondaryProgress(data.get(position).getValue());
                        viewHolderBar.valueView.setProgress(data.get(position).getValue());

                        viewHolderBar.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String name = data.get(position).name;
                                int value = data.get(position).value;
                                int max = data.get(position).maximumValue;
                                Toast.makeText(context, name + " " + value + "/" + max, Toast.LENGTH_SHORT).show();
                            }
                        });
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
