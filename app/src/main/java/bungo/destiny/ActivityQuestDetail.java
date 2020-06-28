package bungo.destiny;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityQuestDetail extends AppCompatActivity {

    JSONObject inventoryItem;
    public JSONObject itemDefinition;
    public String characterId;
    public Context context;

    int stepPosition;
    int trackingValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest_detail);
        context = this;

        View titleLayout = findViewById(R.id.layout_title);
        View objectivesLayout = findViewById(R.id.layout_objectives);

        Intent receivedIntent = getIntent();

        try {
            characterId = receivedIntent.getStringExtra("characterId");
            inventoryItem = new JSONObject(receivedIntent.getStringExtra("item"));
            String itemHash = inventoryItem.getString("itemHash");
            String signedHash = ActivityMain.context.getSignedHash(itemHash);
            itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
            trackingValue = getTrackingValue(itemHash);
            stepPosition = getStepPosition(itemHash);

            Log.d("Item", inventoryItem.toString());
            Log.d("Definition", itemDefinition.toString());

            setTitleData(titleLayout);

            setSteps(objectivesLayout);

            setRewards(objectivesLayout);

        } catch (Exception e) {
            Log.e("Quest Detail", Log.getStackTraceString(e));
        }
    }

    private void setTitleData (View view) {
        TextView title = view.findViewById(R.id.text_title);
        View background = view.findViewById(R.id.layout_title);
        try {
            if (itemDefinition.has("setData")) {
                title.setText(itemDefinition.getJSONObject("setData").getString("questLineName"));
            } else {
                title.setText(itemDefinition.getJSONObject("displayProperties").getString("name"));
            }

            int tierType = itemDefinition.getJSONObject("inventory").getInt("tierType");
            int colorCode;

            switch (tierType) {
                case 6:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.exotic, null);
                    break;
                case 5:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.legendary, null);
                    break;
                case 4:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.rare, null);
                    break;
                case 3:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.common, null);
                    break;
                case 2:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.basic, null);
                    break;
                default:
                    colorCode = ActivityMain.context.getResources().getColor(R.color.no_tier, null);
                    break;
            }
            background.setBackgroundColor(colorCode);

        } catch (Exception e) {
            Log.e("Set Title", Log.getStackTraceString(e));
        }
    }

    private void setSteps (View view) {
        //to display quest steps and progress. set data is each step in a quest.
        //use getObjectives for each item in setData
        RecyclerView step_recycler = view.findViewById(R.id.recycler_objectives);
        StepsAdapter stepsAdapter = new StepsAdapter(this, itemDefinition);
        RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(step_recycler);
        step_recycler.setLayoutManager(layOutManager);
        step_recycler.setAdapter(stepsAdapter);
        layOutManager.scrollToPosition(stepPosition);

    }

    private void setRewards (View view) {
        //rewards are for bounties only. where are the rewards for QUESTS?
        //rewards are found in the item definition, "value" object. See DestinyItemValueBlockDefinition
        try {
            if (itemDefinition.has("value")) {
                View rewardsView = view.findViewById(R.id.layout_rewards);
                RecyclerView reward_recycler = rewardsView.findViewById(R.id.recycler_rewards);
                rewardsView.setVisibility(View.VISIBLE);
                JSONArray itemValue = itemDefinition.getJSONObject("value").getJSONArray("itemValue");
                JSONArray rewards = new JSONArray();
                for (int i = 0; i < itemValue.length(); i++) {
                    if (!itemValue.getJSONObject(i).getString("itemHash").equals("0")) {
                        rewards.put(itemValue.getJSONObject(i));
                    }
                }

                RewardAdapter rewardAdapter = new RewardAdapter(this, rewards);
                RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityCharacter.context);
                reward_recycler.setLayoutManager(layOutManager);
                reward_recycler.setAdapter(rewardAdapter);

            }
        } catch (Exception e) {
            Log.d("Rewards", Log.getStackTraceString(e));
        }
    }

    static class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        JSONArray data;
        ViewHolder viewHolder;

        RewardAdapter(Context context, JSONArray data){
            this.layoutInflater = LayoutInflater.from(context);
            this.data = data;
        }

        @Override
        @NonNull
        public RewardAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.quest_reward, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            viewHolder = holder;
            try {
                String itemHash = data.getJSONObject(position).getString("itemHash");
                //String quantity = data.getJSONObject(position).getString("quantity");

                String signedHash = ActivityMain.context.getSignedHash(itemHash);
                JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedHash, "DestinyInventoryItemDefinition"));
                String name = itemDefinition.getJSONObject("displayProperties").getString("name");
                String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");

                holder.rewardTextView.setText(name);
                new LoadImages(holder.rewardIconImageView).execute(icon);

            } catch (Exception e) {
                Log.d("Reward", Log.getStackTraceString(e));
            }
        }

        @Override
        public int getItemCount() {
            return data.length();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView rewardIconImageView;
            TextView rewardTextView;

            ViewHolder(final View itemView) {
                super(itemView);
                rewardIconImageView = itemView.findViewById(R.id.reward_icon);
                rewardTextView = itemView.findViewById(R.id.reward_text);
            }
        }
    }

    class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        JSONArray data;
        ViewHolder viewHolder;

        StepsAdapter(Context context, JSONObject data){
            this.layoutInflater = LayoutInflater.from(context);
            try {
                if (data.has("setData")) {
                    //setData means item is a quest and has multiple steps
                    this.data = data.getJSONObject("setData").getJSONArray("itemList");

                } else {
                    //no step data so, only one thing to do. put the itemHash here instead
                    JSONObject stepData = new JSONObject().put("itemHash", inventoryItem.getString("itemHash"));
                    this.data = new JSONArray().put(stepData);
                    //Log.d("Item", data.toString());
                }

            } catch (Exception e) {
                Log.d("Steps Adapter", Log.getStackTraceString(e));
            }
        }

        @Override
        @NonNull
        public StepsAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.quest_step, parent, false);
            setRewards(view);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            viewHolder = holder;
            try {
                String stepHash = data.getJSONObject(position).getString("itemHash");
                String signedStepHash = ActivityMain.context.getSignedHash(stepHash);

                JSONObject stepDefinition = new JSONObject(ActivityMain.context.defineElement(signedStepHash, "DestinyInventoryItemDefinition"));
                String stepText = stepDefinition.getJSONObject("displayProperties").getString("name");
                String description = stepDefinition.getJSONObject("displayProperties").getString("description");
                String displaySource = stepDefinition.getString("displaySource");
                viewHolder.step_text.setText(stepText);
                viewHolder.step_description.setText(description);
                viewHolder.step_displaySource.setText(displaySource);

                if (!itemDefinition.has("setData")) {
                    viewHolder.step_text.setText("");
                }

                if (stepDefinition.has("objectives")) {
                    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(ActivityQuestDetail.this, LinearLayoutManager.VERTICAL, false);
                    ObjectivesAdapter objectivesAdapter = new ObjectivesAdapter(ActivityQuestDetail.this, stepDefinition);
                    viewHolder.step_recycler.setAdapter(objectivesAdapter);
                    viewHolder.step_recycler.setLayoutManager(layoutManager);
                }

            } catch (Exception e) {
                Log.d("Step", Log.getStackTraceString(e));
            }
        }

        @Override
        public int getItemCount() {
            return data.length();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView step_text;
            TextView step_displaySource;
            TextView step_description;
            RecyclerView step_recycler;
            ConstraintLayout step_layout;

            ViewHolder(final View itemView) {
                super(itemView);
                step_text = itemView.findViewById(R.id.step_text);
                step_description = itemView.findViewById(R.id.text_description);
                step_displaySource = itemView.findViewById(R.id.step_displaySource);
                step_recycler = itemView.findViewById(R.id.step_recycler);
                step_layout = itemView.findViewById(R.id.step_layout);
            }
        }
    }

    class ObjectivesAdapter extends RecyclerView.Adapter<ObjectivesAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        JSONArray data;
        boolean stepComplete = false;
        boolean stepFuture = false;
        Context context;

        ObjectivesAdapter(Context context, JSONObject stepDefinition) {
            this.layoutInflater = LayoutInflater.from(context);
            this.context = context;
            try {
                this.data = stepDefinition.getJSONObject("objectives").getJSONArray("objectiveHashes");
                if (getTrackingValue(stepDefinition.getString("hash")) < trackingValue) {
                    stepComplete = true;
                } else if (getTrackingValue(stepDefinition.getString("hash")) > trackingValue) {
                    stepFuture = true;
                }
            } catch (Exception e) {
                Log.d("Objectives Adapter", Log.getStackTraceString(e));
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
                            .getJSONArray(inventoryItem.getString("itemHash"));

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
                    //String itemInstanceId = inventoryItem.getJSONObject("inventoryItem").getString("itemInstanceId");
                    String itemInstanceId = inventoryItem.getString("itemInstanceId");
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

                if (stepComplete) {
                    holder.completeCheckBox.setChecked(true);
                    holder.completionProgress.setMax(1);
                    holder.completionProgress.setProgress(1);
                    String completionValueText = completionText(valueStyle, progress, max);
                    holder.completionText.setText(completionValueText);
                }

                if (stepFuture) {
                    String completionValueText = completionText(
                            valueStyle,
                            0,
                            max);
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

    private int getStepPosition (String itemHash) {
        int stepPosition = 0;
        try {
            if (itemDefinition.has("setData")) {
                JSONArray itemList = itemDefinition.getJSONObject("setData").getJSONArray("itemList");
                for (int i = 0; i < itemList.length(); i++) {
                    if (itemList.getJSONObject(i).getString("itemHash").equals(itemHash)) {
                        stepPosition = i;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.d("Step Position", Log.getStackTraceString(e));
        }

        return stepPosition;
    }

    private int getTrackingValue (String itemHash) {
        int trackingValue = 0;
        try {
            if (itemDefinition.has("setData")) {
                JSONArray itemList = itemDefinition.getJSONObject("setData").getJSONArray("itemList");
                for (int i = 0; i < itemList.length(); i++) {
                    if (itemList.getJSONObject(i).getString("itemHash").equals(itemHash)) {
                        trackingValue = itemDefinition.getJSONObject("setData").getJSONArray("itemList").getJSONObject(i).getInt("trackingValue");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.d("Tracking Value", Log.getStackTraceString(e));
        }
        return trackingValue;
    }

    private String completionText (int type, int progress, int max) {
        String text;
        if (type == 3) {
            double decimal = (double) progress / (double) max * 100;
            text = String.format(Locale.getDefault(), "%.2f", decimal) + "%";
        } else {
            text = progress + "/" + max;
        }
        return text;
    }

    /* value style enum:
   0:automatic
   1:fraction
   2:checkbox
   3:percentage
   4:datatime
   5:fractionfloat
   6:integer
   7:timeduration
   8:hidden
   9:multiplier
   10:greenpips
   11:redpips
   12:explicitpercentage
   13:rawfloat
     */

}
