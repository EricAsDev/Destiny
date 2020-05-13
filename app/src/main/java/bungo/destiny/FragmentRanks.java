package bungo.destiny;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class FragmentRanks extends Fragment {

    Data.Character character = ActivityMain.context.character;

    private String gloryStreakHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_glory_streak);
    private String valorStreakHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_valor_streak);
    private String gloryHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_glory);
    private String valorHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_valor);
    private String gambitHash = ActivityMain.context.getResources().getString(R.string.fixed_hash_gambit);
    private String gloryHashDetailed = ActivityMain.context.getResources().getString(R.string.fixed_hash_glory_detailed);
    private String valorHashDetailed = ActivityMain.context.getResources().getString(R.string.fixed_hash_valor_detailed);

    private int RANKS_RETRIEVED = 100;
    private int RANKS_UPDATED = 200;

    Handler handler;

    private JSONArray displayList = new JSONArray();

    private RankAdapter rankAdapter;
    private RecyclerView rankRecycler;
    private SwipeRefreshLayout swipeRefreshLayout;

    public FragmentRanks() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ranks, container, false);
        rankRecycler = view.findViewById(R.id.crucible_rank_recyclerView);
        RecyclerView.LayoutManager layoutManager;
        layoutManager = new LinearLayoutManager(ActivityCharacter.context, RecyclerView.VERTICAL, false);
        rankRecycler.setLayoutManager(layoutManager);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {

                switch (inputMessage.what){
                    case 100:
                        rankAdapter = new RankAdapter(displayList);
                        rankRecycler.setAdapter(rankAdapter);
                        rankAdapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                        break;
                    case 200:
                        getRankData();
                        break;
                }
            }
        };

        swipeRefreshLayout = view.findViewById(R.id.rank_refresh_layout);
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                displayList = new JSONArray();
                rankAdapter.notifyDataSetChanged();
                updateRankData();
                Log.d("Refresh", "Initiated");
            }
        });
        return view;
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        getRankData();
    }

    private void updateRankData () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject progressionData = new DestinyAPI().updateCharacter("202");
                    ActivityMain.context.character.setCharacterProgressions(progressionData.getJSONObject("Response").optJSONObject("characterProgressions"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Message message = new Message();
                message.what = RANKS_UPDATED;
                handler.sendMessage(message);
            }
        });
    }

    private void getRankData () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject gloryStreakProgression;
                JSONObject valorStreakProgression;
                JSONObject gloryProgression;
                JSONObject valorProgression;
                JSONObject gambitProgression;

                try {
                    String characterId = ActivityCharacter.context.characterId;
                    JSONObject getCharacterProgression = character.getCharacterProgressions();

                    JSONObject characterProgression = getCharacterProgression.getJSONObject("data").getJSONObject(characterId);
                    JSONObject progressions = characterProgression.getJSONObject("progressions");
                    for (int i = 0; i < progressions.length(); i++) {
                        Log.d("Progress", progressions.getJSONObject(progressions.names().getString(i)).toString());
                    }

                    gloryProgression = progressions.getJSONObject(gloryHashDetailed);
                    Log.d("Glory", gloryProgression.toString());
                    valorProgression = progressions.getJSONObject(valorHashDetailed);
                    gambitProgression = progressions.getJSONObject(gambitHash);

                    gloryStreakProgression = progressions.getJSONObject(gloryStreakHash);
                    valorStreakProgression = progressions.getJSONObject(valorStreakHash);
                    JSONObject gloryDefinition = new JSONObject(ActivityMain.context.defineElement(
                            ActivityMain.context.getSignedHash(gloryHashDetailed),"DestinyProgressionDefinition"));
                    JSONObject valorDefinition = new JSONObject(ActivityMain.context.defineElement(
                            ActivityMain.context.getSignedHash(valorHashDetailed),"DestinyProgressionDefinition"));
                    JSONObject gambitDefinition = new JSONObject(ActivityMain.context.defineElement(
                            ActivityMain.context.getSignedHash(gambitHash),"DestinyProgressionDefinition"));

                    String gloryStreak = gloryStreakProgression.getString("currentProgress");
                    String valorStreak = valorStreakProgression.getString("currentProgress");

                    String gloryValue = gloryProgression.getString("currentProgress");
                    int gloryProgressToNextLevel = gloryProgression.getInt("progressToNextLevel");
                    int gloryNextLevelAt = gloryProgression.getInt("nextLevelAt");
                    int gloryStepIndex = gloryProgression.getInt("stepIndex");
                    String gloryRankIcon = gloryDefinition.getJSONArray("steps").getJSONObject(gloryStepIndex).getString("icon");
                    String stepName = gloryDefinition.getJSONArray("steps").getJSONObject(gloryStepIndex).getString("stepName");
                    String glorySmallRankIcon = gloryDefinition.getString("rankIcon");
                    int alpha = gloryDefinition.getJSONObject("color").getInt("alpha");
                    int red = gloryDefinition.getJSONObject("color").getInt("red");
                    int green = gloryDefinition.getJSONObject("color").getInt("green");
                    int blue = gloryDefinition.getJSONObject("color").getInt("blue");
                    int RGB = android.graphics.Color.argb(alpha, red, green, blue);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("RGB", RGB);
                    jsonObject.put("smallRankIcon", glorySmallRankIcon);
                    jsonObject.put("stepName", stepName);
                    jsonObject.put("rankIcon", gloryRankIcon);
                    jsonObject.put("nextLevelAt", gloryNextLevelAt);
                    jsonObject.put("progressToNextLevel", gloryProgressToNextLevel);
                    jsonObject.put("value", gloryValue);
                    jsonObject.put("streak", gloryStreak);
                    jsonObject.put("mode", "69");

                    displayList.put(jsonObject);

                    gloryValue = valorProgression.getString("currentProgress");
                    gloryProgressToNextLevel = valorProgression.getInt("progressToNextLevel");
                    gloryNextLevelAt = valorProgression.getInt("nextLevelAt");
                    gloryStepIndex = valorProgression.getInt("stepIndex");
                    //if (gloryStepIndex > 5) gloryStepIndex = 5;
                    gloryRankIcon = valorDefinition.getJSONArray("steps").getJSONObject(gloryStepIndex).getString("icon");
                    stepName = valorDefinition.getJSONArray("steps").getJSONObject(gloryStepIndex).getString("stepName");
                    glorySmallRankIcon = valorDefinition.getString("rankIcon");
                    alpha = valorDefinition.getJSONObject("color").getInt("alpha");
                    red = valorDefinition.getJSONObject("color").getInt("red");
                    green = valorDefinition.getJSONObject("color").getInt("green");
                    blue = valorDefinition.getJSONObject("color").getInt("blue");
                    RGB = android.graphics.Color.argb(alpha, red, green, blue);

                    jsonObject = new JSONObject();
                    jsonObject.put("RGB", RGB);
                    jsonObject.put("smallRankIcon", glorySmallRankIcon);
                    jsonObject.put("stepName", stepName);
                    jsonObject.put("rankIcon", gloryRankIcon);
                    jsonObject.put("nextLevelAt", gloryNextLevelAt);
                    jsonObject.put("progressToNextLevel", gloryProgressToNextLevel);
                    jsonObject.put("value", gloryValue);
                    jsonObject.put("streak", valorStreak);
                    jsonObject.put("mode", "70");

                    displayList.put(jsonObject);

                    gloryValue = gambitProgression.getString("currentProgress");
                    gloryProgressToNextLevel = gambitProgression.getInt("progressToNextLevel");
                    gloryNextLevelAt = gambitProgression.getInt("nextLevelAt");
                    gloryStepIndex = gambitProgression.getInt("stepIndex");
                    //if (gloryStepIndex > 5) gloryStepIndex = 5;
                    gloryRankIcon = gambitDefinition.getJSONArray("steps").getJSONObject(gloryStepIndex).getString("icon");
                    stepName = gambitDefinition.getJSONArray("steps").getJSONObject(gloryStepIndex).getString("stepName");
                    glorySmallRankIcon = gambitDefinition.getString("rankIcon");
                    alpha = gambitDefinition.getJSONObject("color").getInt("alpha");
                    red = gambitDefinition.getJSONObject("color").getInt("red");
                    green = gambitDefinition.getJSONObject("color").getInt("green");
                    blue = gambitDefinition.getJSONObject("color").getInt("blue");
                    RGB = android.graphics.Color.argb(alpha, red, green, blue);

                    jsonObject = new JSONObject();
                    jsonObject.put("RGB", RGB);
                    jsonObject.put("smallRankIcon", glorySmallRankIcon);
                    jsonObject.put("stepName", stepName);
                    jsonObject.put("rankIcon", gloryRankIcon);
                    jsonObject.put("nextLevelAt", gloryNextLevelAt);
                    jsonObject.put("progressToNextLevel", gloryProgressToNextLevel);
                    jsonObject.put("value", gloryValue);
                    jsonObject.put("mode", "63");

                    displayList.put(jsonObject);

                    Log.d("Glory " + gloryStreak, gloryProgression.toString());
                    Log.d("Valor " + valorStreak, valorDefinition.toString());
                    Log.d("Gambit", gambitDefinition.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Message message = new Message();
        message.what = RANKS_RETRIEVED;
        handler.sendMessage(message);
    }

    public class RankAdapter extends RecyclerView.Adapter<RankAdapter.RankViewHolder> {

        private JSONArray itemsList;

        class RankViewHolder extends RecyclerView.ViewHolder {

            ProgressBar rankProgress;

            TextView rankValueText;
            TextView stepNameText;
            TextView rankStreak;

            ImageView rankIcon;
            ImageView rankIconSmall;

            View view;
            private RankViewHolder(View view) {
                super(view);
                rankProgress = view.findViewById(R.id.rank_progress);

                rankValueText = view.findViewById(R.id.rank_value_text);
                stepNameText = view.findViewById(R.id.rank_title);
                rankStreak = view.findViewById(R.id.rank_streak);

                rankIcon = view.findViewById(R.id.rank_icon);
                rankIconSmall = view.findViewById(R.id.icon_rank_small);

                this.view = view;

            }
        }

        RankAdapter(JSONArray list) {
            this.itemsList = list;
        }

        @Override
        @NonNull
        public RankAdapter.RankViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.rank_item, viewGroup, false);
            return new RankAdapter.RankViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RankViewHolder viewHolder, final int position) {
            try {
                JSONObject rankObject = itemsList.getJSONObject(position);
                int nextLevelAt = rankObject.getInt("nextLevelAt");
                int progressToNextLevel = rankObject.getInt("progressToNextLevel");
                int RGB = rankObject.getInt("RGB");
                final String mode = rankObject.getString("mode");

                String value = rankObject.getString("value");
                String stepName = rankObject.getString("stepName");
                String rankIcon = rankObject.getString("rankIcon");
                String smallRankIcon = rankObject.getString("smallRankIcon");

                String streakText = "";
                if (rankObject.has("streak")) {
                    String streak = rankObject.getString("streak");
                    streakText = "Streak: " + streak;
                }

                viewHolder.rankProgress.setMax(nextLevelAt);
                viewHolder.rankProgress.setProgress(progressToNextLevel);
                viewHolder.rankProgress.getProgressDrawable().setColorFilter(RGB, PorterDuff.Mode.SRC_ATOP);

                viewHolder.rankValueText.setText(value);
                viewHolder.stepNameText.setText(stepName);
                viewHolder.rankStreak.setText(streakText);

                new LoadImages(viewHolder.rankIcon).execute(rankIcon);
                new LoadImages(viewHolder.rankIconSmall).execute(smallRankIcon);

                viewHolder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ActivityCharacter.context, ActivityActivityList.class);
                        intent.putExtra("mode", mode);
                        startActivity(intent);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return itemsList.length();
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
}
