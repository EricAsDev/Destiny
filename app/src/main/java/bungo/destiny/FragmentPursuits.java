package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FragmentPursuits extends Fragment {

    private String characterId;
    private Handler handler;

    private JSONArray trackedPursuits;
    private ItemAdapter pursuitsAdapter;

    public FragmentPursuits() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        characterId = ActivityCharacter.context.characterId;

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                pursuitsAdapter.notifyDataSetChanged();
            }
        };
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pursuits, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        RecyclerView trackedRecycler = view.findViewById(R.id.tracked_recycler);
        FloatingActionButton floatingActionButton = view.findViewById(R.id.AllPursuitsFAB);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(ActivityMain.context, ActivityAllPursuits.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.d("All pursuits click", e.toString());
                }
            }
        });

        /*
        View allPursuits = view.findViewById(R.id.all);
        allPursuits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(ActivityCharacter.context, "Open All Pursuits Activity", Toast.LENGTH_SHORT).show();
                try {
                    Intent intent = new Intent(ActivityMain.context, ActivityAllPursuits.class);
                    //intent.putExtra("characterId", characterId);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.d("All pursuits click", e.toString());
                }
            }
        });
*/
        trackedPursuits = new JSONArray();
        RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityCharacter.context);
        trackedRecycler.setLayoutManager(layOutManager);
        pursuitsAdapter = new ItemAdapter(ActivityCharacter.context, trackedPursuits);
        trackedRecycler.setAdapter(pursuitsAdapter);

        getTrackedPursuits();

    }

    private void getTrackedPursuits () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject characterInventories = ActivityMain.context.character.getCharacterInventories();
                    JSONArray characterInventoryArray = characterInventories
                            .getJSONObject("data")
                            .getJSONObject(characterId)
                            .getJSONArray("items");
                    for (int i = 0; i < characterInventoryArray.length(); i++) {
                        JSONObject inventoryItem = characterInventoryArray.getJSONObject(i);
                        String bucketHash = inventoryItem.getString("bucketHash");
                        if (bucketHash.equals(getResources().getString(R.string.fixed_hash_pursuits))) {
                            int state = inventoryItem.getInt("state");
                            if (state == 2) { //tracked pursuit
                                trackedPursuits.put(inventoryItem);
                            }
                        }
                        Message message = new Message();
                        handler.sendMessage(message);
                    }

                } catch (Exception e) {
                    Log.d("Pursuit Tracking Error", Log.getStackTraceString(e));
                }
            }
        });
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray dataArray;

        ItemAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.dataArray = data;
        }

        @Override
        @NonNull public ItemAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.quest_tracked_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            try {

                String itemHash = dataArray.getJSONObject(position).getString("itemHash");
                String signedHash = ActivityMain.context.getSignedHash(itemHash);
                JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
                if (itemDefinition.getJSONObject("inventory").getString("tierType").equals("6"))  {
                    int[] colors = {Color.parseColor("#aaa9a9a9"), Color.parseColor("#d4af37")};
                    GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
                    gradientDrawable.setStroke(5, Color.parseColor("#d4af37"));
                    holder.gearTint.setBackground(gradientDrawable);
                } else {
                    holder.gearTint.setBackground(getResources().getDrawable(R.drawable.custom_border));
                }

                ImageView iconImageView = holder.includeView.findViewById(R.id.inventory_item_image);

                //todo set completed icon

                String name = itemDefinition.getJSONObject("displayProperties").getString("name");
                String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                holder.titleTextView.setText(name);
                new LoadImages(iconImageView).execute(icon);

                if (itemDefinition.has("objectives")) {
                    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
                    ObjectivesAdapter objectivesAdapter = new ObjectivesAdapter(getContext(), dataArray.getJSONObject(position));
                    holder.objectivesRecycler.setAdapter(objectivesAdapter);
                    holder.objectivesRecycler.setLayoutManager(layoutManager);
                }
            } catch (Exception e){
                Log.d("onBind", e.toString());
            }

            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) holder.view.getTag();
                    try {
                        Intent intent = new Intent(ActivityMain.context, ActivityQuestDetail.class);
                        intent.putExtra("item", dataArray.getJSONObject(viewHolder.getAdapterPosition()).toString());
                        intent.putExtra("characterId", characterId);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.d("pursuit error", e.toString());
                    }
                }
            });
        }
        @Override
        public int getItemCount() {
            return dataArray.length();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            TextView titleTextView;
            RecyclerView objectivesRecycler;
            ImageView gearTint;
            View includeView;

            Drawable background;

            View view;

            ViewHolder(final View itemView) {
                super(itemView);

                titleTextView = itemView.findViewById(R.id.title);
                gearTint = itemView.findViewById(R.id.gear_tint);
                objectivesRecycler = itemView.findViewById(R.id.objectives);
                background = gearTint.getBackground();
                includeView = itemView.findViewById(R.id.include_layout);
                this.view = itemView;

                itemView.setTag(this);
                /*
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RecyclerView.ViewHolder viewHolder = (RecyclerView.ViewHolder) itemView.getTag();
                        try {
                            Intent intent = new Intent(ActivityMain.context, ActivityQuestDetail.class);
                            intent.putExtra("item", dataArray.getJSONObject(viewHolder.getAdapterPosition()).toString());
                            intent.putExtra("characterId", characterId);
                            startActivity(intent);
                        } catch (Exception e) {
                            Log.d("pursuit error", e.toString());
                        }
                    }
                });
                */
            }
        }

        private int getItem(int position) {
            return position;
        }
    }

    class ObjectivesAdapter extends RecyclerView.Adapter<ObjectivesAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private JSONArray data;
        private JSONObject itemDefinition;
        private JSONObject questItem;

        ObjectivesAdapter(Context context, JSONObject questItem) {
            this.layoutInflater = LayoutInflater.from(context);
            try {

                String itemHash = questItem.getString("itemHash");
                String signedHash = ActivityMain.context.getSignedHash(itemHash);
                JSONObject stepDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
                this.questItem = questItem;
                this.itemDefinition = stepDefinition;
                this.data = stepDefinition.getJSONObject("objectives").getJSONArray("objectiveHashes");
            } catch (Exception e) {
                Log.d("Objective Recycler", e.toString());
            }
        }

        @Override
        @NonNull
        public ObjectivesAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.quest_objective, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            try {
                //set defaults
                holder.completionProgress.setMax(1);
                holder.completionProgress.setProgress(0);
                holder.completeCheckBox.setChecked(false);
                holder.completionText.setText("");

                String objectiveHash = data.getString(position);
                String signedHash = ActivityMain.context.getSignedHash(objectiveHash);
                JSONObject objectiveDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyObjectiveDefinition"));
                String progressDescription = objectiveDefinition.getString("progressDescription");
                int progress = objectiveDefinition.getInt("completionValue");
                int max = objectiveDefinition.getInt("completionValue");
                int valueStyle = objectiveDefinition.getInt("completedValueStyle");

                String regex = "\\[(.*?)]";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(progressDescription);
                if (matcher.find()) {
                    String internalName = matcher.group();
                    internalName = internalName.replaceAll("[\\[\\]]", "");
                    String code = ActivityMain.context.getSymbol(internalName);
                    String symbol = ActivityMain.context.getResources()
                            .getString(ActivityMain.context.getResources().getIdentifier(code, "string", ActivityMain.context.getPackageName()));
                    progressDescription = progressDescription.replace(matcher.group(), symbol);
                }
                holder.objectiveText.setText(progressDescription);

            if (!itemDefinition.getJSONObject("inventory").getBoolean("isInstanceItem")) {
                JSONArray unInstancedItemObjective = ActivityMain.context.character
                        .getCharacterProgressions()
                        .getJSONObject("data")
                        .getJSONObject(characterId)
                        .getJSONObject("uninstancedItemObjectives")
                        .getJSONArray(itemDefinition.getString("hash"));
                for (int i = 0; i < unInstancedItemObjective.length(); i++) {
                    if (unInstancedItemObjective.getJSONObject(i).getString("objectiveHash").equals(objectiveHash)) {
                        holder.completeCheckBox.setChecked(unInstancedItemObjective.getJSONObject(i).getBoolean("complete"));
                        holder.completionProgress.setMax(unInstancedItemObjective.getJSONObject(i).getInt("completionValue"));
                        holder.completionProgress.setProgress(unInstancedItemObjective.getJSONObject(i).getInt("progress"));

                        String completionValueText = completionText(
                                valueStyle,
                                unInstancedItemObjective.getJSONObject(i).getInt("progress"),
                                max);
                        holder.completionText.setText(completionValueText);
                        break;
                    }
                }
            } else {
                String itemInstanceId = questItem.getString("itemInstanceId");
                JSONObject objectives = ActivityMain.context.character.getItemObjectives().optJSONObject(itemInstanceId);
                for (int i = 0; i < objectives.getJSONArray("objectives").length(); i++) {
                    if (objectives.getJSONArray("objectives").getJSONObject(i).getString("objectiveHash").equals(data.getString(position))) {
                        holder.completeCheckBox.setChecked(objectives.getJSONArray("objectives").getJSONObject(i).getBoolean("complete"));
                        holder.completionProgress.setMax(objectives.getJSONArray("objectives").getJSONObject(i).getInt("completionValue"));
                        holder.completionProgress.setProgress(objectives.getJSONArray("objectives").getJSONObject(i).getInt("progress"));
                        String completionValueText = completionText(
                                valueStyle,
                                objectives.getJSONArray("objectives").getJSONObject(i).getInt("progress"),
                                max);
                        holder.completionText.setText(completionValueText);
                        break;
                    }
                }
            }

            boolean stepComplete = false;
            if (stepComplete) {
                holder.completeCheckBox.setChecked(true);
                holder.completionProgress.setMax(1);
                holder.completionProgress.setProgress(1);
                String completionValueText = completionText(valueStyle, progress, max);
                holder.completionText.setText(completionValueText);
            }
                //completed objectives do not have instance info associated with them.
            } catch (Exception e) {
                Log.d("Objectives Adapter", Log.getStackTraceString(e));
            }

        }

        @Override
        public int getItemCount() {
            return data.length();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox completeCheckBox;
            ProgressBar completionProgress;
            TextView objectiveText;
            TextView completionText;

            ViewHolder(final View itemView) {
                super(itemView);
                completeCheckBox = itemView.findViewById(R.id.objective_checkbox);
                completionProgress = itemView.findViewById(R.id.completionProgressBar);
                objectiveText = itemView.findViewById(R.id.objective_text);
                completionText = itemView.findViewById(R.id.completion_text);
            }
        }
    }

    private String completionText (int type, int progress, int max) {
        String text = "";
        switch (type) {
            case 3:
                double decimal = (double) progress / (double) max * 100;
                text = String.format(Locale.getDefault(), "%.2f", decimal)  + "%";
                break;
            default:
                text = progress + "/" + max;
        }
        return text;
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
                //Log.d("Icon Url", iconUrl);
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