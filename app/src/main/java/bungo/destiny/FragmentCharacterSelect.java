package bungo.destiny;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class FragmentCharacterSelect extends Fragment {

    JSONArray menuItems;

    //Data data = new Data();
    Data.Profile profile = ActivityMain.context.profile;
    Data.Character character = ActivityMain.context.character;
    Data.Clan clan = ActivityMain.context.clan;
    Data.Account account = ActivityMain.context.account;

    RecyclerView.Adapter menuAdapter;
    RecyclerView menuRecycler;

    public FragmentCharacterSelect() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_character_select, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        menuRecycler = ActivityMain.context.findViewById(R.id.menu_recyclerView);

    }

    void buildCharacterFragment () {

        menuItems = new JSONArray();

        TextView nameTextView = ActivityMain.context.findViewById(R.id.user_name_value);
        TextView clanTextView = ActivityMain.context.findViewById(R.id.clan_name_value);

        try {
            String playerName = account.getAccountData()
                    .getJSONObject("Response")
                    .getJSONObject("bungieNetUser")
                    .getString("displayName");
            nameTextView.setText(playerName);

            String clanName = clan.getClanData()
                    .getJSONArray("results")
                    .getJSONObject(0)
                    .getJSONObject("group")
                    .getString("name");
            clanTextView.setText(clanName);

        } catch (Exception e) {
            Log.d("Error reading clan data", e.toString());
        }


        RecyclerView.LayoutManager layOutManager = new LinearLayoutManager(ActivityMain.context);
        menuRecycler.setLayoutManager(layOutManager);
        menuAdapter = new MainMenuAdapter(menuItems);
        menuRecycler.setAdapter(menuAdapter);

        buildMainMenu();

    }

    private void buildMainMenu () {
        try {
            JSONArray characterIds  =  character.getCharacters().getJSONObject("data").names();

            for (int i = 0; i < characterIds.length(); i++) {
                String classType;
                String raceType;
                String genderType;
                String level;
                String lightLevel;
                String imagePath;
                String iconPath;

                String characterId = characterIds.getString(i);
                JSONObject characterObject = character.getCharacters().getJSONObject("data").getJSONObject(characterIds.getString(i));

                String signedHash = ActivityMain.context.getSignedHash(characterObject.getString("classHash"));
                JSONObject classTypeObject = new JSONObject(ActivityMain.context.defineElement(String.valueOf(signedHash), "DestinyClassDefinition"));
                classType = classTypeObject.getJSONObject("displayProperties").getString("name");

                signedHash = ActivityMain.context.getSignedHash(characterObject.getString("raceHash"));
                JSONObject raceTypeObject = new JSONObject(ActivityMain.context.defineElement(String.valueOf(signedHash), "DestinyRaceDefinition"));
                raceType = raceTypeObject.getJSONObject("displayProperties").getString("name");

                signedHash = ActivityMain.context.getSignedHash(characterObject.getString("genderHash"));
                JSONObject genderTypeObject = new JSONObject(ActivityMain.context.defineElement(String.valueOf(signedHash), "DestinyGenderDefinition"));
                genderType = genderTypeObject.getJSONObject("displayProperties").getString("name");

                lightLevel = characterObject.getString("light");
                level = characterObject.getString("baseCharacterLevel");
                imagePath = characterObject.getString("emblemBackgroundPath");
                iconPath = characterObject.getString("emblemPath");

                JSONObject menuCharacterDataObject = new JSONObject();
                menuCharacterDataObject.put("class", classType);
                menuCharacterDataObject.put("race", raceType);
                menuCharacterDataObject.put("gender", genderType);
                menuCharacterDataObject.put("level", level);
                menuCharacterDataObject.put("lightLevel", lightLevel);
                menuCharacterDataObject.put("imagePath", imagePath);
                menuCharacterDataObject.put("emblemPath", iconPath);
                menuCharacterDataObject.put("characterId", characterId);

                menuItems.put(menuCharacterDataObject);

                menuAdapter.notifyDataSetChanged();

            }

            ProgressBar progressBar = ActivityMain.context.findViewById(R.id.mainActivityProgressBar);
            progressBar.setVisibility(View.GONE);

        } catch (Exception e) {
            Log.d("Menu Building", e.toString());
        }
    }

    public class MainMenuAdapter extends RecyclerView.Adapter<MainMenuAdapter.MainMenuViewHolder> {

        private JSONArray menuRecyclerData;

        class MainMenuViewHolder extends RecyclerView.ViewHolder {
            private TextView classText;
            private TextView genderText;
            private TextView raceText;
            private TextView lightText;
            private TextView levelText;
            private ImageView backgroundImage;
            private ImageView emblemImage;
            private MainMenuViewHolder (View view) {
                super(view);
                classText = view.findViewById(R.id.banner_class);
                genderText = view.findViewById(R.id.banner_gender);
                raceText = view.findViewById(R.id.banner_race);
                lightText = view.findViewById(R.id.banner_lLevel);
                levelText = view.findViewById(R.id.banner_level);
                backgroundImage = view.findViewById(R.id.banner_image);
                emblemImage = view.findViewById(R.id.banner_icon);
            }
        }

        MainMenuAdapter(JSONArray dataArray) {
            menuRecyclerData = dataArray;
        }

        @Override
        public MainMenuAdapter.MainMenuViewHolder onCreateViewHolder (ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.character_banner, viewGroup, false);
            return new MainMenuAdapter.MainMenuViewHolder(view);
        }

        @Override
        public void onBindViewHolder (@NonNull MainMenuAdapter.MainMenuViewHolder mainMenuViewHolder, final int position) {
            try {
                mainMenuViewHolder.classText.setText(menuRecyclerData.getJSONObject(position).optString("class"));
                mainMenuViewHolder.raceText.setText(menuRecyclerData.getJSONObject(position).optString("race"));
                mainMenuViewHolder.genderText.setText(menuRecyclerData.getJSONObject(position).optString("gender"));
                mainMenuViewHolder.lightText.setText(getResources().getText(R.string.light) + menuRecyclerData.getJSONObject(position).optString("lightLevel"));
                mainMenuViewHolder.levelText.setText(menuRecyclerData.getJSONObject(position).optString("level"));

                String emblemPath = menuRecyclerData.getJSONObject(position).optString("emblemPath");
                if (!emblemPath.equals("")) {
                    emblemPath = emblemPath.replaceAll("'\'/", "/");
                    String[] imageParams = new String[1];
                    imageParams[0] = "https://www.bungie.net" + emblemPath;
                    String backgroundPath = emblemPath.substring(emblemPath.lastIndexOf("/") + 1);
                    Bitmap emblem = new ActivityMain().getImage(backgroundPath);
                    if (emblem == null) {
                        ActivityMain activityMain = new ActivityMain();
                        emblem = activityMain.new DownloadImage().execute(imageParams).get();
                        Log.d("Image", "Storing");
                        new ActivityMain().storeImage(emblem, backgroundPath);
                        mainMenuViewHolder.emblemImage.setImageBitmap(emblem);
                    } else {
                        mainMenuViewHolder.emblemImage.setImageBitmap(emblem);
                    }
                }

                String imagePath = menuRecyclerData.getJSONObject(position).optString("imagePath");
                if (!imagePath.equals("")) {
                    imagePath = imagePath.replaceAll("'\'/", "/");

                    String[] imageParams = new String[1];
                    imageParams[0] = "https://www.bungie.net" + imagePath.replaceAll("'\'/", "/");
                    String backgroundPath = imagePath.substring(imagePath.lastIndexOf("/") + 1);
                    Bitmap background = new ActivityMain().getImage(backgroundPath);
                    if (background == null) {
                        ActivityMain activityMain = new ActivityMain();
                        background = activityMain.new DownloadImage().execute(imageParams).get();
                        Log.d("Image", "Storing");
                        new ActivityMain().storeImage(background, backgroundPath);
                        mainMenuViewHolder.backgroundImage.setImageBitmap(background);
                    } else {
                        mainMenuViewHolder.backgroundImage.setImageBitmap(background);
                    }
                }

                mainMenuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (position < 3) {
                            try {
                                //Intent intent = new Intent(ActivityMain.context, ActivityCharacter.class);
                                Intent intent = new Intent(ActivityMain.context, ActivityCharacter.class);

                                intent.putExtra("characterId", menuRecyclerData
                                        .getJSONObject(position)
                                        .getString("characterId"));
                                ActivityMain.context.startActivity(intent);
                            } catch (Exception e) {
                                Log.d("Char Select Error", e.toString());
                            }

                        } else if (position == 3) {
                            //Intent intent = new Intent(ActivityMain.context, VaultActivity.class);
                            //ActivityMain.context.startActivity(intent);
                        }
                    }
                });

            } catch (Exception e) {
                Log.d("MenuAdapter", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return menuRecyclerData.length();
        }

    }
}
