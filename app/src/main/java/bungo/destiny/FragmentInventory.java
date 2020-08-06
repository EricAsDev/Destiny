package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class FragmentInventory extends Fragment {

    Data.Profile profile = ActivityMain.context.profile;
    JSONObject sortedIntoBuckets;
    JSONArray engrams;
    EngramsAdapter engramsAdapter;
    InventoryAdapter inventoryAdapter;
    InventoryPagerAdapter inventoryPagerAdapter;
    InventoryAdapter.ViewHolder viewHolder;
    RecyclerView.LayoutManager layoutManager;
    RecyclerView.LayoutManager pageSelectLayout;
    PageSelectAdapter pageSelectAdapter;
    ArrayList<Drawable> tabIcons;

    public static ViewPager inventoryViewPager;
    //TabLayout inventoryTabs;

    Handler handler;
    final int UPDATE_INVENTORY_ADAPTER = 100;
    final int UPDATE_ENGRAM_ADAPTER = 101;

    public FragmentInventory() {}

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
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        RecyclerView engramsRecycler = view.findViewById(R.id.inventory_engrams);
        RecyclerView pageSelect =  view.findViewById(R.id.page_select);

        sortedIntoBuckets = new JSONObject();
        engrams = new JSONArray();

        Drawable consumablesIcon = ActivityMain.context.getResources().getDrawable(R.drawable.icon_consumables, null);
        Drawable modificationsIcon = ActivityMain.context.getResources().getDrawable(R.drawable.icon_modifications, null);
        Drawable shadersIcon = ActivityMain.context.getResources().getDrawable(R.drawable.icon_shaders, null);

        tabIcons = new ArrayList<>();
        tabIcons.add(consumablesIcon);
        tabIcons.add(modificationsIcon);
        tabIcons.add(shadersIcon);

        pageSelectAdapter = new PageSelectAdapter(ActivityCharacter.context, tabIcons);
        pageSelectLayout = new LinearLayoutManager(ActivityCharacter.context, LinearLayoutManager.VERTICAL, false);
        pageSelect.setLayoutManager(pageSelectLayout);
        int spanCount = 1;
        int spacing = 20; //todo calculate spacing view width minus 3 x 22% screen width cubes divided by 2
        boolean includeEdge = false;
        pageSelect.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        pageSelect.setAdapter(pageSelectAdapter);

        inventoryViewPager = view.findViewById(R.id.select_inventory_viewpager);
        inventoryPagerAdapter = new InventoryPagerAdapter(ActivityCharacter.context, sortedIntoBuckets);
        inventoryViewPager.setAdapter(inventoryPagerAdapter);
        inventoryPagerAdapter.notifyDataSetChanged();

        engramsAdapter = new EngramsAdapter(engrams);
        layoutManager = new LinearLayoutManager(ActivityCharacter.context, LinearLayoutManager.HORIZONTAL, false);
        engramsRecycler.setLayoutManager(layoutManager);
        engramsRecycler.setAdapter(engramsAdapter);

        pageSelectAdapter.notifyDataSetChanged();

        //inventoryTabs = view.findViewById(R.id.select_inventory_tabs);
        //inventoryTabs.setupWithViewPager(inventoryViewPager);



        buildInventory();
        buildEngrams();



/*
        Objects.requireNonNull(inventoryTabs.getTabAt(0)).setIcon(consumablesIcon);
        Objects.requireNonNull(inventoryTabs.getTabAt(1)).setIcon(modificationsIcon);
        Objects.requireNonNull(inventoryTabs.getTabAt(2)).setIcon(shadersIcon);

 */
/*
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
                            //Log.d("Engram", item.toString());
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

                int spanCount = 3;

                RecyclerView recyclerView = viewGroup.findViewById(R.id.select_inventory_pager_recycler);
                recyclerView.setLayoutManager(new GridLayoutManager(mContext, spanCount));

                int spacing = 50; //todo calculate spacing view width minus 3 x 22% screen width cubes divided by 2
                boolean includeEdge = false;
                recyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));

                inventoryAdapter = new InventoryAdapter(mContext,
                        sortedIntoBuckets.getJSONObject(pagerItems.names().getString(position)).getJSONArray("items"));
                recyclerView.setAdapter(inventoryAdapter);

                collection.addView(viewGroup);
                //set tab icon
                //Objects.requireNonNull(inventoryTabs.getTabAt(position)).setIcon(tabIcons.get(position));

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
            final View view = layoutInflater.inflate(R.layout.thumbnail_layout, parent, false);
            Display display = ActivityCharacter.context.getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            view.getLayoutParams().height = (int) (width * .22);
            view.getLayoutParams().width = (int) (width * .22);
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

                //Log.d("item", dataArray.getJSONObject(position).toString());
                holder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(ActivityCharacter.context, ActivityItem.class);
                        intent.putExtra("itemHash", itemHash);
                        ActivityCharacter.context.startActivity(intent);
                    }
                });
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
            View view;

            ViewHolder(final View itemView) {
                super(itemView);
                view = itemView;
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
                    .inflate(R.layout.thumbnail_layout, parent, false);
            Display display = ActivityCharacter.context.getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            view.getLayoutParams().width = (int) (width*.22);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                final String itemHash = dataArray.getJSONObject(position).getString("itemHash");
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
                holder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(ActivityCharacter.context, ActivityItem.class);
                        intent.putExtra("itemHash", itemHash);
                        ActivityCharacter.context.startActivity(intent);
                    }
                });
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
            View view;

            ViewHolder(final View itemView) {
                super(itemView);
                view = itemView;
                itemTextView = itemView.findViewById(R.id.inventory_item_number);
                itemElementImageView = itemView.findViewById(R.id.inventory_item_element);
                itemImageView = itemView.findViewById(R.id.inventory_item_image);
                itemBackground = itemView.findViewById(R.id.inventory_item_background);
            }
        }
    }

    static class PageSelectAdapter extends RecyclerView.Adapter<PageSelectAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private ArrayList<Drawable> dataArray;

        PageSelectAdapter(Context context, ArrayList<Drawable> data){
            this.layoutInflater = LayoutInflater.from(context);
            this.dataArray = data;
        }

        @Override
        @NonNull public PageSelectAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.thumbnail_layout, parent, false);
            int width = parent.getWidth();
            view.getLayoutParams().height = width;
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                holder.itemImageView.setBackground(null);
                holder.itemImageView.setImageBitmap(((BitmapDrawable) dataArray.get(position)).getBitmap());
            } catch (Exception e){
                Log.d("onBind", Log.getStackTraceString(e));
            }
        }
        @Override
        public int getItemCount() {
            return dataArray.size();
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
                        inventoryViewPager.setCurrentItem(viewHolder.getAdapterPosition());
                        Log.d("Click!", String.valueOf(viewHolder.getAdapterPosition()));
                    }
                });
                itemTextView = itemView.findViewById(R.id.inventory_item_number);
                itemElementImageView = itemView.findViewById(R.id.inventory_item_element);
                itemImageView = itemView.findViewById(R.id.inventory_item_image);
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

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }
}
