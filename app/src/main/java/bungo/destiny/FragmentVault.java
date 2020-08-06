package bungo.destiny;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FragmentVault extends Fragment {

    private String vaultHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_vault_vendor);
    private String vaultBucketHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_vault_bucket);

    Data.Profile profile = ActivityMain.context.profile;

    private SearchView searchView;
    public VaultAdapter vaultAdapter;
    RecyclerView.LayoutManager layoutManager;
    RecyclerView vaultRecycler;
    Handler handler;
    View view;

    private int VAULT_BUILD_COMPLETE = 100;
    private int TRANSFER_COMPLETE = 101;

    Bitmap icon_primary;
    Bitmap icon_special;
    Bitmap icon_heavy;

    public FragmentVault() {}

    final JSONArray vaultItems = new JSONArray();

    private List<InventoryObjects> inventoryList;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inventoryList = new ArrayList<>();

        icon_primary = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_primary);
        icon_special = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_special);
        icon_heavy = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_heavy);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 100:
                        inventoryList = new Gson().fromJson(vaultItems.toString(), new TypeToken<List<InventoryObjects>>() {}.getType());
                        //sort list by name, then type
                        Collections.sort(inventoryList, new Comparator<InventoryObjects>() {
                            @Override
                            public int compare(InventoryObjects o1, InventoryObjects o2) {
                                return o1.getName().compareToIgnoreCase(o2.getName());
                            }
                        });
                        Collections.sort(inventoryList, new Comparator<InventoryObjects>() {
                            @Override
                            public int compare(InventoryObjects o1, InventoryObjects o2) {
                                return o1.getItemTypeDisplayName().compareToIgnoreCase(o2.itemTypeDisplayName);
                            }
                        });
                        vaultAdapter = new VaultAdapter(inventoryList);
                        vaultRecycler.setAdapter(vaultAdapter);
                        vaultAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    case 101:
                        vaultAdapter.notifyDataSetChanged();
                        updateVaultInventory();
                        updateUnequippedInventory();
                }
            }
        };



    }

    @Override
    public void onResume () {
        super.onResume();
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vault, container, false);

    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.view = view;

        vaultRecycler = view.findViewById(R.id.vault_rec);

        swipeRefreshLayout = ActivityMain.context.findViewById(R.id.vault_refresh_layout);
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
        //displayList = new JSONArray();
                //vaultAdapter.notifyDataSetChanged();
                updateUnequippedInventory();
                updateVaultInventory();
            }
        });
        layoutManager = new LinearLayoutManager(ActivityMain.context);
        vaultRecycler.setLayoutManager(layoutManager);

        SearchManager searchManager = (SearchManager) ActivityMain.context.getSystemService(Context.SEARCH_SERVICE);
        searchView = view.findViewById(R.id.vault_search);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(ActivityMain.context.getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                vaultAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                vaultAdapter.getFilter().filter(newText);
                return false;
            }
        });
        searchView.clearFocus();

        final ImageView vaultBackground = view.findViewById(R.id.vault_background);
        final ImageView vaultIcon = view.findViewById(R.id.vault_icon);
        vaultIcon.setColorFilter(Color.parseColor("#AA777777"));
        try {
            JSONObject vaultVendor = new JSONObject(ActivityMain.context.defineElement(vaultHash, "DestinyVendorDefinition"));
            String largeIcon = vaultVendor.getJSONObject("displayProperties").getString("largeTransparentIcon");
            String icon = vaultVendor.getJSONObject("displayProperties").getString("icon");
            new LoadInventoryImages(vaultBackground).execute(largeIcon);
            new LoadInventoryImages(vaultIcon).execute(icon);
        } catch (Exception e) {
            Log.d("Vault Definition", e.toString());
        }

    }

    public void updateUnequippedInventory () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject characterUnequipped = new DestinyAPI().updateUnequippedInventory();
                ActivityMain.context.character.setCharacterInventories(characterUnequipped);
            }
        });
    }

    public void updateVaultInventory () {

        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject profileInventory = new DestinyAPI().updateProfileInventory();
                profile.setProfileObjects(profileInventory);
            }
        });
    }

    public void  updateInventoryList () {
        Message message = new Message();
        message.what = VAULT_BUILD_COMPLETE;
        handler.sendMessage(message);
    }

    public void buildInventoryList () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject profileInventoryObject = profile.getProfileInventory();
                    JSONArray profileInventory = profileInventoryObject.getJSONObject("data").getJSONArray("items");
                    for (int i = 0; i < profileInventory.length(); i++){
                        if (profileInventory.getJSONObject(i).getString("bucketHash").equals(vaultBucketHash)) {
                            String itemHash = profileInventory.getJSONObject(i).getString("itemHash");
                            String itemInstanceId = profileInventory.getJSONObject(i).optString("itemInstanceId");
                            String quantity = profileInventory.getJSONObject(i).getString("quantity");
                            int state = profileInventory.getJSONObject(i).getInt("state");

                            String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                            JSONObject itemDefinition = new JSONObject(
                                    ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition")
                            );

                            String name = itemDefinition.getJSONObject("displayProperties").getString("name");
                            String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                            String itemTypeDisplayName = itemDefinition.getString("itemTypeDisplayName");
                            int ammoType = 0;
                            if (itemDefinition.has("equippingBlock")) ammoType = itemDefinition.getJSONObject("equippingBlock").getInt("ammoType");

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("itemHash", itemHash);
                            jsonObject.put("itemInstanceId", itemInstanceId);
                            jsonObject.put("number", quantity);
                            jsonObject.put("name", name);
                            jsonObject.put("icon", icon);
                            jsonObject.put("itemTypeDisplayName", itemTypeDisplayName);
                            jsonObject.put("state", state);
                            jsonObject.put("ammoType", ammoType);

                            if (!itemInstanceId.isEmpty()) {
                                JSONObject itemInstance = ActivityMain.context.profile.getItemComponents()
                                        .getJSONObject("instances")
                                        .getJSONObject("data")
                                        .optJSONObject(itemInstanceId);

                                if (!itemInstance.getString("damageType").equals("0")) {
                                    String damageTypeHash = itemInstance.getString("damageTypeHash");
                                    String signedDamageTypeHash = ActivityMain.context.getSignedHash(damageTypeHash);
                                    JSONObject damageTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedDamageTypeHash, "DestinyDamageTypeDefinition"));
                                    String elementIcon = damageTypeDefinition.getJSONObject("displayProperties").getString("icon");
                                    jsonObject.put("elementIcon", elementIcon);
                                }

                                JSONObject primaryStat = itemInstance.optJSONObject("primaryStat");
                                if (primaryStat != null) {
                                    String itemPower = itemInstance.getJSONObject("primaryStat").getString("value");
                                    jsonObject.put("number", itemPower);
                                }
                            }
                            vaultItems.put(jsonObject);
                        }
                    }
                } catch (Exception e) {
                    Log.d("Build Vault", e.toString());
                }

                Message message = new Message();
                message.what = VAULT_BUILD_COMPLETE;
                handler.sendMessage(message);
            }
        });
    }

    public class VaultAdapter extends RecyclerView.Adapter<VaultAdapter.VaultViewHolder> implements Filterable {

        private List<InventoryObjects> inventoryList;
        private List<InventoryObjects> inventoryListFiltered;

        class VaultViewHolder extends RecyclerView.ViewHolder {
            private ImageView inventoryItemImage;
            private ImageView inventoryItemBackground;
            private ImageView elementIcon;
            private ImageView ammunitionTypeIcon;
            private ImageView transferIcon;

            private TextView equippedText;
            private TextView type;
            private TextView itemPower;

            private View itemView;

            private VaultViewHolder (final View view) {
                super(view);
                inventoryItemImage = view.findViewById(R.id.inventory_item_image);
                inventoryItemBackground = view.findViewById(R.id.inventory_item_background);
                elementIcon = view.findViewById(R.id.element_icon);
                ammunitionTypeIcon = view.findViewById(R.id.ammunition_type_icon);
                transferIcon = view.findViewById(R.id.swap_image);

                equippedText = view.findViewById(R.id.equipped_text);
                type = view.findViewById(R.id.type);
                itemPower = view.findViewById(R.id.item_power);

                itemView = view;

            }
        }

        VaultAdapter(List<InventoryObjects> inventoryList) {
            this.inventoryListFiltered = inventoryList;
            this.inventoryList = inventoryList;
        }

        @Override
        public VaultAdapter.VaultViewHolder onCreateViewHolder (ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.character_equipped_layout, viewGroup, false);
            return new VaultAdapter.VaultViewHolder(view);
        }

        @Override
        public void onBindViewHolder (@NonNull VaultAdapter.VaultViewHolder vaultViewHolder, final int position) {
            try {
                final InventoryObjects inventoryObject = inventoryListFiltered.get(position);

                String icon = inventoryObject.getIcon();
                String name = inventoryObject.getName();
                String number = inventoryObject.getNumber();
                String elementIcon = inventoryObject.getElementIcon();
                int state = inventoryObject.getState();
                int ammoType = inventoryObject.getAmmoType();
                final String itemInstanceId = inventoryObject.getItemInstanceId();
                final String itemHash = inventoryObject.getItemHash();

                vaultViewHolder.equippedText.setText(name);
                vaultViewHolder.type.setText(inventoryObject.getItemTypeDisplayName());
                vaultViewHolder.itemPower.setText(number);

                new LoadInventoryImages(vaultViewHolder.inventoryItemImage).execute(icon);
                if (elementIcon != null && !elementIcon.isEmpty()) {
                    new LoadInventoryImages(vaultViewHolder.elementIcon).execute(elementIcon);
                } else {
                    vaultViewHolder.elementIcon.setImageBitmap(null);
                }

                if (state == 4) {
                    vaultViewHolder.inventoryItemBackground.setColorFilter(Color.parseColor("#a0f5c242"));
                } else {
                    vaultViewHolder.inventoryItemBackground.setColorFilter(null);
                }

                if (ammoType != 0) {
                    vaultViewHolder.ammunitionTypeIcon.setImageBitmap(getAmmoIcon(ammoType));
                } else {
                    vaultViewHolder.ammunitionTypeIcon.setImageBitmap(null);
                }

                vaultViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            JSONObject profileInventoryObject = profile.getProfileInventory();
                            JSONArray profileInventory = profileInventoryObject.getJSONObject("data").getJSONArray("items");
                            if (!itemInstanceId.equals("")) {
                                for (int i = 0; i < profileInventory.length(); i++) {
                                    if (profileInventory.getJSONObject(i).has("itemInstanceId")) {
                                        if (profileInventory.getJSONObject(i).getString("itemInstanceId").equals(itemInstanceId)) {
                                            Intent intent = new Intent(ActivityMain.context, ActivityItem.class);
                                            intent.putExtra("instanceString", profileInventory.getJSONObject(i).toString());
                                            ActivityMain.context.startActivity(intent);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                Intent intent = new Intent(ActivityMain.context, ActivityItem.class);
                                intent.putExtra("itemHash", itemHash);
                                ActivityMain.context.startActivity(intent);
                            }
                        }
                        catch (Exception e) {
                            Log.d("Item Click", e.toString());
                        }
                    }
                });

//todo inventory item transferStatus enum to determine transfer eligible
                vaultViewHolder.transferIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DialogCharacterPicker dialogCharacterPicker = new DialogCharacterPicker(ActivityMain.context);
                        dialogCharacterPicker.setReturnSelection(new DialogCharacterPicker.ReturnSelection() {
                            @Override
                            public void selectObject(String characterId) {
                                Log.d("Character Pick", characterId);

                                try {
                                    JSONObject fromVaultObject = new JSONObject();
                                    fromVaultObject.put("characterId", characterId);
                                    fromVaultObject.put("itemInstanceId", itemInstanceId);
                                    fromVaultObject.put("itemReferenceHash", itemHash);
                                    fromVaultObject.put("position", position);

                                    transferItemFromVault(fromVaultObject);
                                } catch (Exception e) {
                                    Log.d("Building xfer Object", e.toString());
                                }
                            }
                        });
                        dialogCharacterPicker.show();
                        Window window = dialogCharacterPicker.getWindow();
                        if (window != null) {
                            window.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                            //window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.parseColor("#CA777777")));
                        }
                    }
                });

                setAnimation(vaultViewHolder.itemView);

            } catch (Exception e) {
                Log.d("Vault Adapter", e.toString());
            }
        }

        private void setAnimation (View animateView) {
            Animation animation = AnimationUtils.loadAnimation(ActivityMain.context, android.R.anim.slide_in_left);
            animateView.startAnimation(animation);
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();
                    if (charString.isEmpty()) {
                        inventoryListFiltered = inventoryList;
                    } else {
                        List<InventoryObjects> filteredList = new ArrayList<>();
                        for (InventoryObjects row : inventoryList) {
                            if (row.getName().toLowerCase().contains(charString.toLowerCase())
                                    || row.getItemTypeDisplayName().toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(row);
                            }
                        }

                        inventoryListFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = inventoryListFiltered;
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    inventoryListFiltered = (ArrayList<InventoryObjects>) filterResults.values;
                    notifyDataSetChanged();
                }
            };
        }

        @Override
        public int getItemCount() {
            return inventoryListFiltered.size();
        }
    }

    private void transferItemFromVault (final JSONObject transferParams) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences;
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityMain.context.getApplicationContext());
                String membershipType = sharedPreferences.getString("membership_type", null);
                String characterId;
                String itemInstanceId;
                String itemReferenceHash;
                int position;
                try {
                    characterId = transferParams.getString("characterId");
                    itemInstanceId = transferParams.getString("itemInstanceId");
                    itemReferenceHash = transferParams.getString("itemReferenceHash");
                    position = transferParams.getInt("position");

                    String[] params = new String[6];
                    params[0] = membershipType;
                    params[1] = characterId;
                    params[2] = itemInstanceId;
                    params[3] = itemReferenceHash;
                    params[4] = "1"; //stack size -- always 1? IDK what this means
                    params[5] = "false";

                    DestinyAPI destinyAPI = new DestinyAPI();
                    JSONObject response = destinyAPI.transferToVault(params);

                    if (response.getInt("ErrorCode") == 1) {
                        inventoryList.remove(position);

                        Message transferMessage = new Message();
                        transferMessage.what = TRANSFER_COMPLETE;
                        handler.sendMessage(transferMessage);

                    } else {
                        Log.d("Transfer Error", response.toString());
                    }
                    Log.d("Server Response", response.toString());
                } catch (Exception e) {
                    Log.d("transfer from vault", e.toString());
                }
            }
        });
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

    private class InventoryObjects {

        String itemHash;
        String itemInstanceId;
        String name;
        String icon;
        String itemTypeDisplayName;
        String elementIcon;
        String number;
        int state;
        int ammoType;

        public String getItemHash() {
            return itemHash;
        }

        public String getItemInstanceId() {
            return itemInstanceId;
        }

        public String getName() {
            return name;
        }

        public String getIcon() {
            return icon;
        }

        public String getItemTypeDisplayName() {
            return itemTypeDisplayName;
        }

        public String getElementIcon () { return elementIcon; }

        public String getNumber () { return number; }

        public int getState () { return state; }

        public int getAmmoType () {return  ammoType; }

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

}
