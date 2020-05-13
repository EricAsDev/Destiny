package bungo.destiny;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

public class DialogCharacterPicker extends Dialog implements View.OnClickListener {

    private JSONArray displayList;
    Handler handler;

    private int selectedItem;

    private int CHARACTER_BUILT = 100;

    private ReturnSelection returnSelection;

    private CharacterSelectAdapter characterSelectAdapter;
    private RecyclerView characterRecycler;
    private RecyclerView.LayoutManager characterLayoutManager;

    public DialogCharacterPicker(Activity context) {
        super(context);
        this.returnSelection = null;
    }

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_character_select);
        TextView buttonConfirm = findViewById(R.id.character_select_yes);
        TextView buttonCancel = findViewById(R.id.character_select_no);

        buttonConfirm.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);

        characterRecycler = findViewById(R.id.select_character_recycler);
        characterLayoutManager = new LinearLayoutManager(ActivityCharacter.context, LinearLayoutManager.VERTICAL, false);
        characterRecycler.setLayoutManager(characterLayoutManager);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                if (inputMessage.what == 100) {
                    characterSelectAdapter = new CharacterSelectAdapter(displayList);
                    characterRecycler.setAdapter(characterSelectAdapter);
                    characterSelectAdapter.notifyDataSetChanged();
                }
            }
        };

        buildCharacterItems();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.character_select_yes:
                Log.d("Button", "Select");
                try {
                    returnSelection.selectObject(displayList.getJSONObject(selectedItem).getString("characterId"));
                } catch (Exception e) {
                    Log.d("Transfer characterId", e.toString());
                }
                dismiss();
                break;
            case R.id.character_select_no:
                Log.d("Button", "Cancel");
                dismiss();
                break;
        }
    }

    private void buildCharacterItems () {
        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                displayList = new JSONArray();

                try {
                    Data.Character character = ActivityMain.context.character;
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

                        displayList.put(menuCharacterDataObject);
                    }

                } catch (Exception e) {
                    Log.d("Unequipped Sort Error", e.toString());
                }

                Message message = new Message();
                message.what = CHARACTER_BUILT;
                handler.sendMessage(message);
            }
        });
    }

    public interface ReturnSelection {
        void selectObject(String itemInstanceHash);
    }

    public void setReturnSelection (ReturnSelection returnSelection) {
        this.returnSelection = returnSelection;
    }

    public class CharacterSelectAdapter extends RecyclerView.Adapter<CharacterSelectAdapter.ItemsViewHolder> {

        JSONArray charactersRecyclerData;

        class ItemsViewHolder extends RecyclerView.ViewHolder {
            private TextView classText;
            private TextView genderText;
            private TextView raceText;
            private TextView lightText;
            private TextView levelText;
            private ImageView backgroundImage;
            private ImageView emblemImage;
            
            private ItemsViewHolder(final View view) {
                super(view);

                classText = view.findViewById(R.id.banner_class);
                genderText = view.findViewById(R.id.banner_gender);
                raceText = view.findViewById(R.id.banner_race);
                lightText = view.findViewById(R.id.banner_lLevel);
                levelText = view.findViewById(R.id.banner_level);
                backgroundImage = view.findViewById(R.id.banner_image);
                emblemImage = view.findViewById(R.id.banner_icon);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("Click Dialog Item", String.valueOf(getAdapterPosition()));
                        selectedItem = getAdapterPosition();
                    }
                });
            }
        }

        CharacterSelectAdapter(JSONArray dataArray) {
            try {
                charactersRecyclerData = dataArray;
            } catch (Exception e) {
                Log.d("Character Recycler", e.toString());
            }
        }

        @Override
        @NonNull
        public CharacterSelectAdapter.ItemsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.character_banner, viewGroup, false);
            return new CharacterSelectAdapter.ItemsViewHolder(view);

        }

        @Override
        public void onBindViewHolder(@NonNull CharacterSelectAdapter.ItemsViewHolder viewHolder, final int position) {
            try {
                viewHolder.classText.setText(charactersRecyclerData.getJSONObject(position).optString("class"));
                viewHolder.raceText.setText(charactersRecyclerData.getJSONObject(position).optString("race"));
                viewHolder.genderText.setText(charactersRecyclerData.getJSONObject(position).optString("gender"));
                viewHolder.lightText.setText(charactersRecyclerData.getJSONObject(position).optString("lightLevel"));
                viewHolder.levelText.setText(charactersRecyclerData.getJSONObject(position).optString("level"));

                String emblemPath = charactersRecyclerData.getJSONObject(position).optString("emblemPath");
                if (!emblemPath.isEmpty()) {
                    new LoadImages(viewHolder.emblemImage).execute(emblemPath);
                }

                String imagePath = charactersRecyclerData.getJSONObject(position).optString("imagePath");
                if (!imagePath.isEmpty()) {
                    new LoadImages(viewHolder.backgroundImage).execute(imagePath);
                }
            } catch (Exception e) {
                Log.d("Character Select", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return charactersRecyclerData.length();
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
                Log.d("Load Inventory Images", e.getMessage());
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
