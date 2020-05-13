package bungo.destiny;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.List;

public class FragmentGearSelect extends Fragment {

    private RecyclerView classRecycler;
    private RecyclerView weaponsRecycler;
    private RecyclerView statsRecycler;

    private ClassAdapter classAdapter;
    private WeaponsAdapter weaponsAdapter;
    private StatsAdapter statsAdapter;

    private JSONArray equippedItems;
    private JSONArray unequippedItems;

    private JSONArray equippedArray;
    private JSONArray unequippedSubclass;
    private JSONObject statsObject;

    String characterId = ActivityCharacter.context.characterId;

    Handler handler;
    private int EQUIPMENT_BUILT = 100;
    private int UNEQUIPPED_BUILT = 200;
    private int STATS_BUILT = 300;
    private int SUBCLASS_EQUIP_SUCCESS = 400;
    private int CHARACTER_UPDATED = 401;

    private Bitmap icon_primary;
    private Bitmap icon_special;
    private Bitmap icon_heavy;

    public FragmentGearSelect() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gear_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        equippedArray = new JSONArray();
        unequippedSubclass = new JSONArray();
        statsObject = new JSONObject();

        icon_primary = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_primary);
        icon_special = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_special);
        icon_heavy = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_heavy);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 100:
                        //Log.d("Equipped", "Data Loaded");
                        weaponsAdapter = new WeaponsAdapter(equippedArray);
                        weaponsRecycler.setAdapter(weaponsAdapter);
                        weaponsAdapter.notifyDataSetChanged();
                        break;

                    case 200:
                        //Log.d("Unequipped", classObject.toString());
                        classAdapter = new ClassAdapter(ActivityCharacter.context, unequippedSubclass);
                        classRecycler.setAdapter(classAdapter);
                        classAdapter.notifyDataSetChanged();
                        break;

                    case 300:
                        statsAdapter = new StatsAdapter(statsObject);
                        statsRecycler.setAdapter(statsAdapter);
                        statsAdapter.notifyDataSetChanged();
                        break;

                    case 400:
                        //todo update equipped and unequipped inventory
                        //todo refresh view

                        ActivityMain.threadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                ActivityMain.context.character.setCharacterObjects(new DestinyAPI().getCharacter());

                                Message characterMessage = new Message();
                                characterMessage.what = CHARACTER_UPDATED;
                                handler.sendMessage(characterMessage);
                            }
                        });
                        break;

                    case 401:
                        equippedArray = new JSONArray();
                        unequippedSubclass = new JSONArray();
                        sortUnequippedItems();
                        sortInventory(view);
                        //buildStatsBanner(view);
                        break;
                }
            }
        };

        classRecycler = view.findViewById(R.id.class_recycler);
        RecyclerView.LayoutManager classLayoutManager = new GridLayoutManager(ActivityCharacter.context, 2);
        classRecycler.setLayoutManager(classLayoutManager);

        weaponsRecycler = view.findViewById(R.id.weapons_recycler);
        RecyclerView.LayoutManager weaponLayoutManager = new LinearLayoutManager(ActivityCharacter.context, RecyclerView.VERTICAL, false);
        weaponsRecycler.setLayoutManager(weaponLayoutManager);

        statsRecycler = view.findViewById(R.id.stats_recycler);
        RecyclerView.LayoutManager statsLayoutManager = new GridLayoutManager(ActivityCharacter.context, 4);
        statsRecycler.setLayoutManager(statsLayoutManager);

        sortUnequippedItems();
        sortInventory(view);
        buildStatsBanner(view);
    }
/*
    void updateCharacters () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject characterData = new DestinyAPI().updateCharacter();
                    ActivityMain.context.character.setCharacters(characterData);
                } catch (Exception e) {
                    Log.d("Update Characters", e.toString());
                }
            }
        });

        Message message = new Message();
        message.what = CHARACTER_UPDATED;
        handler.sendMessage(message);
    }
*/
    void buildStatsBanner (final View view) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ImageView bannerBackground = view.findViewById(R.id.character_banner_background);
                try {
                    JSONObject characterObject = ActivityMain.context.character.getCharacters().getJSONObject("data").getJSONObject(characterId);
                    JSONObject stats = characterObject.getJSONObject("stats");
                    String emblemHash = characterObject.getString("emblemHash");
                    String emblemSignedHash = ActivityMain.context.getSignedHash(emblemHash);
                    JSONObject emblemDefinition = new JSONObject(ActivityMain.context.defineElement(emblemSignedHash, "DestinyInventoryItemDefinition"));
                    String secondarySpecial = emblemDefinition.getString("secondarySpecial");

                    for (int i = 0; i < stats.length(); i++) {
                        String statHash = stats.names().getString(i);
                        String signedStatHash = ActivityMain.context.getSignedHash(statHash);
                        JSONObject statDefinition = new JSONObject(ActivityMain.context.defineElement(signedStatHash, "DestinyStatDefinition"));
                        String icon = statDefinition.getJSONObject("displayProperties").getString("icon");
                        String value = stats.getString(statHash);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("icon", icon);
                        jsonObject.put("value", value);
                        statsObject.put(statHash, jsonObject);
                    }
                    new LoadImages(bannerBackground).execute(secondarySpecial);
                } catch (Exception e) {
                    Log.d("Build Banner", e.toString());
                }
                Message message = new Message();
                message.what = STATS_BUILT;
                handler.sendMessage(message);
            }
        });
    }

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

    void sortUnequippedItems() {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        unequippedItems = ActivityMain.context.character.getCharacterInventories()
                                .getJSONObject("data")
                                .getJSONObject(characterId)
                                .getJSONArray("items");

                    } catch (Exception e) {
                        Log.d("Unequipped Items", e.toString());
                    }

                    for (int i = 0; i < unequippedItems.length(); i++) {
                        String itemHash = unequippedItems.getJSONObject(i).getString("itemHash");
                        String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                        JSONObject itemDefinition = new JSONObject(
                                ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition"));

                        if (!itemDefinition.has("itemCategoryHashes")) {
                            Log.d("Categoryless", itemDefinition.toString());
                            continue;
                        }
                        JSONArray itemCategoryHashes = itemDefinition.getJSONArray("itemCategoryHashes");
                        List<String> categoryList = new Gson().fromJson(itemCategoryHashes.toString(), new TypeToken<List<String>>() {}.getType());

                        if (categoryList.contains("50")) { //subclass
                            String itemInstanceId = unequippedItems.getJSONObject(i).getString("itemInstanceId");
                            String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                            String name = itemDefinition.getJSONObject("displayProperties").getString("name");

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("icon", icon);
                            jsonObject.put("itemHash", itemHash);
                            jsonObject.put("itemInstanceId", itemInstanceId);
                            jsonObject.put("name", name);

                            unequippedSubclass.put(jsonObject);
                        }
                    }

                    Message message = new Message();
                    message.what = UNEQUIPPED_BUILT;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    Log.d("Unequipped", e.toString());
                }
            }
        });
    }

    void sortInventory (final View view) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                equippedArray = new JSONArray();
                try {
                    JSONObject equipment = ActivityMain.context.character.getCharacterEquipment()
                            .getJSONObject("data")
                            .getJSONObject(characterId);
                    equippedItems = equipment.getJSONArray("items");

                    for (int i = 0; i < equippedItems.length(); i++ ) {
                        final String itemHash = equippedItems.getJSONObject(i).getString("itemHash");
                        String itemInstanceId = equippedItems.getJSONObject(i).getString("itemInstanceId");

                        String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                        JSONObject itemDefinition = new JSONObject(
                                ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition"));
                        JSONArray itemCategoryHashes = itemDefinition.getJSONArray("itemCategoryHashes");

                        List<String> categoryList = new Gson().fromJson(itemCategoryHashes.toString(), new TypeToken<List<String>>() {}.getType());

                        if (categoryList.contains("50")) { //subclass
                            String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                            final String name = itemDefinition.getJSONObject("displayProperties").getString("name");
                            ImageView imageView = view.findViewById(R.id.class_image_equipped);
                            new LoadImages(imageView).execute(icon);

                            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    Toast.makeText(getContext(), "Configure subclass " + name, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(ActivityMain.context, ActivityClassConfig.class);
                                    intent.putExtra("hashId", itemHash);
                                    ActivityMain.context.startActivity(intent);
                                    return true;
                                }
                            });

                        } else if (categoryList.contains("1")) { //weapon
                            String name = itemDefinition.getJSONObject("displayProperties").getString("name");
                            String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                            String type = itemDefinition.getString("itemTypeDisplayName");

                            JSONObject instanceData = ActivityMain.context.character.getItemInstances().getJSONObject(itemInstanceId);

                            String elementIcon = null;
                            if (instanceData.has("damageTypeHash")) {
                                String damageTypeHash = instanceData.getString("damageTypeHash");
                                String signedDamageTypeHash = ActivityMain.context.getSignedHash(damageTypeHash);JSONObject damageTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedDamageTypeHash, "DestinyDamageTypeDefinition"));
                                elementIcon = damageTypeDefinition.getJSONObject("displayProperties").getString("icon");
                            }

                            String itemPower = instanceData.getJSONObject("primaryStat").getString("value");
                            int state = equippedItems.getJSONObject(i).getInt("state");
                            int ammoType = itemDefinition.getJSONObject("equippingBlock").getInt("ammoType");

                            JSONObject itemObject = new JSONObject();
                            itemObject.put("itemHash", itemHash);
                            itemObject.put("itemInstanceId", itemInstanceId);
                            itemObject.put("name", name);
                            itemObject.put("icon", icon);
                            itemObject.put("state", state);
                            itemObject.put("elementIcon", elementIcon);
                            itemObject.put("itemPower", itemPower);
                            itemObject.put("type", type);
                            itemObject.put("ammoType", ammoType);

                            equippedArray.put(itemObject);

                        } else if (categoryList.contains("20")) { //armor

                            String name = itemDefinition.getJSONObject("displayProperties").getString("name");
                            String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                            String type = itemDefinition.getString("itemTypeDisplayName");
                            int state = equippedItems.getJSONObject(i).getInt("state");

                            JSONObject instanceData = ActivityMain.context.character.getItemInstances().getJSONObject(itemInstanceId);
                            String itemPower = instanceData.getJSONObject("primaryStat").getString("value");

                            JSONObject itemObject = new JSONObject();
                            itemObject.put("itemHash", itemHash);
                            itemObject.put("itemInstanceId", itemInstanceId);
                            itemObject.put("name", name);
                            itemObject.put("icon", icon);
                            itemObject.put("state", state);
                            itemObject.put("itemPower", itemPower);
                            itemObject.put("type", type);

                            equippedArray.put(itemObject);

                        } else if (categoryList.contains("39")) { //ghosts
                            String name = itemDefinition.getJSONObject("displayProperties").getString("name");
                            String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                            String type = itemDefinition.getString("itemTypeDisplayName");

                            JSONObject itemObject = new JSONObject();
                            itemObject.put("itemHash", itemHash);
                            itemObject.put("itemInstanceId", itemInstanceId);
                            itemObject.put("name", name);
                            itemObject.put("icon", icon);
                            itemObject.put("type", type);

                            equippedArray.put(itemObject);
                        }
                        Message message = new Message();
                        message.what = EQUIPMENT_BUILT;
                        handler.sendMessage(message);
                    }

                } catch (Exception e) {
                    Log.d("Gear", e.toString());
                }
            }
        });
    }

    class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray data;

        ClassAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.data = data;
        }

        @Override
        @NonNull
        public ClassAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.recyclerview_subclass_item, parent, false);
            return new ClassAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ClassAdapter.ViewHolder holder, final int position) {
            try {

                final String name = data.getJSONObject(position).getString("name");
                final String itemHash = data.getJSONObject(position).getString("itemHash");
                final String itemInstanceId = data.getJSONObject(position).getString("itemInstanceId");
                String icon = data.getJSONObject(position).getString("icon");

                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ActivityCharacter.context);
                        alertDialogBuilder.setMessage("Equip subclass " + name + "?");
                        alertDialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityMain.threadPoolExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String[] params = new String[2];
                                            params[0] = itemInstanceId;
                                            params[1] = characterId;
                                            JSONObject response = new DestinyAPI().equipItem(params);
                                            if (response.getInt("ErrorCode") == 1 ) {
                                                Message recordsMessage = new Message();
                                                recordsMessage.what = SUBCLASS_EQUIP_SUCCESS;
                                                handler.sendMessage(recordsMessage);
                                            } else {
                                                String errorMessage = response.getString("Message");
                                                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                                            }
                                            Log.d("Equip Response", response.toString());
                                        } catch (Exception e) {
                                            Log.d("Class Click", e.toString());
                                        }
                                    }
                                });
                                dialog.dismiss();
                            }
                        });
                        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                });

                holder.imageView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Toast.makeText(getContext(), "Configure subclass " + name, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ActivityMain.context, ActivityClassConfig.class);
                        intent.putExtra("hashId", itemHash);
                        ActivityMain.context.startActivity(intent);
                        return true;
                    }
                });

                new LoadImages(holder.imageView).execute(icon);
            } catch (Exception e){
                Log.d("Class Adapter", e.toString());
            }
        }
        @Override
        public int getItemCount() {
            return 2;
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            ImageView imageView;

            ViewHolder(final View itemView) {
                super(itemView);
                itemView.setTag(this);
                imageView = itemView.findViewById(R.id.subclass_recycler_item);
            }
        }
    }

    public class WeaponsAdapter extends RecyclerView.Adapter<WeaponsAdapter.WeaponsViewHolder> {

        JSONArray weaponsRecyclerData;

        class WeaponsViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;
            private ImageView elementImageView;
            private ImageView inventoryItemBackground;
            private ImageView swapImageView;
            private ImageView ammoIcon;
            private TextView powerTextView;
            private TextView titleTextView;
            private TextView typeTextView;

            private WeaponsViewHolder(final View view) {
                super(view);
                //todo find and add icon for ammo type ie. primary, secondary...
                imageView = view.findViewById(R.id.inventory_item_image);
                elementImageView = view.findViewById(R.id.element_icon);
                inventoryItemBackground = view.findViewById(R.id.inventory_item_background);
                swapImageView = view.findViewById(R.id.swap_image);
                ammoIcon = view.findViewById(R.id.ammunition_type_icon);

                titleTextView = view.findViewById(R.id.equipped_text);
                powerTextView = view.findViewById(R.id.item_power);
                typeTextView = view.findViewById(R.id.type);

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        try {
                            String itemInstanceId = weaponsRecyclerData.getJSONObject(getAdapterPosition()).getString("itemInstanceId");
                            for ( int i = 0; i < equippedItems.length() ; i++ ) {
                                if (equippedItems.getJSONObject(i).getString("itemInstanceId").equals(itemInstanceId)) {
                                    String instanceString = String.valueOf(equippedItems.getJSONObject(i));
                                    Intent intent = new Intent(ActivityCharacter.context, ActivityItem.class);
                                    intent.putExtra("instanceString", instanceString);
                                    ActivityCharacter.context.startActivity(intent);
                                }
                            }
                        } catch (Exception e) {
                            Log.d("Long Click Item", e.toString());
                        }
                        return true;
                    }
                });
            }
        }

        WeaponsAdapter(JSONArray dataArray) {
            try {
                weaponsRecyclerData = dataArray;
            } catch (Exception e) {
                Log.d("Weapons Recycler", e.toString());
            }
        }

        @Override
        @NonNull
        public WeaponsAdapter.WeaponsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.character_equipped_layout, viewGroup, false);
            return new WeaponsAdapter.WeaponsViewHolder(view);

        }

        @Override
        public void onBindViewHolder(@NonNull final WeaponsAdapter.WeaponsViewHolder viewHolder, int position) {
            try {
                JSONObject item = weaponsRecyclerData.getJSONObject(viewHolder.getAdapterPosition());
                final String itemHash = item.getString("itemHash");
                final String itemInstanceId = item.getString("itemInstanceId");
                //Log.d(itemHash, itemInstanceId);

                String icon = item.getString("icon");
                String name = item.getString("name");
                String elementIcon = item.optString("elementIcon");
                String itemPower = item.optString("itemPower");
                int state = item.optInt("state");
                int ammoType = item.optInt("ammoType");
                String type = item.getString("type");

                Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.missing_icon_d2);
                viewHolder.imageView.setImageBitmap(defaultBitmap);
                viewHolder.titleTextView.setText(name);
                viewHolder.powerTextView.setText(itemPower);
                viewHolder.typeTextView.setText(type);

                if (state == 4) {
                    viewHolder.inventoryItemBackground.setColorFilter(Color.parseColor("#a0f5c242"));
                } else {
                    viewHolder.inventoryItemBackground.setColorFilter(null);
                }
                if (!elementIcon.isEmpty()) {
                    new LoadImages(viewHolder.elementImageView).execute(elementIcon);
                } else {
                    viewHolder.elementImageView.setImageBitmap(null);
                }
                if (item.has("ammoType") && ammoType != 0) {
                    viewHolder.ammoIcon.setImageBitmap(getAmmoIcon(ammoType));
                } else {
                    viewHolder.ammoIcon.setImageBitmap(null);
                }

                new LoadImages(viewHolder.imageView).execute(icon);

                viewHolder.swapImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogItemPicker dialogItemPicker = new DialogItemPicker(ActivityCharacter.context, itemHash);
                        dialogItemPicker.setReturnSelection(new DialogItemPicker.ReturnSelection() {
                            @Override
                            public void selectObject(JSONObject itemData) {
                                equipItem(itemData, viewHolder.getAdapterPosition());
                            }
                        });
                        dialogItemPicker.show();
                        Window window = dialogItemPicker.getWindow();
                        if (window != null) window.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
                    }
                });
            } catch (Exception e) {
                Log.d("WeaponsAdapter", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return weaponsRecyclerData.length();
        }

    }

    private void equipItem (JSONObject returnedItem, int position) {
        try {
            equippedArray.put(position, returnedItem);
            weaponsAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.d("equippedArray", e.toString());
        }
    }

    public class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatsViewHolder> {

        JSONObject statsRecyclerData;

        class StatsViewHolder extends RecyclerView.ViewHolder {
            private ImageView statIconImageView;
            private TextView statValueTextView;

            private StatsViewHolder(final View view) {
                super(view);
                statIconImageView = view.findViewById(R.id.character_stat_icon);
                statValueTextView = view.findViewById(R.id.character_stat_value);

            }
        }

        StatsAdapter(JSONObject dataObject) {
            try {
                statsRecyclerData = dataObject;
            } catch (Exception e) {
                Log.d("Stats Recycler", e.toString());
            }
        }

        @Override
        @NonNull
        public StatsAdapter.StatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.gear_fragment_stats, viewGroup, false);
            return new StatsAdapter.StatsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StatsViewHolder statsViewHolder, final int position) {
            try {
                JSONObject statObject = statsRecyclerData.getJSONObject(statsRecyclerData.names().getString(position));
                String icon = statObject.getString("icon");
                String value = statObject.getString("value");

                statsViewHolder.statValueTextView.setText(value);
                new LoadImages(statsViewHolder.statIconImageView).execute(icon);
            } catch (Exception e) {
                Log.d("Stats Adapter", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return statsRecyclerData.length();
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
            Bitmap icon;
            try {
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
