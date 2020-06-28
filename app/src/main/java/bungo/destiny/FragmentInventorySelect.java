package bungo.destiny;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.UnknownHostException;

public class FragmentInventorySelect extends Fragment {

    Data.Profile profile = ActivityMain.context.profile;
    JSONObject sortedIntoBuckets;
    JSONArray engrams;
    EngramsAdapter engramsAdapter;
    InventoryAdapter inventoryAdapter;
    InventoryPagerAdapter inventoryPagerAdapter;
    InventoryAdapter.ViewHolder viewHolder;
    RecyclerView.LayoutManager layoutManager;

    ViewPager inventoryViewPager;
    //TabLayout inventoryTabs;

    Handler handler;
    final int UPDATE_INVENTORY_ADAPTER = 100;
    final int UPDATE_ENGRAM_ADAPTER = 101;

    public FragmentInventorySelect() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case UPDATE_INVENTORY_ADAPTER:
                        inventoryPagerAdapter.notifyDataSetChanged();
                        break;
                    case UPDATE_ENGRAM_ADAPTER:
                        engramsAdapter.notifyDataSetChanged();
                        break;

                }
            }
        };
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory_select, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        RecyclerView engramsRecycler = view.findViewById(R.id.inventory_engrams);

        sortedIntoBuckets = new JSONObject();
        engrams = new JSONArray();

        inventoryViewPager = view.findViewById(R.id.select_inventory_viewpager);
        inventoryPagerAdapter = new InventoryPagerAdapter(ActivityCharacter.context, sortedIntoBuckets);
        inventoryViewPager.setAdapter(inventoryPagerAdapter);
        inventoryPagerAdapter.notifyDataSetChanged();

        engramsAdapter = new EngramsAdapter(engrams);
        layoutManager = new LinearLayoutManager(ActivityCharacter.context, LinearLayoutManager.HORIZONTAL, false);
        engramsRecycler.setLayoutManager(layoutManager);
        engramsRecycler.setAdapter(engramsAdapter);

        buildInventory();
        buildEngrams();
/*
        Drawable consumablesIcon = ActivityMain.context.getResources().getDrawable(R.drawable.icon_consumables, null);
        Drawable modificationsIcon = ActivityMain.context.getResources().getDrawable(R.drawable.icon_modifications, null);
        Drawable shadersIcon = ActivityMain.context.getResources().getDrawable(R.drawable.icon_shaders, null);

        inventoryTabs = view.findViewById(R.id.select_inventory_tabs);
        inventoryTabs.setupWithViewPager(inventoryViewPager);

        Objects.requireNonNull(inventoryTabs.getTabAt(0)).setIcon(consumablesIcon);
        Objects.requireNonNull(inventoryTabs.getTabAt(1)).setIcon(modificationsIcon);
        Objects.requireNonNull(inventoryTabs.getTabAt(2)).setIcon(shadersIcon);

        for (int i = 0; i < inventoryTabs.getTabCount(); i++){
            TabLayout.Tab tab = inventoryTabs.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(R.layout.tab_icon_layout);
                Objects.requireNonNull(tab.getIcon()).setColorFilter(Color.parseColor("#D0777777"), PorterDuff.Mode.SRC_IN);
            }
        }

 */

    }

    public void buildEngrams () {
        final String engramBucketHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_engrams);
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject characterInventory = ActivityMain.context.character
                            .getCharacterInventories()
                            .getJSONObject("data")
                            .getJSONObject(ActivityCharacter.context.characterId);
                    //Log.d("Character Shit", characterInventory.getJSONObject("data").getJSONObject("2305843009267660573").toString());
                    //Log.d("Item", currentItem.toString());

                    for (int i = 0; i < characterInventory.getJSONArray("items").length(); i++) {
                        JSONObject item = characterInventory.getJSONArray("items").getJSONObject(i);
                        String bucketHash = item.getString("bucketHash");
                        if (bucketHash.equals(engramBucketHash)) {
                            Log.d("Engram", item.toString());
                            engrams.put(item);
                            Message message = new Message();
                            message.what = UPDATE_ENGRAM_ADAPTER;
                            handler.sendMessage(message);
                        }
                    }

                } catch (Exception e) {
                    Log.d("Build Engrams", Log.getStackTraceString(e));
                }
            }
        });
    }

    public void buildInventory () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject inventoryItemsObject = profile.getProfileInventory();
                    int totalInventoryCount = inventoryItemsObject.getJSONObject("data").getJSONArray("items").length();

                    for (int i = 0; i < totalInventoryCount; i++) {
                        JSONObject currentItem = inventoryItemsObject.getJSONObject("data").getJSONArray("items").getJSONObject(i);
                        String bucketHash = currentItem.getString("bucketHash");
/*
                        String signedHash = ActivityMain.context.getSignedHash(
                                currentItem.getString("itemHash")
                        );
                        JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(
                                signedHash,
                                "DestinyInventoryItemDefinition"
                        ));
                        //Log.d("Item Type", itemDefinition.getString("itemType"));
*/
                        if (!sortedIntoBuckets.has(bucketHash)) {
                            String convertedBucketHash = ActivityMain.context.getSignedHash(bucketHash);
                            JSONObject bucketDefinition = new JSONObject(ActivityMain.context.defineElement(convertedBucketHash, "DestinyInventoryBucketDefinition"));

                            if (bucketDefinition.getBoolean("enabled") && bucketDefinition.getString("location").equals("0")){
                                JSONObject newItemObject = new JSONObject();
                                newItemObject.put("definition", bucketDefinition);
                                newItemObject.put("items", new JSONArray().put(currentItem));
                                sortedIntoBuckets.put(bucketHash, newItemObject);
                                inventoryPagerAdapter.notifyDataSetChanged();
                            }
                        } else {
                            sortedIntoBuckets.getJSONObject(bucketHash).getJSONArray("items").put(currentItem);
                        }
                    }

                    Message message = new Message();
                    message.what = UPDATE_INVENTORY_ADAPTER;
                    handler.sendMessage(message);

                } catch (Exception e) {
                    Log.d("Inventory Load Error", Log.getStackTraceString(e));
                }
            }
        });
    }

    class InventoryPagerAdapter extends PagerAdapter{

        Context mContext;
        JSONObject pagerItems;

        InventoryPagerAdapter(Context context, JSONObject items) {
            this.mContext = context;
            this.pagerItems = items;

        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            ViewGroup viewGroup = (ViewGroup) View.inflate(mContext, R.layout.inventory_select_pager, null);

            TextView groupTitle = viewGroup.findViewById(R.id.select_inventory_pager_text);
            TextView groupCount = viewGroup.findViewById(R.id.select_inventory_pager_count);
            try {
                String title = sortedIntoBuckets
                        .getJSONObject(pagerItems.names().getString(position))
                        .getJSONObject("definition")
                        .getJSONObject("displayProperties")
                        .getString("name");
                groupTitle.setText(title);

                String maxCount = sortedIntoBuckets
                        .getJSONObject(pagerItems.names().getString(position))
                        .getJSONObject("definition")
                        .getString("itemCount");

                String count = String.valueOf(sortedIntoBuckets
                        .getJSONObject(pagerItems.names().getString(position))
                        .getJSONArray("items")
                        .length());
                String countText = count + "/" + maxCount;
                groupCount.setText(countText);

                RecyclerView recyclerView = viewGroup.findViewById(R.id.select_inventory_pager_recycler);
                recyclerView.setLayoutManager(new GridLayoutManager(mContext, 4));
                inventoryAdapter = new InventoryAdapter(mContext,
                        sortedIntoBuckets.getJSONObject(pagerItems.names().getString(position)).getJSONArray("items"));
                recyclerView.setAdapter(inventoryAdapter);

                collection.addView(viewGroup);

            } catch (Exception e) {
                Log.d("Inventory Adapter", Log.getStackTraceString(e));
            }
            return viewGroup;
        }

        @Override
        public int getCount() {
            if (pagerItems.names() != null) {
                return pagerItems.names().length();
            } else {
                return 0;
            }
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position,@NonNull Object view) {
            collection.removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray dataArray;

        InventoryAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.dataArray = data;
        }

        @Override
        @NonNull public InventoryAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.inventory_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                viewHolder = holder;
                String quantity = dataArray.getJSONObject(position).getString("quantity");
                final String itemHash = dataArray.getJSONObject(position).getString("itemHash");
                holder.itemTextView.setText(quantity);
                new LoadInventoryImages(holder.itemImageView).execute(itemHash);
            } catch (Exception e){
                Log.d("onBind", Log.getStackTraceString(e));
            }
        }
        @Override
        public int getItemCount() {
            return dataArray.length();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            TextView itemTextView;
            ImageView itemElementImageView;
            ImageView itemImageView;

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
                itemTextView = itemView.findViewById(R.id.inventory_item_number);
                itemElementImageView = itemView.findViewById(R.id.inventory_item_element);
                itemImageView = itemView.findViewById(R.id.inventory_item_image);
            }
        }
    }

    static class EngramsAdapter extends RecyclerView.Adapter<EngramsAdapter.ViewHolder> {
        private JSONArray dataArray;

        EngramsAdapter(JSONArray data){
            this.dataArray = data;
        }

        @Override
        @NonNull public EngramsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            View view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.inventory_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                String itemHash = dataArray.getJSONObject(position).getString("itemHash");
                String itemInstanceId = dataArray.getJSONObject(position).getString("itemInstanceId");
                JSONObject itemInstance = ActivityMain.context.character.getItemInstances().getJSONObject(itemInstanceId);
                //Log.d("Instance", itemInstance.toString());
                String engramQuality = itemInstance.getString("itemLevel") +
                        itemInstance.getString("quality");
                holder.itemTextView.setText(engramQuality);

                String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                JSONObject itemDefinition = new JSONObject(ActivityMain.context
                        .defineElement(signedItemHash, "DestinyInventoryItemDefinition"));
                String iconUrl = itemDefinition.getJSONObject("displayProperties").getString("icon");
                new LoadImages(holder.itemImageView).execute(iconUrl);
                holder.itemImageView.setBackground(null);
                holder.itemBackground.setVisibility(View.INVISIBLE);
            } catch (Exception e){
                Log.d("onBind", Log.getStackTraceString(e));
            }
        }
        @Override
        public int getItemCount() {
            return dataArray.length();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {

            TextView itemTextView;
            ImageView itemElementImageView;
            ImageView itemImageView;
            ImageView itemBackground;

            ViewHolder(final View itemView) {
                super(itemView);
                itemView.setTag(this);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) itemView.getTag();
                        Toast.makeText(ActivityCharacter.context, "Click! " + viewHolder.getAdapterPosition(), Toast.LENGTH_SHORT).show();
                    }
                });
                itemTextView = itemView.findViewById(R.id.inventory_item_number);
                itemElementImageView = itemView.findViewById(R.id.inventory_item_element);
                itemImageView = itemView.findViewById(R.id.inventory_item_image);
                itemBackground = itemView.findViewById(R.id.inventory_item_background);
            }
        }
    }



    static class LoadInventoryImages extends AsyncTask<String, Void, Bitmap> {

        private WeakReference<ImageView> imageViewWeakReference;
        private LoadInventoryImages (ImageView imageView){
            imageViewWeakReference = new WeakReference<>(imageView);
        }
        @Override
        protected Bitmap doInBackground (String... params) {
            String itemHash = params[0];
            String signedHash = ActivityMain.context.getSignedHash(itemHash);
            Bitmap icon;
            try {
                JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
                String iconUrl = itemDefinition.getJSONObject("displayProperties").getString("icon");
                iconUrl = iconUrl.replaceAll("''/", "/");

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
