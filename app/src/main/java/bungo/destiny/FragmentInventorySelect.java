package bungo.destiny;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class FragmentInventorySelect extends Fragment {

    Data.Profile profile = ActivityMain.context.profile;
    JSONObject sortedIntoBuckets;
    InventoryAdapter inventoryAdapter;
    InventoryPagerAdapter inventoryPagerAdapter;
    InventoryAdapter.ViewHolder viewHolder;

    ViewPager inventoryViewPager;
    //NonSwipeableViewPager inventoryViewPager;
    TabLayout inventoryTabs;

    Handler handler;
    int UPDATE_INVENTORY_ADAPTER = 100;

    //Drawable consumablesIcon;
    //Drawable modificationsIcon;
    //Drawable shadersIcon;

    public FragmentInventorySelect() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                if (inputMessage.what == UPDATE_INVENTORY_ADAPTER) {

                    Drawable consumablesIcon = ActivityCharacter.context.getResources().getDrawable(R.drawable.icon_consumables, null);
                    Drawable modificationsIcon = ActivityCharacter.context.getResources().getDrawable(R.drawable.icon_modifications, null);
                    Drawable shadersIcon = ActivityCharacter.context.getResources().getDrawable(R.drawable.icon_shaders, null);

                    inventoryViewPager.setAdapter(inventoryPagerAdapter);
                    inventoryAdapter.notifyDataSetChanged();
                    inventoryPagerAdapter.notifyDataSetChanged();

                    inventoryTabs.setupWithViewPager(inventoryViewPager);
                    inventoryTabs.getTabAt(0).setIcon(consumablesIcon);
                    inventoryTabs.getTabAt(1).setIcon(modificationsIcon);
                    inventoryTabs.getTabAt(2).setIcon(shadersIcon);

                    for (int i = 0; i < inventoryTabs.getTabCount(); i++){
                        TabLayout.Tab tab = inventoryTabs.getTabAt(i);
                        if (tab != null) {
                            tab.setCustomView(R.layout.tab_icon_layout);
                            tab.getIcon().setColorFilter(Color.parseColor("#D0777777"), PorterDuff.Mode.SRC_IN);
                        };
                    }
                }
            }
        };
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        buildInventory();
        return inflater.inflate(R.layout.fragment_inventory_select, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);

        inventoryViewPager = view.findViewById(R.id.select_inventory_viewpager);
        inventoryTabs = view.findViewById(R.id.select_inventory_tabs);

        inventoryPagerAdapter = new InventoryPagerAdapter(ActivityCharacter.context, sortedIntoBuckets);
    }

    private void buildInventory () {
        sortedIntoBuckets = new JSONObject();

        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject inventoryItemsObject = profile.getProfileInventory();
                    int totalInventoryCount = inventoryItemsObject.getJSONObject("data").getJSONArray("items").length();
                    for (int i = 0; i < totalInventoryCount; i++) {
                        JSONObject currentItem = inventoryItemsObject.getJSONObject("data").getJSONArray("items").getJSONObject(i);
                        String bucketHash = currentItem.getString("bucketHash");

                        if (!sortedIntoBuckets.has(bucketHash)) {
                            String convertedBucketHash = ActivityMain.context.getSignedHash(bucketHash);
                            JSONObject bucketDefinition = new JSONObject(ActivityMain.context.defineElement(convertedBucketHash, "DestinyInventoryBucketDefinition"));
                            if (bucketDefinition.getBoolean("enabled") && bucketDefinition.getString("location").equals("0")){
                                JSONObject newItemObject = new JSONObject();
                                newItemObject.put("definition", bucketDefinition);
                                newItemObject.put("items", new JSONArray().put(currentItem));
                                sortedIntoBuckets.put(bucketHash, newItemObject);
                            }
                        } else {
                            sortedIntoBuckets.getJSONObject(bucketHash).getJSONArray("items").put(currentItem);
                        }
                    }
                    Message message = new Message();
                    message.what = UPDATE_INVENTORY_ADAPTER;
                    handler.sendMessage(message);
                } catch (Exception e) {
                    Log.d("Inventory Load Error", e.toString());
                }

            }
        });
    }

    class InventoryPagerAdapter extends PagerAdapter{

        private Context mContext;
        private JSONObject pagerItems;

        private InventoryPagerAdapter(Context context, JSONObject items) {
            mContext = context;
            pagerItems = items;
        }

        @Override
        @NonNull public Object instantiateItem(@NonNull ViewGroup collection, int position) {

            //LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup viewGroup = (ViewGroup) collection.inflate(mContext, R.layout.inventory_select_pager, null);

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
                inventoryAdapter.notifyDataSetChanged();

                collection.addView(viewGroup);

            } catch (Exception e) {
                Log.d("Inventory Adapter", e.toString());
            }
            return viewGroup;
        }

        @Override
        public int getCount() {
            return pagerItems.names().length();
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
                Log.d("onBind", e.toString());
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

        //private int getItem(int position) {
        //    return position;
        //}
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
/*
    private void inventoryImage (String itemHash) {

        String signedHash = ActivityMain.context.getSignedHash(itemHash);
        try {
            JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
            String iconUrl = itemDefinition.getJSONObject("displayProperties").getString("icon");

            if (!iconUrl.equals("")) {
                iconUrl = iconUrl.replaceAll("'\'/", "/");

                String iconPath = iconUrl.substring(iconUrl.lastIndexOf("/") + 1);
                File image = new File(ActivityMain.context.getDir("Files", Context.MODE_PRIVATE), iconPath);
                Bitmap icon;
                if (image.exists()) {
                    icon = BitmapFactory.decodeFile(image.getAbsolutePath());
                    //Log.d("Image", "Found in local storage");
                } else {
                    try {
                        InputStream in = new URL("https://www.bungie.net" + iconUrl).openStream();
                        icon = BitmapFactory.decodeStream(in);
                        in.close();
                    } catch (Exception e) {
                        Log.e("Error", e.getMessage());
                        e.printStackTrace();
                    }
                    Message message = new Message();
                    message.what = UPDATE_INVENTORY_ADAPTER;
                    handler.sendMessage(message);
                }
            }
        } catch (Exception e) {
            Log.d("Inventory Item", e.toString());
        }
    }
*/
}
