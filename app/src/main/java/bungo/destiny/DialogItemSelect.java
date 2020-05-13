package bungo.destiny;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DialogItemSelect extends Dialog implements View.OnClickListener {

    private int selectedItem = -1;
    private int INVENTORY_BUILT = 100;
    private int FILTER_COMPLETE = 101;

    private String postmasterHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_postmaster);
    private String characterId;

    private List<String> itemCategories;

    private Bitmap icon_primary;
    private Bitmap icon_special;
    private Bitmap icon_heavy;
    private Bitmap icon_default;

    private ReturnSelection returnSelection;

    private RecyclerView itemsRecycler;
    RecyclerView.LayoutManager itemsLayoutManager;
    private ItemsAdapter itemsAdapter;

    private List<InventoryObjects> displayList;
    private List<InventoryObjects> filteredDisplayList;

    private JSONObject getProfileInventory = ActivityMain.context.profile.getProfileInventory();
    private JSONObject getCharacterEquipment = ActivityMain.context.character.getCharacterEquipment();
    private JSONObject getCharacterInventory = ActivityMain.context.character.getCharacterInventories();
    private JSONObject itemDefinitions = new JSONObject();

    private JSONArray inventoryArray = new JSONArray();

    Handler handler;

    SearchView searchView;

    public DialogItemSelect(Activity context, List<String> data) {
        super(context);
        itemCategories = data;
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
        buttonVault.setVisibility(View.GONE);

        buttonEquip.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);
        buttonVault.setOnClickListener(this);

        icon_primary = BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.icon_ammo_primary);
        icon_special = BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.icon_ammo_special);
        icon_heavy = BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.icon_ammo_heavy);
        icon_default = BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.missing_icon_d2);

        itemsRecycler = findViewById(R.id.picker_recycler);
        itemsLayoutManager = new LinearLayoutManager(ActivityCharacter.context, RecyclerView.VERTICAL, false);
        itemsRecycler.setLayoutManager(itemsLayoutManager);
        ItemsDecoration itemsDecoration = new ItemsDecoration(getContext(), Color.RED, 50);
        itemsRecycler.addItemDecoration(itemsDecoration);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 100:
                        Log.d("Build Complete", displayList.size() + " items");
                        //preFilterData();
                        break;
                    case 101:
                        itemsAdapter = new ItemsAdapter(displayList);
                        itemsRecycler.setAdapter(itemsAdapter);
                        itemsAdapter.notifyDataSetChanged();
                        ProgressBar progressBar = findViewById(R.id.select_progress);
                        progressBar.setVisibility(View.GONE);
                        break;
                }
            }
        };

        SearchManager searchManager = (SearchManager) ActivityMain.context.getSystemService(Context.SEARCH_SERVICE);
        searchView = findViewById(R.id.picker_search);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(ActivityMain.context.getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                itemsAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                itemsAdapter.getFilter().filter(newText);
                return false;
            }
        });
        searchView.clearFocus();
        buildInventoryList();
    }

    private void buildInventoryList () {
        displayList = new ArrayList<>();
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d("Profile Inventory", getProfileInventory.toString());
                Log.d("Character Inventory", getCharacterInventory.toString());
                Log.d("Character Equipment", getCharacterEquipment.toString());

                try {
                    JSONArray characterEquipment;
                    JSONArray characterInventory;
                    JSONArray workingList = new JSONArray();
                    JSONArray profileInventory = getProfileInventory.getJSONObject("data").getJSONArray("items");
                    JSONArray characterIds = ActivityMain.context.profile.getProfile().getJSONObject("data").getJSONArray("characterIds");
                    for (int i = 0; i < characterIds.length(); i++) {
                        characterEquipment = getCharacterEquipment.getJSONObject("data").getJSONObject(characterIds.getString(i)).getJSONArray("items");
                        //Log.d(characterIds.getString(i), characterEquipment.toString());
                        characterInventory = getCharacterInventory.getJSONObject("data").getJSONObject(characterIds.getString(i)).getJSONArray("items");
                        workingList.put(characterEquipment);
                        workingList.put(characterInventory);
                    }
                    workingList.put(profileInventory);

                    //JSONArray workingList = ActivityCharacter.context.aggregateEquipment;
                    for (int j = 0; j < workingList.length(); j++) {
                        JSONArray currentArray = workingList.getJSONArray(j);
                        for (int k = 0; k < currentArray.length(); k++) {
                            int location = currentArray.getJSONObject(k).getInt("location");
                            String itemHash = currentArray.getJSONObject(k).getString("itemHash");
                            String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                            JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition"));

                            //get the things in the correct scope
                            JSONArray itemCategoryHashes = itemDefinition.getJSONArray("itemCategoryHashes");
                            List<String> categories = new ArrayList<>();
                            for (int l = 0; l < itemCategoryHashes.length(); l++) {
                                categories.add(itemCategoryHashes.getString(l));
                            }
                            if (!categories.containsAll(itemCategories)) continue;

                            Boolean equippable = itemDefinition.getBoolean("equippable");
                            JSONObject displayProperties = itemDefinition.getJSONObject("displayProperties");
                            String name = displayProperties.getString("name");
                            String description = displayProperties.getString("description");
                            boolean hasIcon = displayProperties.getBoolean("hasIcon");
                            String icon = null;
                            if (hasIcon) icon = displayProperties.getString("icon");
                            String itemTypeDisplayName = itemDefinition.getString("itemTypeDisplayName");
                            String elementIcon = null;
                            if (itemDefinition.has("damageTypeHash")) {
                                String damageTypeHash = itemDefinition.getString("damageTypeHash");
                                String signedDamageTypeHash = ActivityMain.context.getSignedHash(damageTypeHash);
                                JSONObject damageTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedDamageTypeHash, "DestinyDamageTypeDefinition"));
                                elementIcon = damageTypeDefinition.getJSONObject("displayProperties").getString("icon");
                            }

                            String itemInstanceId = null;
                            if (currentArray.getJSONObject(k).has("itemInstanceId")) {
                                itemInstanceId = currentArray.getJSONObject(k).getString("itemInstanceId");
                            }

                        /* location enum
                        Unknown: 0
                        Inventory: 1
                        Vault: 2
                        Vendor: 3
                        Postmaster: 4
                         */
                        /*
                        if (itemInstanceId != null) {
                            if (location == 1 && instanceData.has(itemInstanceId)) {
                                //Log.d("character", instanceData.getJSONObject(itemInstanceId).toString());
                            } else if (location == 2 & profileInstanceData.has(itemInstanceId)) {
                                //Log.d("profile", profileInstanceData.getJSONObject(itemInstanceId).toString());
                            } else if (location == 3) {
                                Log.d("Vendor?", itemHash);
                            } else if (location == 4) {
                                Log.d("Postmaster", inventoryArray.getJSONObject(i).toString());
                            } else {
                                Log.d("Other", itemInstanceId);
                            }

                        }
                        */

                        //JSONObject jsonObject = new JSONObject();

                        //jsonObject.put("name", name);
                        //jsonObject.put("itemHash", itemHash);
                        //jsonObject.put("itemInstanceId", itemInstanceId);
                        //jsonObject.put("icon", icon);
                        //jsonObject.put("elementIcon", elementIcon);
                        //jsonObject.put("description", description);
                        //jsonObject.put("itemTypeDisplayName", itemTypeDisplayName);
                        //jsonObject.put("itemCategoryHashes", itemCategoryHashes);
                        //jsonObject.put("equippable", equippable);
                            //inventoryArray.put(jsonObject);

                        InventoryObjects inventoryObjects = new InventoryObjects();
                        inventoryObjects.setName(name);
                        inventoryObjects.setItemHash(itemHash);
                        inventoryObjects.setItemInstanceId(itemInstanceId);

                        displayList.add(inventoryObjects);

                        }
                    }

                    //displayList = new Gson().fromJson(inventoryArray.toString(), new TypeToken<List<InventoryObjects>>() {}.getType());

                    Message message = new Message();
                    //message.what = INVENTORY_BUILT;
                    message.what = FILTER_COMPLETE;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class InventoryObjects {
        String itemHash;
        String itemInstanceId;
        String bucketHash;
        String name;
        String itemTypeDisplayName;
        String icon;

        int quantity;
        int bindStatus;
        int location;
        int transferStatus;
        int state;
        int dismantlePermission;

        boolean lockable;
        boolean isWrapper;
        boolean equippable;

        public void setName(String name) {
            this.name = name;
        }

        public void setItemInstanceId(String itemInstanceId) {
            this.itemInstanceId = itemInstanceId;
        }

        public void setItemHash(String itemHash) {
            this.itemHash = itemHash;
        }

        public String getIcon() {
            return icon;
        }

        public String getItemTypeDisplayName() {
            return itemTypeDisplayName;
        }

        public String getName() {
            return name;
        }

        public String getItemInstanceId() {
            return itemInstanceId;
        }

        public int getState() {
            return state;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getBucketHash() {
            return bucketHash;
        }

        public String getItemHash() {
            return itemHash;
        }

        public int getLocation() {
            return location;
        }

        public boolean isEquippable() {
            return equippable;
        }


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.picker_button_equip:
                Log.d("Button", "Equip");
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
                break;
        }
    }

    private JSONObject getReturnData (int selectedItem) {
        JSONObject returnData = new JSONObject();
        try {
            InventoryObjects selectedObject = displayList.get(selectedItem);
            String itemInstanceId = selectedObject.getItemInstanceId();
            String itemHash = selectedObject.getItemHash();
            Log.d("selected", selectedObject.getItemInstanceId());

        /*
            String itemHash = selectedObject.getString("itemHash");
            String icon = selectedObject.getString("icon");
            String name = selectedObject.getString("name");
            String elementIcon = selectedObject.optString("elementIcon");
            String itemPower = selectedObject.optString("itemPower");
            int state = selectedObject.optInt("state");
            int ammoType = selectedObject.optInt("ammoType");
            String type = selectedObject.getString("type");

            returnData.put("icon", icon);
            returnData.put("name", name);
            returnData.put("elementIcon", elementIcon);
            returnData.put("itemPower", itemPower);
            returnData.put("state", state);
            returnData.put("ammoType", ammoType);
            returnData.put("type", type);
*/
            returnData.put("itemInstanceId", itemInstanceId);
            returnData.put("itemHash", itemHash);
        } catch (Exception e) {
            Log.d("getReturnData", e.toString());
        }

        return returnData;
    }

    public interface ReturnSelection {
        void selectObject(JSONObject itemData);
    }

    public void setReturnSelection (ReturnSelection returnSelection) {
        this.returnSelection = returnSelection;
    }

    public class ItemsDecoration extends RecyclerView.ItemDecoration {
        private Paint paint;
        private TextPaint paintText;
        private Context context;
        private int dividerHeight;
        private int layoutOrientation;

        public ItemsDecoration (Context mContext, int color, int mDividerHeight) {
            paint = new Paint();
            paint.setColor(color);
            paint.setStrokeWidth(mDividerHeight);

            paintText = new TextPaint();
            paintText.setColor(color);
            paintText.setTextSize(16 * getContext().getResources().getDisplayMetrics().density);

            dividerHeight = mDividerHeight;
            context = mContext;
        }
        @Override
        public void getItemOffsets (Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.set(0,dividerHeight,0,dividerHeight);
            } else {
                outRect.set(0, 0, 0, dividerHeight);
            }
        }

        @Override
        public void onDraw (Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            super.onDraw(canvas, parent, state);
            verticalDivider(canvas, parent);
        }

        private void verticalDivider (Canvas canvas, RecyclerView parent) {
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();
            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                final int top = child.getBottom() + params.bottomMargin;
                //canvas.drawLine(left, top, right, top, paint);
                canvas.drawText("Shit", left, child.getTop() - params.topMargin, paintText);
            }
        }
    }

    public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ItemsViewHolder> implements Filterable {

        private List<InventoryObjects> inventoryList;
        private List<InventoryObjects> inventoryListFiltered;

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
                        Log.d(displayList.get(selectedItem).getItemHash(), displayList.get(selectedItem).getItemInstanceId());
                    }
                });

                view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (selectedItem > -1) {
                            Log.d("Selected Item", displayList.get(selectedItem).toString());
                            View oldView = itemsLayoutManager.findViewByPosition(selectedItem);
                            ImageView oldBorderView = oldView.findViewById(R.id.gear_tint);
                            Drawable oldDrawable = oldBorderView.getBackground();
                            oldDrawable.setColorFilter(null);
                        }
                        drawable.setColorFilter(Color.parseColor("#d04286f4"), PorterDuff.Mode.SRC_IN);
                        selectedItem = getAdapterPosition();
                        return false;
                    }
                });
            }
        }

        ItemsAdapter(List<InventoryObjects> inventoryList) {
            this.inventoryListFiltered = inventoryList;
            this.inventoryList = inventoryList;
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
                InventoryObjects item = inventoryListFiltered.get(position);
                int state = item.getState();
                String name = item.getName();
                String itemTypeDisplayName = item.getItemTypeDisplayName();
                String icon = item.getIcon();
                viewHolder.titleTextView.setText(name);

                if (icon!=null) {
                    new LoadImages(viewHolder.imageView).execute(icon);
                } else {
                    viewHolder.imageView.setImageBitmap(icon_default);
                }
            } catch (Exception e) {
                Log.d("items Adapter", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return inventoryListFiltered.size();
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

/*
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
*/