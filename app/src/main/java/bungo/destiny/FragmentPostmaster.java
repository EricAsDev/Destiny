package bungo.destiny;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class FragmentPostmaster extends Fragment {

    private String postmasterVendorHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_postmaster_vendor);
    private String postmasterHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_postmaster);

    //Data.Profile profile = ActivityMain.context.profile;

    public PostmasterAdapter postmasterAdapter;
    RecyclerView.LayoutManager layoutManager;
    RecyclerView postmasterRecycler;
    Handler handler;
    View view;

    private int POSTMASTER_ITEM_ADDED = 100;
    private int POSTMASTER_REFRESHED = 101;
    private int ITEM_PULLED = 102;

    Bitmap icon_primary;
    Bitmap icon_special;
    Bitmap icon_heavy;

    public FragmentPostmaster() {}

    final JSONArray postmasterItems = new JSONArray();

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        icon_primary = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_primary);
        icon_special = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_special);
        icon_heavy = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_heavy);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 100: //POSTMASTER ITEM ADDED
                        postmasterAdapter.notifyDataSetChanged();
                        break;
                    case 101: //POSTMASTER UPDATED
                        postmasterRecycler.setAdapter(postmasterAdapter);
                        postmasterAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        //updateVaultInventory();
                        //updatePostmaster();
                        break;
                    case 102: //ITEM PULLED FROM POSTMASTER
                        break;

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

        postmasterRecycler = view.findViewById(R.id.vault_rec);

        swipeRefreshLayout = ActivityMain.context.findViewById(R.id.vault_refresh_layout);
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updatePostmaster();
            }
        });

        layoutManager = new LinearLayoutManager(ActivityMain.context);
        postmasterAdapter = new PostmasterAdapter(postmasterItems);
        postmasterRecycler.setLayoutManager(layoutManager);
        postmasterRecycler.setAdapter(postmasterAdapter);

        buildInventoryList();

        final ImageView vaultBackground = view.findViewById(R.id.vault_background);
        final ImageView vaultIcon = view.findViewById(R.id.vault_icon);
        vaultIcon.setColorFilter(Color.parseColor("#AA777777"));
        try {
            JSONObject postmasterVendor = new JSONObject(ActivityMain.context.defineElement(postmasterVendorHash, "DestinyVendorDefinition"));
            String largeIcon = postmasterVendor.getJSONObject("displayProperties").getString("largeTransparentIcon");
            String icon = postmasterVendor.getJSONObject("displayProperties").getString("icon");
            new LoadInventoryImages(vaultBackground).execute(largeIcon);
            new LoadInventoryImages(vaultIcon).execute(icon);
        } catch (Exception e) {
            Log.d("Postmaster Definition", Log.getStackTraceString(e));
        }

    }

    public void updatePostmaster() {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {



            }
        });
    }

    public void buildInventoryList () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject inventoryObject = ActivityMain.context.character.getCharacterInventories();
                    JSONArray profileInventory = inventoryObject.getJSONObject("data").getJSONObject(ActivityCharacter.context.characterId).getJSONArray("items");
                    for (int i = 0; i < profileInventory.length(); i++){
                        if (profileInventory.getJSONObject(i).getString("bucketHash").equals(postmasterHash)) {
                            postmasterItems.put(profileInventory.getJSONObject(i));
                            //Log.d("Postmaster", profileInventory.getJSONObject(i).toString());
                        }
                    }
                } catch (Exception e) {
                    Log.d("Build Postmaster", Log.getStackTraceString(e));
                }

                Message message = new Message();
                message.what = POSTMASTER_ITEM_ADDED;
                handler.sendMessage(message);


            }
        });
    }

    public class PostmasterAdapter extends RecyclerView.Adapter<PostmasterAdapter.PostmasterViewHolder> {

        private JSONArray postmasterItems;

        class PostmasterViewHolder extends RecyclerView.ViewHolder {
            private ImageView inventoryItemImage;
            private ImageView inventoryItemBackground;
            private ImageView elementIcon;
            private ImageView ammunitionTypeIcon;
            private ImageView transferIcon;

            private TextView equippedText;
            private TextView type;
            private TextView itemPower;

            private PostmasterViewHolder(final View view) {
                super(view);
                inventoryItemImage = view.findViewById(R.id.inventory_item_image);
                inventoryItemBackground = view.findViewById(R.id.inventory_item_background);
                elementIcon = view.findViewById(R.id.element_icon);
                ammunitionTypeIcon = view.findViewById(R.id.ammunition_type_icon);
                transferIcon = view.findViewById(R.id.swap_image);

                equippedText = view.findViewById(R.id.equipped_text);
                type = view.findViewById(R.id.type);
                itemPower = view.findViewById(R.id.item_power);

            }
        }

        PostmasterAdapter(JSONArray data) {
            this.postmasterItems = data;
        }

        @Override
        public PostmasterViewHolder onCreateViewHolder (ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.quest_item_layout, viewGroup, false);
            return new PostmasterViewHolder(view);
        }

        @Override
        public void onBindViewHolder (@NonNull PostmasterViewHolder vaultViewHolder, final int position) {
            try {
                JSONObject inventoryObject = postmasterItems.getJSONObject(position);
/*
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

 */
//todo inventory item transferStatus enum to determine transfer eligible
                vaultViewHolder.transferIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(ActivityCharacter.context, "Click Postmaster Item", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.d("Postmaster Adapter", Log.getStackTraceString(e));
            }
        }

        @Override
        public int getItemCount() {
            return postmasterItems.length();
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
                        //inventoryList.remove(position);
/*
                        Message transferMessage = new Message();
                        transferMessage.what = TRANSFER_COMPLETE;
                        handler.sendMessage(transferMessage);
 */
                    } else {
                        Log.d("Transfer Error", response.toString());
                    }
                    Log.d("Server Response", response.toString());
                } catch (Exception e) {
                    Log.d("transfer from vault", Log.getStackTraceString(e));
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
