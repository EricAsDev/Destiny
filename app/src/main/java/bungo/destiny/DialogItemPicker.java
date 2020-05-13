package bungo.destiny;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class DialogItemPicker extends Dialog implements View.OnClickListener {

    private String itemHash;
    private String characterId;
    JSONArray displayList;
    Handler handler;
    private String postmasterHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_postmaster);

    private int selectedItem = -1;

    private Bitmap icon_primary;
    private Bitmap icon_special;
    private Bitmap icon_heavy;

    private int EQUIPMENT_BUILT = 100;
    private int TRANSFERRED_TO_VAULT = 101;
    private int VAULT_UPDATED = 102;

    private ReturnSelection returnSelection;

    private ItemsAdapter itemsAdapter;
    private RecyclerView itemsRecycler;
    RecyclerView.LayoutManager itemsLayoutManager;


    public DialogItemPicker(Activity context, String data) {
        super(context);
        this.itemHash = data;
        this.returnSelection = null;
        this.characterId = ActivityCharacter.context.characterId;
    }
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_item_select);
        TextView buttonEquip = findViewById(R.id.picker_button_equip);
        TextView buttonCancel = findViewById(R.id.picker_button_cancel);
        TextView buttonVault = findViewById(R.id.picker_button_vault);

        buttonEquip.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);
        buttonVault.setOnClickListener(this);

        icon_primary = BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.icon_ammo_primary);
        icon_special = BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.icon_ammo_special);
        icon_heavy = BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.icon_ammo_heavy);

        itemsRecycler = findViewById(R.id.picker_recycler);
        itemsLayoutManager = new LinearLayoutManager(ActivityCharacter.context, RecyclerView.VERTICAL, false);
        itemsRecycler.setLayoutManager(itemsLayoutManager);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 100://equipment built
                        itemsAdapter = new ItemsAdapter(displayList);
                        itemsRecycler.setAdapter(itemsAdapter);
                        itemsAdapter.notifyDataSetChanged();
                        break;
                    case 101://transferred to vault
                        itemsAdapter.notifyDataSetChanged();
                        updateVault();
                        updateUnequipped();
                        ActivityMain.context.fragmentVaultSelect.updateInventoryList();
                        selectedItem = -1;
                        break;
                    case 102://vault updated
                        Log.d("Inventory Update", "Completed");
                        break;
                }
            }
        };

        listUnequippedItems(itemHash);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.picker_button_equip:
                Log.d("Button", "Equip");
                equipItem(selectedItem);
                try {
                    returnSelection.selectObject(getReturnData(selectedItem));
                } catch (Exception e) {
                    Log.d("Equip Button", e.toString());
                }
                selectedItem = -1;
                dismiss();
                break;
            case R.id.picker_button_cancel:
                Log.d("Button", "Cancel");
                dismiss();
                break;
            case R.id.picker_button_vault:
                Log.d("Button", "Vault");
                try {
                    JSONObject selectedObject = displayList.getJSONObject(selectedItem);
                    String itemInstanceId = selectedObject.getString("itemInstanceId");
                    String itemHash = selectedObject.getString("itemHash");
                    transferItemToVault(itemInstanceId, itemHash);
                } catch (Exception e) {
                    Log.d("Vault Button", e.toString());
                }
                break;
        }
    }

    private JSONObject getReturnData (int selectedItem) {
        JSONObject returnData = new JSONObject();
        try {
            JSONObject selectedObject = displayList.getJSONObject(selectedItem);
            String itemInstanceId = selectedObject.getString("itemInstanceId");
            String itemHash = selectedObject.getString("itemHash");
            String icon = selectedObject.getString("icon");
            String name = selectedObject.getString("name");
            String elementIcon = selectedObject.optString("elementIcon");
            String itemPower = selectedObject.optString("itemPower");
            int state = selectedObject.optInt("state");
            int ammoType = selectedObject.optInt("ammoType");
            String type = selectedObject.getString("type");

            returnData.put("itemInstanceId", itemInstanceId);
            returnData.put("itemHash", itemHash);
            returnData.put("icon", icon);
            returnData.put("name", name);
            returnData.put("elementIcon", elementIcon);
            returnData.put("itemPower", itemPower);
            returnData.put("state", state);
            returnData.put("ammoType", ammoType);
            returnData.put("type", type);

        } catch (Exception e) {
            Log.d("getReturnData", e.toString());
        }
        return returnData;
    }

    private void updateUnequipped () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject unequipped = new DestinyAPI().updateUnequippedInventory();
                    ActivityMain.context.character.setCharacterInventories(unequipped);
                } catch (Exception e) {
                    Log.d("Update Unequipped", e.toString());
                }
            }
        });
        Message message = new Message();
        message.what = VAULT_UPDATED;
        handler.sendMessage(message);

    }

    private void updateVault () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject profileInventory = new DestinyAPI().updateProfileInventory();
                    ActivityMain.context.profile.setProfileInventory(profileInventory);
                } catch (Exception e) {
                    Log.d("Update Vault", e.toString());
                }
            }
        });
        Message message = new Message();
        message.what = VAULT_UPDATED;
        handler.sendMessage(message);
    }

    private void equipItem (final int position) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String itemInstanceId = displayList.getJSONObject(position).getString("itemInstanceId");
                    String[] params = new String[2];
                    params[0] = itemInstanceId;
                    params[1] = characterId;
                    JSONObject response = new DestinyAPI().equipItem(params);
                    if (response.getInt("ErrorCode") == 1) {
                        updateUnequipped();
                        displayList.remove(position);
                    } else {
                        Log.d("Transfer Error", response.toString());
                    }
                } catch (Exception e) {
                    Log.d("Equip Item", e.toString());
                }
            }});
    }

    private void listUnequippedItems (final String sourceItemHash) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    displayList = new JSONArray();
                    JSONArray unequippedItems = ActivityMain.context.character.getCharacterInventories()
                            .getJSONObject("data")
                            .getJSONObject(characterId)
                            .getJSONArray("items");
                    String sourceItemSignedHashed = ActivityMain.context.getSignedHash(sourceItemHash);
                    JSONObject sourceItemDefinition = new JSONObject(ActivityMain.context.defineElement(sourceItemSignedHashed, "DestinyInventoryItemDefinition"));

                    String equipmentSlotTypeHash = sourceItemDefinition.getJSONObject("equippingBlock").getString("equipmentSlotTypeHash");

                    for (int i = 0; i < unequippedItems.length(); i++) {
                        String itemHash = unequippedItems.getJSONObject(i).getString("itemHash");
                        String signedItemHashId = ActivityMain.context.getSignedHash(itemHash);
                        //Filter out postmaster items, since they're added to unequipped inventory
                        if (unequippedItems.getJSONObject(i).getString("bucketHash").equals(postmasterHash)) {
                            Log.d("Postmaster", itemHash);
                            continue;
                        }

                        JSONObject unequippedItemDefinition = new JSONObject(ActivityMain.context.defineElement(signedItemHashId, "DestinyInventoryItemDefinition"));

                        if (unequippedItemDefinition.optBoolean("redacted")) {
                            Log.d("Redacted", unequippedItemDefinition.toString());
                            continue;
                        }

                        if (!unequippedItemDefinition.has("equippingBlock"))  continue;

                        String itemEquipmentSlot = unequippedItemDefinition.optJSONObject("equippingBlock").getString("equipmentSlotTypeHash");
                        if (itemEquipmentSlot.equals(equipmentSlotTypeHash)) {
                            String itemInstanceId = unequippedItems.getJSONObject(i).getString("itemInstanceId");
                            String name = unequippedItemDefinition.getJSONObject("displayProperties").getString("name");
                            String type = unequippedItemDefinition.getString("itemTypeDisplayName");
                            String icon = unequippedItemDefinition.getJSONObject("displayProperties").getString("icon");
                            int state = unequippedItems.getJSONObject(i).getInt("state");
                            int ammoType = unequippedItemDefinition.getJSONObject("equippingBlock").getInt("ammoType");
                            String itemPower = "";
                            String elementIcon = "";

                            JSONObject instanceData = ActivityMain.context.character.getItemInstances().getJSONObject(itemInstanceId);

                            if (instanceData.has("primaryStat")) itemPower = instanceData.getJSONObject("primaryStat").getString("value");
                            if (instanceData.has("damageTypeHash")) {
                                String damageTypeHash = instanceData.getString("damageTypeHash");
                                String signedDamageTypeHash = ActivityMain.context.getSignedHash(damageTypeHash);
                                JSONObject damageTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedDamageTypeHash, "DestinyDamageTypeDefinition"));
                                elementIcon = damageTypeDefinition.getJSONObject("displayProperties").getString("icon");
                            }

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

                            displayList.put(itemObject);

                        }
                    }
                } catch (Exception e) {
                    Log.d("Unequipped Sort Error", e.toString());
                }

                Message message = new Message();
                message.what = EQUIPMENT_BUILT;
                handler.sendMessage(message);
            }
        });
    }

    public interface ReturnSelection {
        void selectObject (JSONObject itemData);

    }

    public void setReturnSelection (ReturnSelection returnSelection) {
        this.returnSelection = returnSelection;
    }

    public class DividerItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable divider;
        public DividerItemDecoration(Drawable divider) {
            this.divider = divider;
        };
        @Override
        public void getItemOffsets (Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            if (parent.getChildAdapterPosition(view) == 0) {
                return;
            }
            outRect.top = divider.getIntrinsicHeight();
        }
        @Override
        public void onDraw (Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            int dividerLeft = parent.getPaddingLeft();
            int dividerRight = parent.getWidth()-parent.getPaddingRight();
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount - 1; i++) {
                View child = parent.getChildAt(i);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
                int dividerTop = child.getBottom() + layoutParams.bottomMargin;
                int dividerBottom = dividerTop + divider.getIntrinsicHeight();

                divider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
                divider.draw(canvas);
            }
        }

    }

    private void transferItemToVault (final String itemInstanceId, final String itemReferenceHash) {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences;
                sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityMain.context.getApplicationContext());
                String membershipType = sharedPreferences.getString("membership_type", null);
                String[] params = new String[6];
                params[0] = membershipType;
                params[1] = characterId;
                params[2] = itemInstanceId;
                params[3] = itemReferenceHash;
                params[4] = "1"; //stack size -- always 1? IDK what this means
                params[5] = "true";

                DestinyAPI destinyAPI = new DestinyAPI();
                JSONObject response = destinyAPI.transferToVault(params);

                try {
                    if (response.getInt("ErrorCode") == 1) {
                        displayList.remove(selectedItem);

                        Message message = new Message();
                        message.what = TRANSFERRED_TO_VAULT;
                        handler.sendMessage(message);
                    } else {
                        Log.d("Transfer Error", response.toString());
                    }
                } catch (Exception e) {
                    Log.d("Vault Transfer JSON", e.toString());
                }

                Log.d("Server Response", response.toString());


            }
        });
    }

    public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ItemsViewHolder> {

        JSONArray itemsRecyclerData;

        class ItemsViewHolder extends RecyclerView.ViewHolder {

            private ImageView imageView;
            private ImageView elementImageView;
            private ImageView inventoryItemBackground;
            private ImageView swapImageView;
            private ImageView ammoIcon;
            private ImageView border;
            private TextView powerTextView;
            private TextView titleTextView;
            private TextView typeTextView;

            private ItemsViewHolder(final View view) {
                super(view);
                imageView = view.findViewById(R.id.inventory_item_image);
                elementImageView = view.findViewById(R.id.element_icon);
                inventoryItemBackground = view.findViewById(R.id.inventory_item_background);
                swapImageView = view.findViewById(R.id.swap_image);
                ammoIcon = view.findViewById(R.id.ammunition_type_icon);
                swapImageView.setVisibility(View.GONE);
                border = view.findViewById(R.id.gear_tint);
                final Drawable drawable = border.getBackground();

                titleTextView = view.findViewById(R.id.equipped_text);
                powerTextView = view.findViewById(R.id.item_power);
                typeTextView = view.findViewById(R.id.type);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (selectedItem > -1) {
                            View oldView = itemsLayoutManager.findViewByPosition(selectedItem);
                            ImageView oldBorderView = oldView.findViewById(R.id.gear_tint);
                            Drawable oldDrawable = oldBorderView.getBackground();
                            oldDrawable.setColorFilter(null);
                        }
                        drawable.setColorFilter(Color.parseColor("#d04286f4"), PorterDuff.Mode.SRC_IN);
                        selectedItem = getAdapterPosition();
                    }
                });

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (selectedItem > -1) {
                            View oldView = itemsLayoutManager.findViewByPosition(selectedItem);
                            ImageView oldBorderView = oldView.findViewById(R.id.gear_tint);
                            Drawable oldDrawable = oldBorderView.getBackground();
                            oldDrawable.setColorFilter(null);
                        }
                        drawable.setColorFilter(Color.parseColor("#d04286f4"), PorterDuff.Mode.SRC_IN);
                        selectedItem = getAdapterPosition();
                        try {
                            String itemInstanceId = displayList.getJSONObject(getAdapterPosition()).getString("itemInstanceId");
                            JSONObject characterInventories = ActivityMain.context.character.getCharacterInventories();
                            JSONArray inventoryArray = characterInventories.getJSONObject("data").getJSONObject(characterId).getJSONArray("items");
                            for (int i = 0; i < inventoryArray.length(); i++) {
                                String indexInstanceId = inventoryArray.getJSONObject(i).getString("itemInstanceId");
                                if (indexInstanceId.equals(itemInstanceId)) {
                                    String instanceString = String.valueOf(inventoryArray.getJSONObject(i));
                                    Intent intent = new Intent(ActivityCharacter.context, ActivityItem.class);
                                    intent.putExtra("instanceString", instanceString);
                                    ActivityCharacter.context.startActivity(intent);
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            Log.d("Item Select", e.toString());
                        }
                        return false;
                    }
                });
            }
        }

        ItemsAdapter(JSONArray dataArray) {
            try {
                itemsRecyclerData = dataArray;
            } catch (Exception e) {
                Log.d("Weapons Recycler", e.toString());
            }
        }

        @Override
        @NonNull
        public ItemsAdapter.ItemsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.character_equipped_layout, viewGroup, false);
            return new ItemsAdapter.ItemsViewHolder(view);

        }

        @Override
        public void onBindViewHolder(@NonNull ItemsAdapter.ItemsViewHolder viewHolder, final int position) {
            try {
                JSONObject item = itemsRecyclerData.getJSONObject(position);
                final String itemHash = item.getString("itemHash");
                final String itemInstanceId = item.getString("itemInstanceId");
                String icon = item.getString("icon");
                String name = item.getString("name");
                String elementIcon = item.optString("elementIcon", null);
                String itemPower = item.optString("itemPower");
                int state = item.optInt("state");
                int ammoType = item.optInt("ammoType");
                String type = item.getString("type");

                Bitmap defaultBitmap = BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.missing_icon_d2);
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

            } catch (Exception e) {
                Log.d("items Adapter", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return itemsRecyclerData.length();
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
            //Log.d("Icon", itemUrl);
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
                Log.d("Load Inventory Images", e.getMessage());
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
