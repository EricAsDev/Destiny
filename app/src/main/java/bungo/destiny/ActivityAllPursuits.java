package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.UnknownHostException;

public class ActivityAllPursuits extends AppCompatActivity {

    public static ActivityAllPursuits context;

    JSONArray traitIds = new JSONArray();
    JSONArray bounties = new JSONArray();
    JSONArray quests = new JSONArray();
    JSONArray featured = new JSONArray();

    int BOUNTY_ADDED = 1;
    int QUEST_ADDED = 2;
    int FEATURED_ADDED = 3;

    Handler handler;

    PursuitsPagerAdapter pursuitsPagerAdapter;

    ItemsAdapter bountiesAdapter;
    ItemsAdapter questsAdapter;
    ItemsAdapter featuredAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_pursuits);
        context = this;

        final TabLayout pursuitsTabs = findViewById(R.id.pursuits_tabLayout);
        ViewPager pursuitsViewPager = findViewById(R.id.pursuits_viewPager);

        bountiesAdapter = new ItemsAdapter(this, bounties);
        questsAdapter = new ItemsAdapter(this, quests);
        featuredAdapter = new ItemsAdapter(this, featured);

        try {
            String traitCategoryHash = ActivityMain.context.getResources().getString(R.string.fixed_trait_category_hash);
            JSONObject traitCategoryDefinition = new JSONObject(ActivityMain.context.defineElement(traitCategoryHash, "DestinyTraitCategoryDefinition"));
            traitIds = traitCategoryDefinition.getJSONArray("traitIds");

            pursuitsPagerAdapter = new PursuitsPagerAdapter(traitIds);
            pursuitsViewPager.setAdapter(pursuitsPagerAdapter);
            pursuitsViewPager.setOffscreenPageLimit(3);
            pursuitsPagerAdapter.notifyDataSetChanged();

            pursuitsTabs.setupWithViewPager(pursuitsViewPager);

        } catch (Exception e) {
            Log.d("Pursuits ViewPager", e.toString());
        }

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                int message = inputMessage.what;
                switch (message) {
                    case 1:
                        Log.d("Bounties", String.valueOf(bounties.length()));
                        bountiesAdapter.notifyDataSetChanged();
                        break;
                    case 2:
                        Log.d("Quests", String.valueOf(quests.length()));
                        questsAdapter.notifyDataSetChanged();
                        break;
                    case 3:
                        Log.d("Featured", String.valueOf(featured.length()));
                        featuredAdapter.notifyDataSetChanged();
                        break;
                }
            }
        };
        sortPursuits();
    }

    private void sortPursuits() {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String characterId = ActivityCharacter.context.characterId;
                    JSONObject characterInventories = ActivityMain.context.character.getCharacterInventories();
                    JSONArray characterInventoryArray = characterInventories
                            .getJSONObject("data")
                            .getJSONObject(characterId)
                            .getJSONArray("items");
                    for (int i = 0; i < characterInventoryArray.length(); i++) {
                        JSONObject inventoryItem = characterInventoryArray.getJSONObject(i);
                        String bucketHash = inventoryItem.getString("bucketHash");
                        if (bucketHash.equals(getResources().getString(R.string.fixed_hash_pursuits))) {
                            String itemHash = inventoryItem.getString("itemHash");
                            String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                            JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition"));
                            JSONArray traitIds = itemDefinition.getJSONArray("traitIds");
                            Message message = new Message();
                            if (traitIds.toString().contains("inventory_filtering.bounty")) {
                                bounties.put(inventoryItem);
                                message.what = BOUNTY_ADDED;
                                handler.sendMessage(message);
                            } else if (traitIds.toString().contains("inventory_filtering.quest.featured")) {
                                featured.put(inventoryItem);
                                message.what = FEATURED_ADDED;
                                handler.sendMessage(message);
                            } else if (traitIds.toString().contains("inventory_filtering.quest")) {
                                quests.put(inventoryItem);
                                message.what = QUEST_ADDED;
                                handler.sendMessage(message);
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.d("Pursuit Sort", Log.getStackTraceString(e));
                }
            }
        });
    }

    class PursuitsPagerAdapter extends PagerAdapter {
        private JSONArray pagerItems;

        private PursuitsPagerAdapter(JSONArray items) {
            pagerItems = items;
        }

        @Override
        public @NonNull
        Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater layoutInflater = (LayoutInflater) ActivityAllPursuits.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(R.layout.triumph_viewpager, null);

            try {

                if (position == 0) {
                    RecyclerView recyclerView = viewGroup.findViewById(R.id.triumph_recycler);
                    RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityAllPursuits.context);
                    recyclerView.setLayoutManager(layOutManager);
                    recyclerView.setAdapter(bountiesAdapter);
                    bountiesAdapter.notifyDataSetChanged();
                } else if (position == 1) {
                    RecyclerView recyclerView = viewGroup.findViewById(R.id.triumph_recycler);
                    RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityAllPursuits.context);
                    recyclerView.setLayoutManager(layOutManager);
                    recyclerView.setAdapter(questsAdapter);
                    questsAdapter.notifyDataSetChanged();
                } else {
                    RecyclerView recyclerView = viewGroup.findViewById(R.id.triumph_recycler);
                    RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityAllPursuits.context);
                    recyclerView.setLayoutManager(layOutManager);
                    recyclerView.setAdapter(featuredAdapter);
                    featuredAdapter.notifyDataSetChanged();
                }

            } catch (Exception e) {
                Log.d("Items PagerAdapter", e.toString());
            }
            collection.addView(viewGroup);
            return viewGroup;
        }

        @Override
        public int getCount() {
            return pagerItems.length();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title = "";
            try {
                title = ActivityMain.context.getString(ActivityMain.context.getResources().getIdentifier(pagerItems.getString(position), "string", ActivityMain.context.getPackageName()));
            } catch (Exception e) {
                Log.d("getViewpager Title", e.toString());
            }
            return title;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            (container).removeView((View) object);
        }

    }

    class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray dataArray;

        ItemsAdapter(Context context, JSONArray data) {
            this.layoutInflater = LayoutInflater.from(context);
            this.dataArray = data;
        }

        @Override
        @NonNull
        public ItemsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.quest_item_layout, parent, false);
            return new ItemsAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemsAdapter.ViewHolder holder, int position) {
            try {
                JSONObject inventoryItem = dataArray.getJSONObject(position);
                String itemHash = inventoryItem.getString("itemHash");
                String signedHash = ActivityMain.context.getSignedHash(itemHash);
                JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));

                boolean isExotic = false;
                holder.quantityTextView.setText(null);
                holder.progressBar.setVisibility(View.INVISIBLE);
                holder.iconImageView.setImageDrawable(getResources().getDrawable(R.drawable.missing_icon_d2, null));
                holder.gearTint.setBackground(getResources().getDrawable(R.drawable.customborder, ActivityCharacter.context.getTheme()));
                holder.completeImageView.setVisibility(View.INVISIBLE);

                int quantity = inventoryItem.getInt("quantity");
                int state = inventoryItem.getInt("state");
                if (itemDefinition.getJSONObject("inventory").getString("tierType").equals("6"))
                    isExotic = true;
                String name;
                if (itemDefinition.has("setData")) {
                    name = itemDefinition.getJSONObject("setData").getString("questLineName");
                } else {
                    name = itemDefinition.getJSONObject("displayProperties").getString("name");
                }
                String description = itemDefinition.getJSONObject("displayProperties").getString("description");
                String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                boolean complete = false;
                int progress = 0;
                int completionValue = 0;

                if (itemDefinition.getJSONObject("inventory").getBoolean("isInstanceItem")) {
                    String itemInstanceId = inventoryItem.getString("itemInstanceId");
                    JSONObject objectives = ActivityMain.context.character.getItemObjectives().getJSONObject(itemInstanceId);
                    if (objectives.getJSONArray("objectives").length() == 1) {
                        progress = objectives.getJSONArray("objectives").getJSONObject(0).getInt("progress");
                        completionValue = objectives.getJSONArray("objectives").getJSONObject(0).getInt("completionValue");
                        if (objectives.getJSONArray("objectives").getJSONObject(0).getBoolean("complete"))
                            complete = true;
                    } else if (objectives.getJSONArray("objectives").length() > 1) {
                        int counter = 0;
                        for (int j = 0; j < objectives.getJSONArray("objectives").length(); j++) {
                            if (objectives.getJSONArray("objectives").getJSONObject(j).getBoolean("complete")) {
                                counter++;
                            }
                        }
                        progress = counter;
                        completionValue = objectives.getJSONArray("objectives").length();
                        if (counter == objectives.getJSONArray("objectives").length())
                            complete = true;
                    }
                }

                if (isExotic) {
                    int[] colors = {Color.parseColor("#aaa9a9a9"), Color.parseColor("#d4af37")};
                    GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
                    gradientDrawable.setStroke(5, Color.parseColor("#d4af37"));
                    holder.gearTint.setBackground(gradientDrawable);
                } else if (state == 2) {
                    int[] colors = {Color.parseColor("#aaa9a9a9"), ActivityMain.context.getResources().getColor(R.color.tracked, ActivityMain.context.getTheme())};
                    GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
                    gradientDrawable.setStroke(10, ActivityMain.context.getResources().getColor(R.color.tracked, ActivityMain.context.getTheme()));
                    holder.gearTint.setBackground(gradientDrawable);
                    //todo add the tracked icon
                }
                if (quantity > 1) holder.quantityTextView.setText(String.valueOf(quantity));
                if (complete) holder.completeImageView.setVisibility(View.VISIBLE);
                holder.titleTextView.setText(name);
                holder.descriptionTextView.setText(description);
                holder.progressBar.setMax(completionValue);
                holder.progressBar.setProgress(progress);
                holder.progressBar.setVisibility(View.VISIBLE);

                new LoadImages(holder.iconImageView).execute(icon);
            } catch (Exception e) {
                Log.d("onBind", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return dataArray.length();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            TextView titleTextView;
            TextView descriptionTextView;
            TextView quantityTextView;
            ProgressBar progressBar;
            ImageView completeImageView;
            ImageView iconImageView;
            ImageView gearTint;

            Drawable background;

            ViewHolder(final View itemView) {
                super(itemView);

                titleTextView = itemView.findViewById(R.id.pursuit_title);
                descriptionTextView = itemView.findViewById(R.id.description);
                quantityTextView = itemView.findViewById(R.id.inventory_item_number);
                progressBar = itemView.findViewById(R.id.pursuit_progress);
                completeImageView = itemView.findViewById(R.id.completed);
                iconImageView = itemView.findViewById(R.id.inventory_item_image);
                gearTint = itemView.findViewById(R.id.gear_tint);
                background = gearTint.getBackground();

                itemView.setTag(this);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) itemView.getTag();
                        try {
                            Intent intent = new Intent(ActivityMain.context, ActivityQuestDetail.class);
                            intent.putExtra("item", dataArray.getJSONObject(viewHolder.getAdapterPosition()).toString());
                            intent.putExtra("characterId", ActivityCharacter.context.characterId);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.d("Click Error", e.toString());
                        }
                    }
                });
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
