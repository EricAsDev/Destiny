package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ActivityActivityList extends AppCompatActivity {

    Handler handler;

    String mode = "";
    int ACTIVITIES_RECEIVED = 1;
    int PGCR_RECEIVED = 2;
    int ACTIVITIES_FAILED = 3;
    int activitiesToPull = 10;
    int currentCount = 0;

    JSONArray activitiesArray;
    JSONArray cardData;

    JSONObject activityDisplayData;

    private List<ActivityHistory> activityHistoryList;

    SwipeRefreshLayout swipeRefreshLayout;

    RecyclerView pgcr_card_recycler;
    LinearLayoutManager linearLayoutManager;
    CardAdapter cardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_list);
        final View parentView = findViewById(R.id.pgcr_parent);

        cardData = new JSONArray();
        activityHistoryList = Collections.synchronizedList(new ArrayList <ActivityHistory>());

        Intent receivedIntent = getIntent();
        mode = receivedIntent.getStringExtra("mode");
        //mode = "48";// Rumble (Team-less)

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 1:
                        getPGCRs();
                        break;
                    case 2:
                        Log.d("Card Added", activityHistoryList.get(inputMessage.arg1).getActivityId() + " at " + inputMessage.arg1);
                        setCounter(-1);
                        cardAdapter.notifyItemInserted(inputMessage.arg1);
                        break;
                    case 3:
                        Snackbar.make(parentView, inputMessage.obj.toString(), Snackbar.LENGTH_INDEFINITE).show();
                        break;
                }
            }
        };

        getActivities();

        activityDisplayData = getActivityDisplayData();

        swipeRefreshLayout = this.findViewById(R.id.pgcr_swipe_refresh);
        swipeRefreshLayout.setRefreshing(false);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                cardData = new JSONArray();
                activityHistoryList.clear();

                cardAdapter.notifyDataSetChanged();
                getActivities();
            }
        });

        pgcr_card_recycler = this.findViewById(R.id.pgcr_card_recycler);
        cardAdapter = new CardAdapter(this, activityHistoryList);
        linearLayoutManager = new LinearLayoutWrapperManager(this, RecyclerView.VERTICAL, false);
        linearLayoutManager.supportsPredictiveItemAnimations();
        pgcr_card_recycler.setLayoutManager(linearLayoutManager);
        pgcr_card_recycler.setAdapter(cardAdapter);

    }

    private void getActivities() {
        if (ActivityLogin.context.isAuthExpired()) {
            //todo must re-auth
            //ActivityLogin.context.requestAuth("pgcr");
        }

        setCounter(activitiesToPull);
        final String[] params = new String[4];
        params[0] = ActivityCharacter.context.characterId;
        params[1] = String.valueOf(activitiesToPull); //number of activities
        params[2] = mode;
        params[3] = "0"; //page number
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DestinyAPI destinyAPI = new DestinyAPI();
                    JSONObject activityResponse = destinyAPI.getActivities(params);
                    Log.d("Activity Response", activityResponse.toString());
                    if (!activityResponse.getString("ErrorCode").equals("1")) {
                        Message message = new Message();
                        message.what = ACTIVITIES_FAILED;
                        message.obj = activityResponse.getString("Message");
                        handler.sendMessage(message);
                        return;
                    } else {
                        activitiesArray = activityResponse.getJSONObject("Response").getJSONArray("activities");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Message message = new Message();
                message.what = ACTIVITIES_RECEIVED;
                handler.sendMessage(message);
            }
        });
    }

    private void getPGCRs () {
        try {
            for (int i = 0; i < activitiesArray.length(); i++) {
                final JSONObject activity = activitiesArray.getJSONObject(i);
                ActivityMain.threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run () {
                        try {
                            String period = activity.getString("period");

                            JSONObject activityDetails = activity.getJSONObject("activityDetails");
                            String instanceId = activityDetails.getString("instanceId");
                            String mode = activityDetails.getString("mode");

                            DestinyAPI destinyAPI = new DestinyAPI();
                            JSONObject pgcrResponse = destinyAPI.getPostGameCarnageReport(instanceId).getJSONObject("Response");
                            //Log.d("PGCR", pgcrResponse.toString());

                            JSONObject values = activity.getJSONObject("values");
                            //Log.d("Values", values.toString());
                            String standing = values.getJSONObject("standing").getJSONObject("basic").getString("displayValue");

                            boolean isTeamBased = false;
                            JSONArray teams = pgcrResponse.getJSONArray("teams");
                            if (teams.length() > 0) {
                                isTeamBased = true;
                            }
                            //Log.d("Teams", teams.toString());

                            //JSONArray entries = pgcrResponse.getJSONArray("entries");
                            //Log.d("Entries", entries.toString());

                            JSONObject modeDefinition = new JSONObject(searchModeDefinition(mode));

                            String referenceId = activityDetails.getString("referenceId");
                            String signedReferenceId = ActivityMain.context.getSignedHash(referenceId);
                            JSONObject referenceIdDefinition = new JSONObject(ActivityMain.context.defineElement(signedReferenceId, "DestinyActivityDefinition"));

                            String mapName = referenceIdDefinition.getJSONObject("displayProperties").getString("name");
                            //String mapDescription = referenceIdDefinition.getJSONObject("displayProperties").getString("description");
                            String mapImage = referenceIdDefinition.getString("pgcrImage");

                            ActivityHistory activityHistory = new ActivityHistory();
                            activityHistory.setMatchDate(period);
                            activityHistory.setStanding(standing);
                            activityHistory.setName(modeDefinition.getJSONObject("displayProperties").getString("name"));
                            activityHistory.setIcon(modeDefinition.getJSONObject("displayProperties").getString("icon"));
                            activityHistory.setIsTeamBased(isTeamBased);
                            activityHistory.setMapName(mapName);
                            activityHistory.setMapIcon(mapImage);
                            activityHistory.setActivityId(instanceId);

                            if (isTeamBased) {
                                if (teams.getJSONObject(0).getJSONObject("standing").getJSONObject("basic").getInt("value") == 0) {
                                    activityHistory.setWinnerScore(teams.getJSONObject(0).getJSONObject("score").getJSONObject("basic").getString("displayValue"));
                                    activityHistory.setLoserScore(teams.getJSONObject(1).getJSONObject("score").getJSONObject("basic").getString("displayValue"));
                                } else {
                                    activityHistory.setWinnerScore(teams.getJSONObject(1).getJSONObject("score").getJSONObject("basic").getString("displayValue"));
                                    activityHistory.setLoserScore(teams.getJSONObject(0).getJSONObject("score").getJSONObject("basic").getString("displayValue"));
                                }
                                //Log.d("Teams", teams.toString());
                            }

                            int index = 0;
                            if (activityHistoryList.size() == 0) {
                                activityHistoryList.add(activityHistory);
                            } else {
                                for (int i = 0; i < activityHistoryList.size(); i++) {
                                    int compareResult = period.compareTo(activityHistoryList.get(i).getMatchDate());
                                    if (compareResult > 0 || i == activityHistoryList.size() - 1) {
                                        //Log.d("Adding", "Index: " + i);
                                        activityHistoryList.add(i, activityHistory);
                                        index = i;
                                        break;
                                    }
                                }
                            }

                            Message message = new Message();
                            message.what = PGCR_RECEIVED;
                            message.arg1 = index;
                            //message.obj = activityHistory;
                            handler.sendMessage(message);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    JSONObject getActivityDisplayData () {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.activity_display_data);
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
            }

            return new JSONObject(writer.toString());
            //Log.d("ADD", activityDisplayData.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {
        private LayoutInflater layoutInflater;
        private List<ActivityHistory> activityHistory;

        CardAdapter(Context context, List<ActivityHistory> data){
            this.layoutInflater = LayoutInflater.from(context);
            this.activityHistory = data;
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            ImageView imageView;
            TextView matchType;
            TextView matchDetails;
            TextView matchDate;
            TextView matchResult;
            RecyclerView pgcrTeams;
            ProgressBar loadingTeams;
            ImageView mapIcon;
            TextView a_score;
            TextView b_score;
            ImageView left_teamIcon;
            ImageView right_teamIcon;

            ViewHolder(final View itemView) {
                super(itemView);
                itemView.setTag(this);
                imageView = itemView.findViewById(R.id.match_icon);
                matchType = itemView.findViewById(R.id.match_type);
                matchDetails = itemView.findViewById(R.id.match_details);
                matchResult = itemView.findViewById(R.id.match_result);
                matchDate = itemView.findViewById(R.id.match_date);
                loadingTeams = itemView.findViewById(R.id.load_teams);
                mapIcon = itemView.findViewById(R.id.card_background);
                a_score = itemView.findViewById(R.id.score_left);
                b_score = itemView.findViewById(R.id.score_right);
                left_teamIcon = itemView.findViewById(R.id.match_left);
                right_teamIcon = itemView.findViewById(R.id.match_right);
            }
        }

        @Override
        @NonNull
        public CardAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = layoutInflater.inflate(R.layout.card_layout, parent, false);
            return new CardAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final CardAdapter.ViewHolder holder, final int position) {

            try {
                String icon = activityHistory.get(position).getIcon();
                final String name = activityHistory.get(position).getName();
                String mapName = activityHistory.get(position).getMapName();
                String matchDate = activityHistory.get(position).getMatchDate();
                String matchResult = activityHistory.get(position).getStanding();
                String mapIcon = activityHistory.get(position).getMapIcon();
                String a_score = activityHistory.get(position).getWinnerScore();
                String b_score = activityHistory.get(position).getLoserScore();

                holder.matchType.setText(name);
                holder.matchDetails.setText(mapName);
                holder.matchDate.setText(convertDate(matchDate));
                holder.matchResult.setText(matchResult);

                if (activityHistory.get(position).isTeamBased) {
                    Bitmap blue_icon = BitmapFactory.decodeResource(getResources(), R.drawable.icon_bravo);
                    Bitmap red_icon = BitmapFactory.decodeResource(getResources(), R.drawable.icon_alpha);

                    if (matchResult.equals("Victory")) {
                        holder.left_teamIcon.setImageBitmap(blue_icon);
                        holder.right_teamIcon.setImageBitmap(red_icon);
                    } else {
                        holder.left_teamIcon.setImageBitmap(red_icon);
                        holder.right_teamIcon.setImageBitmap(blue_icon);
                    }
                    holder.a_score.setText(a_score);
                    holder.b_score.setText(b_score);
                }

                holder.loadingTeams.setVisibility(View.INVISIBLE);
                if (!icon.isEmpty()) new LoadImages(holder.imageView).execute(icon);
                if (!mapIcon.isEmpty()) new LoadImages(holder.mapIcon).execute(mapIcon);

                setAnimation(holder.itemView);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String activityData = activityHistory.get(position).getActivityId();
                        Intent intent = new Intent(ActivityActivityList.this, ActivityPGCR.class);
                        intent.putExtra("activityData", activityData);
                        startActivity(intent);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        @Override
        public int getItemCount() {
            return activityHistory.size();
        }

        private void setAnimation (View animateView) {
            Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.slide_in_left);
            animateView.startAnimation(animation);
        }
    }

    private void setCounter (int countAdjust) {
        currentCount = currentCount + countAdjust;
        if (currentCount == 0) {
            swipeRefreshLayout.setRefreshing(false);
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

    public String searchModeDefinition (String hash) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityLogin.getContext());
        String filename = sharedPreferences.getString("Database", null);
        String definitionType = "DestinyActivityModeDefinition";
        if (filename!=null) {
            File file = new File(ActivityLogin.getContext().getDir("Files", Context.MODE_PRIVATE), filename);
            SQLiteDatabase sqLiteDatabase = openOrCreateDatabase(file.toString(), MODE_PRIVATE, null);
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + definitionType, null);
            cursor.moveToFirst();

            //Log.d("Cursor", String.valueOf(cursor.getCount()));
            try {
                String data = "";
                for (int i = 0; i < cursor.getCount(); i++) {
                    data = cursor.getString(1);
                    JSONObject jsonObject = new JSONObject(data);
                    String modeType = jsonObject.optString("modeType");
                    if (!modeType.isEmpty()) {
                        if (modeType.equals(hash)) {
                            break;
                        }
                    }
                    cursor.moveToNext();
                }
                sqLiteDatabase.close();
                cursor.close();
                return data;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    String convertDate (String period) {
        SimpleDateFormat  format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = format.parse(period);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return String.valueOf(date);
    }

    private class ActivityHistory {
        String activityId;
        String icon;
        String name;
        String mapName;
        String mapIcon;
        String matchDate;
        String matchResult;
        String winnerScore;
        String loserScore;
        String myTeam;
        String standing;
        boolean isTeamBased;


        public void setActivityId(String activityId) {
            this.activityId = activityId;
        }

        public void setIsTeamBased (boolean isTeamBased) {
            this.isTeamBased = isTeamBased;
        }

        public void setStanding(String standing) {
            this.standing = standing;
        }

        public void setMyTeam(String myTeam) {
            this.myTeam = myTeam;
        }

        public void setLoserScore(String b_score) {
            this.loserScore = b_score;
        }

        public void setWinnerScore(String a_score) {
            this.winnerScore = a_score;
        }

        public void setMatchResult(String matchResult) {
            this.matchResult = matchResult;
        }

        public void setMatchDate(String matchDate) {
            this.matchDate = matchDate;
        }

        public void setMapIcon(String mapIcon) {
            this.mapIcon = mapIcon;
        }

        public void setMapName(String mapName) {
            this.mapName = mapName;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getActivityId() {
            return activityId;
        }

        public boolean getIsTeamBased () {
            return isTeamBased;
        }
        public String getStanding() {
            return standing;
        }

        public String getMyTeam() {
            return myTeam;
        }

        public String getLoserScore() {
            return loserScore;
        }

        public String getWinnerScore() {
            return winnerScore;
        }

        public String getMapIcon() {
            return mapIcon;
        }

        public String getMatchResult() {
            return matchResult;
        }

        public String getMatchDate() {
            return matchDate;
        }

        public String getMapName() {
            return mapName;
        }

        public String getName() {
            return name;
        }

        public String getIcon() {
            return icon;
        }
    }

    private class LinearLayoutWrapperManager extends LinearLayoutManager {
        public LinearLayoutWrapperManager (Context context) {
            super(context);
        }
        public LinearLayoutWrapperManager (Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }
        public LinearLayoutWrapperManager (Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
            super(context, attributeSet, defStyleAttr, defStyleRes);
        }
        @Override
        public boolean supportsPredictiveItemAnimations() {
            return false;
        }
    }

}
