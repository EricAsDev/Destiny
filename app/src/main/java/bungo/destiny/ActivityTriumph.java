package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class ActivityTriumph extends AppCompatActivity {

    public static ActivityTriumph context;

    String triumphHashId;
    JSONArray triumphChildren = new JSONArray();
    JSONObject triumphDefinition;

    TriumphPagerAdapter triumphPagerAdapter;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_triumph);
        context = this;

        Intent receivedIntent = getIntent();
        triumphHashId = receivedIntent.getStringExtra("hashId");

        final TextView nameTextView = findViewById(R.id.triumph_title);
        final TabLayout triumphTabs = findViewById(R.id.triumph_tabLayout);
        ViewPager triumphViewPager = findViewById(R.id.triumph_viewPager);

        triumphPagerAdapter = new TriumphPagerAdapter(triumphChildren);
        triumphViewPager.setAdapter(triumphPagerAdapter);
        triumphPagerAdapter.notifyDataSetChanged();

        triumphTabs.setupWithViewPager(triumphViewPager);
        try {
            String signedHash = ActivityMain.context.getSignedHash(triumphHashId);
            triumphDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyPresentationNodeDefinition"));

            JSONArray jsonArray = triumphDefinition.getJSONObject("children").getJSONArray("presentationNodes");
            String triumphName = triumphDefinition.getJSONObject("displayProperties").getString("name");
            nameTextView.setText(triumphName);

            for (int i = 0; i < jsonArray.length(); i++) {
                String hashId = jsonArray.getJSONObject(i).getString("presentationNodeHash");
                String signedNodeHash = ActivityMain.context.getSignedHash(hashId);
                JSONObject nodeDefinition = new JSONObject(ActivityMain.context.defineElement(signedNodeHash, "DestinyPresentationNodeDefinition"));
                triumphChildren.put(nodeDefinition);
                triumphPagerAdapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            Log.d("Triumph ViewPager", e.toString());
        }
        for (int i = 0; i < triumphTabs.getTabCount(); i++) {
            TabLayout.Tab tab = triumphTabs.getTabAt(i);
            try {
                String tabIconUrl = triumphChildren.getJSONObject(i).getJSONObject("displayProperties").getString("icon");
                new LoadImages(tab).execute(tabIconUrl);
            } catch (Exception e) {
                Log.d("Set Icon", e.toString());
            }
        }

    }

    class TriumphPagerAdapter extends PagerAdapter {
        private JSONArray pagerItems;

        private TriumphPagerAdapter(JSONArray items) {
            pagerItems = items;
        }

        @Override
        public @NonNull Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater layoutInflater = (LayoutInflater) ActivityTriumph.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup viewGroup = (ViewGroup) layoutInflater.inflate(R.layout.triumph_viewpager, null);

            RecyclerView recyclerView = viewGroup.findViewById(R.id.triumph_recycler);
            RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityTriumph.context);
            recyclerView.setLayoutManager(layOutManager);

            try {
                TriumphItemsAdapter triumphItemsAdapter = new TriumphItemsAdapter(context, pagerItems.getJSONObject(position).getJSONObject("children").getJSONArray("presentationNodes"));
                recyclerView.setAdapter(triumphItemsAdapter);
                triumphItemsAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.d("Inst PagerAdapter", e.toString());
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
        public CharSequence getPageTitle (int position) {
            String title = "";
            try {
                title = pagerItems.getJSONObject(position).getJSONObject("displayProperties").getString("name");
            } catch (Exception e) {
                Log.d("getViewpager Title", e.toString());
            }
            return title;
        }

        @Override
        public void destroyItem (@NonNull ViewGroup container, int position, @NonNull Object object) {
            (container).removeView((View) object);
        }

    }

    class TriumphItemsAdapter extends RecyclerView.Adapter<TriumphItemsAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray dataArray;
        ViewHolder viewHolder;

        TriumphItemsAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.dataArray = data;
        }

        @Override
        @NonNull public TriumphItemsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.triumph_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                viewHolder = holder;
                String hashId = dataArray.getJSONObject(position).getString("presentationNodeHash");
                String signedInt = ActivityMain.context.getSignedHash(hashId);
                JSONObject definition = new JSONObject(ActivityMain.context.defineElement(signedInt, "DestinyPresentationNodeDefinition"));
                String title = definition.getJSONObject("displayProperties").getString("name");
                holder.titleTextView.setText(title);
            } catch (Exception e){
                Log.d("onBind", e.toString());
            }
        }
        @Override
        public int getItemCount() {
            return dataArray.length();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            TextView titleTextView;

            ViewHolder(final View itemView) {
                super(itemView);
                itemView.setTag(this);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) itemView.getTag();
                        try {
                            String hashId = dataArray.getJSONObject(viewHolder.getAdapterPosition()).toString();
                            Log.d("Click!", hashId);
                            //String hashId = dataArray.getJSONObject(viewHolder.getAdapterPosition()).getString("presentationNodeHash");
                            Intent intent = new Intent(ActivityTriumph.context, ActivityRecords.class);
                            intent.putExtra("hashId", hashId);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.d("Triumph Click", e.toString());
                        }
                    }
                });
                titleTextView = itemView.findViewById(R.id.triumph_category_text);
            }
        }

        private int getItem(int position) {
            return position;
        }
    }

    static class LoadImages extends AsyncTask<String, Void, Bitmap> {

        private WeakReference<TabLayout.Tab> imageViewWeakReference;
        private LoadImages (TabLayout.Tab tabIcon){
            imageViewWeakReference = new WeakReference<>(tabIcon);
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
                Log.d("LoadRecordImages", e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewWeakReference != null) {
                TabLayout.Tab imageView = imageViewWeakReference.get();
                if(imageView != null) {
                    Drawable drawable = new BitmapDrawable(ActivityTriumph.context.getResources(), bitmap);
                    drawable.setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_ATOP);
                    imageView.setIcon(drawable);
                }
            }
        }
    }

}
