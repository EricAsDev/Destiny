package bungo.destiny;

import android.content.ClipData;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
    //JSONArray ammoTypes;

    RecyclerView.LayoutManager statsLayoutManager;
    StatsAdapter statsAdapter;
    RecyclerView statsRecycler;

    RecyclerView.LayoutManager categoryLayoutManager;
    SocketCategoryAdapter socketsCategories;
    RecyclerView socketCategoryRecycler;

    SocketAdapter socketAdapter;

    Handler handler;
    final int STATS_COMPLETE = 100;
    final int SOCKETS_COMPLETE = 101;
    final int INSTANCES_COMPLETE = 102;
    final int UPDATE_STAT = 103;
    final int UPDATE_PLUGS = 104;

    String itemHash;
    /*
    String kineticURL = "/img/destiny_content/ammo_types/primary.png";
    String specialURL = "/img/destiny_content/ammo_types/special.png";
    String heavyURL = "/img/destiny_content/ammo_types/heavy.png";
     */

    JSONArray statsDisplay;
    JSONObject instanceData;

    public Context context;

    private List<StatsObject> statsObjectList;
    private List<PlugObject> plugObjectList = new ArrayList<>();
    private List<String> itemCategoryList = new ArrayList<>(Arrays.asList(
            "1", "18", "19", "20", "34", "35", "39", "40",
            "41", "42", "43", "51", "55", "56", "57", "59"));

    Data data = new Data();
    Data.Ammunition ammunition = data.new Ammunition();

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
                //Log.d("Instance String", instanceString.toString());
                int state = instanceData.getInt("state");
                ImageView inventoryItemBackground = findViewById(R.id.inventory_item_background);
                int FLAG_3 = 1<<2;
                if (0 != (state & FLAG_3)) {
                    inventoryItemBackground.setColorFilter(Color.parseColor("#a0f5c242"));
                    Log.d("MASTERWORK", itemHash);
                }
/*
                //flag testing
                int FLAG_1 = 1<<0;
                int FLAG_2 = 1<<1;
                int FLAG_3 = 1<<2;
                int FLAG_4 = 1<<3;

                for (int X = 0; X < 10; X++) {

                    if (0 != (X & FLAG_1)) {
                        Log.d("BITMASK TEST " + X, "flag 1");
                    }
                    if (0 != (X & FLAG_2)) {
                        Log.d("BITMASK TEST " + X, "flag 2");
                    }
                    if (0 != (X & FLAG_3)) {
                        Log.d("BITMASK TEST " + X, "flag 4");
                    }
                    if (0 != (X & FLAG_4)) {
                        Log.d("BITMASK TEST " + X, "flag 8");
                    }
                }



 */
            } catch (Exception e) {
                Log.d("Instance String", Log.getStackTraceString(e));
            }
        }

        try {
            String signedHash = ActivityMain.context.getSignedHash(itemHash);
            Log.d("signed hash", signedHash);
            JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
            JSONArray itemCategoryHashes = itemDefinition.getJSONArray("itemCategoryHashes");

            LoadTitle(itemDefinition);

            List<String> itemCategories = new ArrayList<>();

            for (int i = 0; i < itemCategoryHashes.length(); i++) {
                itemCategories.add(itemCategoryHashes.getString(i));
            }

            itemCategoryList.retainAll(itemCategories);
            if (itemCategoryList.get(0).equals("1") || itemCategoryList.get(0).equals("20")) {
                String itemInstanceId = instanceData.getString("itemInstanceId");
                findViewById(R.id.armor_weapon).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_weapon).setVisibility(View.VISIBLE);
                getDefaultStats(itemDefinition);
                getSocketCategories(itemHash);
                getDefaultDetails(itemDefinition, itemInstanceId);
            }

            /*
            if (itemCategories.size() > 0) {
                for (int i = 0; i < itemCategoryHashes.length(); i++) {
                    itemCategories.add(itemCategoryHashes.getString(i));
                }

                itemCategoryList.retainAll(itemCategories);
                if (itemCategoryList.get(0).equals("1") || itemCategoryList.get(0).equals("20")) {
                    String itemInstanceId = instanceData.getString("itemInstanceId");
                    findViewById(R.id.armor_weapon).setVisibility(View.VISIBLE);
                    findViewById(R.id.layout_weapon).setVisibility(View.VISIBLE);
                    getDefaultStats(itemDefinition);
                    getSocketCategories(itemHash);
                    getDefaultDetails(itemDefinition, itemInstanceId);
                }
            }

             */

        } catch (Exception e) {
            Log.d("Define Item", Log.getStackTraceString(e));
        }

        statsDisplay = new JSONArray();
        //String state;

        statsRecycler = findViewById(R.id.stats);
        statsLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        statsRecycler.setLayoutManager(statsLayoutManager);

        socketCategoryRecycler = findViewById(R.id.socket_categories);
        categoryLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        socketCategoryRecycler.setLayoutManager(categoryLayoutManager);

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
                        socketsCategories = new SocketCategoryAdapter(getApplicationContext(), socketCategories);
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

    void getDefaultDetails (JSONObject itemDefinition, String itemInstanceId) {
        try {
            ImageView ammo = findViewById(R.id.ammo_type);
            ImageView element = findViewById(R.id.element);
            TextView level = findViewById(R.id.light_level);
            TextView type = findViewById(R.id.display_type);
            TextView stat_name = findViewById(R.id.stat_name);

            int ammoType = itemDefinition.getJSONObject("equippingBlock").getInt("ammoType");
            if (ammoType > 0 && ammoType < 4) {
                JSONObject ammoObject = ammunition.getAmmoData(ammoType);
                String ammoURL = ammoObject.getString("icon");
                String ammoName = ammoObject.getString("name");
                type.setText(ammoName);
                new LoadInventoryImages(ammo).execute(ammoURL);
            }

            JSONObject instanceData = ActivityMain.context.character.getItemInstances().getJSONObject(itemInstanceId);

            String primaryStatHash = instanceData.getJSONObject("primaryStat").getString("statHash");
            String primaryStatValue = instanceData.getJSONObject("primaryStat").getString("value");
            String signedPrimaryStatHash = ActivityMain.context.getSignedHash(primaryStatHash);
            JSONObject statDefinition = new JSONObject(ActivityMain.context.defineElement(signedPrimaryStatHash, "DestinyStatDefinition"));
            String statName = statDefinition.getJSONObject("displayProperties").getString("name");

            level.setText(primaryStatValue);
            stat_name.setText(statName);

            String damageTypeHash = instanceData.optString("damageTypeHash");
            if (damageTypeHash != null) {
                String signedHash = ActivityMain.context.getSignedHash(damageTypeHash);
                JSONObject damageTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyDamageTypeDefinition"));
                String icon = damageTypeDefinition.getJSONObject("displayProperties").getString("icon");
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
                                Log.d("Creating Blank Object", socketEntry.toString());
                                PlugObject plugObject = new PlugObject();
                                plugObject.defaultVisible = socketEntry.getBoolean("defaultVisible");
                                plugObjectList.add(plugObject);
                                continue;
                            };

                            String signedSingleInitialItemHash = ActivityMain.context.getSignedHash(singleInitialItemHash);
                            JSONObject socketDefinition = new JSONObject(ActivityMain.context.defineElement(signedSingleInitialItemHash, "DestinyInventoryItemDefinition"));

                            String icon = socketDefinition.getJSONObject("displayProperties").getString("icon");
                            String name = socketDefinition.getJSONObject("displayProperties").getString("name");
                            String description = socketDefinition.getJSONObject("displayProperties").getString("description");
                            String socketTypeHash = socketEntry.getString("socketTypeHash");

                            String signedSocketTypeHash = ActivityMain.context.getSignedHash(socketTypeHash);
                            JSONObject socketTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedSocketTypeHash, "DestinySocketTypeDefinition"));
                            JSONArray whiteList = socketTypeDefinition.getJSONArray("plugWhitelist");

                            JSONArray investmentStats = socketDefinition.getJSONArray("investmentStats");

                            PlugObject plugObject = new PlugObject();
                            plugObject.name = name;
                            plugObject.description = description;
                            plugObject.icon = icon;
                            plugObject.whiteList = whiteList;
                            plugObject.investmentStats = investmentStats;

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
                            String statHash = stats.names().getString(i);for (int j = 0; j < statsObjectList.size(); j++) {
                                String defaultStat = statsObjectList.get(j).getStatHash();
                                if (defaultStat.equals(statHash)) {
                                    statsObjectList.get(j).value = stats.getJSONObject(stats.names().getString(i)).getInt("value");

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
                        for (int position = 0; position < sockets.length(); position++) {
                            if (!sockets.getJSONObject(position).has("plugHash")) continue;
                            String signedHash = ActivityMain.context.getSignedHash(sockets.getJSONObject(position).getString("plugHash"));
                            JSONObject plugDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
                            JSONArray investmentStats = plugDefinition.getJSONArray("investmentStats");
                            //String plugCategoryHash = plugDefinition.getJSONObject("plug").getString("plugCategoryHash");
                            //Log.d(plugCategoryHash, plugObjectList.get(position).getWhiteList().toString());
                            String name = plugDefinition.getJSONObject("displayProperties").getString("name");
                            String description = plugDefinition.getJSONObject("displayProperties").getString("description");
                            String icon = plugDefinition.getJSONObject("displayProperties").optString("icon");

                            plugObjectList.get(position).name = name;
                            plugObjectList.get(position).description = description;
                            plugObjectList.get(position).icon = icon;
                            plugObjectList.get(position).investmentStats = investmentStats;

                            Message message = new Message();
                            message.what = UPDATE_PLUGS;
                            handler.sendMessage(message);

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
        JSONArray investmentStats;

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

        private JSONArray getInvestmentStats() {
            return investmentStats;
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

    public class SocketCategoryAdapter extends RecyclerView.Adapter<SocketCategoryAdapter.ViewHolder> {
        JSONArray socketCategories;
        LayoutInflater layoutInflater;

        SocketCategoryAdapter(Context context, JSONArray socketCategories) {
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
            }
        }

        @Override
        @NonNull
        public SocketCategoryAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
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

                JSONArray socketIndexes = socketCategories.getJSONObject(holderPosition).getJSONArray("socketIndexes");

                List<PlugObject> displayPlugs = new ArrayList<>();
                for (int i = 0; i < socketIndexes.length(); i++) {
                    PlugObject plugObject = new PlugObject();

                    plugObject.name = plugObjectList.get(socketIndexes.getInt(i)).getName();
                    plugObject.description = plugObjectList.get(socketIndexes.getInt(i)).getDescription();
                    plugObject.icon = plugObjectList.get(socketIndexes.getInt(i)).getIcon();
                    plugObject.investmentStats = plugObjectList.get(socketIndexes.getInt(i)).getInvestmentStats();

                    displayPlugs.add(plugObject);
                }

                holder.category.setText(name);
                RecyclerView.LayoutManager socketLayoutManager = new LinearLayoutManager(getApplicationContext());
                holder.socketsRecycler.setLayoutManager(socketLayoutManager);

                socketAdapter = new SocketAdapter(getApplicationContext(), displayPlugs);
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

    static class InvestmentStatsAdapter extends RecyclerView.Adapter<InvestmentStatsAdapter.ViewHolder> {
        JSONArray investmentStats;
        LayoutInflater layoutInflater;

        InvestmentStatsAdapter(Context context, JSONArray data) {
            this.investmentStats = data;
            layoutInflater = LayoutInflater.from(context);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            TextView value;
            private ViewHolder (View view) {
                super(view);
                name = view.findViewById(R.id.name);
                value = view.findViewById(R.id.value);
            }
        }
        @Override
        @NonNull
        public InvestmentStatsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            View view = layoutInflater.inflate(R.layout.investment_stat_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            try {
                final int holderPosition = holder.getAdapterPosition();
                String value = investmentStats.getJSONObject(holderPosition).getString("value");
                String statTypeHash = investmentStats.getJSONObject(holderPosition).getString("statTypeHash");
                String signedStatTypeHash = ActivityMain.context.getSignedHash(statTypeHash);
                JSONObject statTypeDefinition= new JSONObject(ActivityMain.context.defineElement(signedStatTypeHash, "DestinyStatDefinition"));
                String name = statTypeDefinition.getJSONObject("displayProperties").getString("name");

                holder.name.setText(name);
                holder.value.setText(value);
            } catch (Exception e) {
                Log.d("InvestmentStatsAdapter", Log.getStackTraceString(e));
            }
        }

        @Override
        public int getItemCount() {
            return investmentStats.length();
        }
    }

    static class SocketAdapter extends RecyclerView.Adapter<SocketAdapter.ViewHolder> {
        List<PlugObject> plugObjects;
        LayoutInflater layoutInflater;
        Context context;

        SocketAdapter(Context context, List<PlugObject> plugs) {
            this.plugObjects = plugs;
            this.context = context;
            this.layoutInflater = LayoutInflater.from(context);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name;
            TextView description;
            RecyclerView investmentStats;
            View item;

            private ViewHolder (View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon);
                name = itemView.findViewById(R.id.name);
                description = itemView.findViewById(R.id.description);
                investmentStats = itemView.findViewById(R.id.investmentStats);
                item = itemView;
            }
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

                String icon = plugObjects.get(holderPosition).getIcon();
                String name = plugObjects.get(holderPosition).getName();
                String description = plugObjects.get(holderPosition).getDescription();

                holder.name.setText(name);
                holder.description.setText(description);
                //todo i don't like this as a way to not show the icon. why isn't it there?
                if (icon!=null) {
                    new LoadInventoryImages(holder.icon).execute(icon);
                }

                JSONArray investmentStats = plugObjects.get(holderPosition).getInvestmentStats();
                if (investmentStats!=null && investmentStats.length() > 0) {
                    RecyclerView investmentStatsRecycler = holder.investmentStats;
                    investmentStatsRecycler.setVisibility(View.VISIBLE);
                    InvestmentStatsAdapter investmentStatsAdapter = new InvestmentStatsAdapter(context, investmentStats);

                    RecyclerView.LayoutManager investmentStatsLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                    investmentStatsRecycler.setLayoutManager(investmentStatsLayoutManager);
                    investmentStatsRecycler.setAdapter(investmentStatsAdapter);
                    investmentStatsAdapter.notifyDataSetChanged();
                }

                holder.item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("Plug list " + holderPosition, plugObjects.get(holderPosition).getIcon());
                    }
                });

            } catch (Exception e) {
                Log.d("Socket Adapter", Log.getStackTraceString(e));
            }
        }

        @Override
        public int getItemCount() {
            return plugObjects.size();
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
                            viewHolderBar.nameView.setTextColor(Color.parseColor("#c3bcb4"));
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
                            viewHolderNumber.nameView.setTextColor(Color.parseColor("#c3bcb4"));
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
            //Log.d("Icon URL", iconUrl);
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
                Log.d("LoadInventoryImages", Log.getStackTraceString(e));
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
