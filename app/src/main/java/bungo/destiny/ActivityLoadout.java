package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.text.InputType.TYPE_CLASS_TEXT;
import static android.text.InputType.TYPE_NULL;

public class ActivityLoadout extends AppCompatActivity {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityLogin.getContext());
    SharedPreferences.Editor editor = sharedPreferences.edit();

    Bitmap icon_primary;
    Bitmap icon_special;
    Bitmap icon_heavy;

    JSONObject loadoutObject;
    JSONArray loadoutItems;

    ImageView editTextImage;
    EditText titleEditText;
    RecyclerView itemsRecycler;
    LoadoutAdapter loadoutAdapter;

    ArrayList<Bitmap> bitmapArrayList;

    private ActivityLoadout context;

    int position = 0;

    Data.Character character = ActivityMain.context.character;
    List<String> armorTypes = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loadout);
        context = this;

        icon_primary = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_primary);
        icon_special = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_special);
        icon_heavy = BitmapFactory.decodeResource(getResources(), R.drawable.icon_ammo_heavy);

        itemsRecycler = findViewById(R.id.loadout_items);
        titleEditText = findViewById(R.id.title);
        editTextImage = findViewById(R.id.edit_title);

        armorTypes.add(0, "22");
        armorTypes.add(1, "23");
        armorTypes.add(2, "21");

        Intent receivedIntent = getIntent();
        try {
            position = receivedIntent.getIntExtra("loadoutData", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String characterId = ActivityCharacter.context.characterId;
            String loadoutObjectString = sharedPreferences.getString(characterId, null);
            JSONArray jsonArray = new JSONArray(loadoutObjectString);
            if (!jsonArray.isNull(position)) {
                loadoutObject = jsonArray.getJSONObject(position);
                if (loadoutObject.has("title")) titleEditText.setText(loadoutObject.getString("title"));
                if (loadoutObject.has("items")) loadoutItems = loadoutObject.getJSONArray("items");
            } else {
                Log.d("Error", "Loadout data not found");
            }

            Log.d("items", loadoutItems.toString());
        }catch (Exception e) {
            e.printStackTrace();
        }

        editTextImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                titleEditText.setInputType(TYPE_CLASS_TEXT);
                titleEditText.setFocusable(true);
                titleEditText.setFocusableInTouchMode(true);
                titleEditText.requestFocus();
                titleEditText.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            titleEditText.setInputType(TYPE_NULL);
                            titleEditText.setFocusable(false);
                            inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                            try {
                                loadoutObject.put("title", titleEditText.getText());
                                saveLoadout(position);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return true;
                    }
                });
            }
        });

        loadoutAdapter = new LoadoutAdapter(loadoutItems);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false);
        itemsRecycler.setLayoutManager(layoutManager);
        itemsRecycler.setAdapter(loadoutAdapter);

        ItemTouchHelper itemTouchHelper = new
                ItemTouchHelper(new SwipeToDeleteCallback(loadoutAdapter));
        itemTouchHelper.attachToRecyclerView(itemsRecycler);

        bitmapArrayList = new ArrayList<>();
        bitmapArrayList.add(0, BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.loadout_kinetic));
        bitmapArrayList.add(0, BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.loadout_kinetic));
        bitmapArrayList.add(1, BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.loadout_energy));
        bitmapArrayList.add(2, BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.loadout_power));
        bitmapArrayList.add(3, BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.loadout_hat));
        bitmapArrayList.add(4, BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.loadout_gloves));
        bitmapArrayList.add(5, BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.loadout_chest));
        bitmapArrayList.add(6, BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.loadout_shoes));
        bitmapArrayList.add(7, BitmapFactory.decodeResource(ActivityCharacter.context.getResources(), R.drawable.loadout_blank));

    }

    public class LoadoutAdapter extends RecyclerView.Adapter<LoadoutAdapter.ViewHolder> {

        JSONArray loadoutData;

        class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;
            private ImageView elementImageView;
            private ImageView inventoryItemBackground;
            private ImageView swapImageView;
            private ImageView ammoIcon;
            private TextView powerTextView;
            private TextView titleTextView;
            private TextView typeTextView;
            private View itemView;

            private ViewHolder(final View view) {
                super(view);
                imageView = view.findViewById(R.id.inventory_item_image);
                elementImageView = view.findViewById(R.id.element_icon);
                inventoryItemBackground = view.findViewById(R.id.inventory_item_background);
                swapImageView = view.findViewById(R.id.swap_image);
                ammoIcon = view.findViewById(R.id.ammunition_type_icon);

                titleTextView = view.findViewById(R.id.equipped_text);
                powerTextView = view.findViewById(R.id.item_power);
                typeTextView = view.findViewById(R.id.type);

                itemView = view;
            }
        }

        LoadoutAdapter(JSONArray dataArray) {
            try {
                this.loadoutData = dataArray;
            } catch (Exception e) {
                Log.d("Weapons Recycler", e.toString());
            }
        }

        @Override
        @NonNull
        public LoadoutAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.character_equipped_layout, viewGroup, false);
            return new LoadoutAdapter.ViewHolder(view);

        }

        private void deleteItem(final int itemPosition) {
            try {
                final JSONObject removedItem = loadoutItems.getJSONObject(itemPosition);
                loadoutItems.put(itemPosition, new JSONObject());
                loadoutObject.put("items", loadoutItems);
                loadoutAdapter.notifyDataSetChanged();
                saveLoadout(position);

                View view = context.findViewById(R.id.loadout_layout);
                Snackbar snackbar = Snackbar.make(view, "Item removed from loadout", Snackbar.LENGTH_LONG);
                snackbar.setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        undoDelete(itemPosition, removedItem);
                    }
                });
                snackbar.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Toast.makeText(getApplicationContext(), "Item Delete", Toast.LENGTH_SHORT).show();
        }
        private void undoDelete (int itemPosition, JSONObject removedItem) {
            try {
                loadoutItems.put(itemPosition, removedItem);
                loadoutObject.put("items", loadoutItems);
                loadoutAdapter.notifyDataSetChanged();
                saveLoadout(position);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final LoadoutAdapter.ViewHolder viewHolder, int position) {
            try {
                viewHolder.imageView.setImageBitmap(bitmapArrayList.get(position));

                String itemHash = loadoutData.getJSONObject(position).optString("itemHash");
                String itemInstanceId = loadoutData.getJSONObject(position).optString("itemInstanceId");

                if (!itemHash.isEmpty()) {
                    String signedItemHash = ActivityMain.context.getSignedHash(itemHash);
                    JSONObject itemDefinition = new JSONObject(ActivityMain.context.defineElement(signedItemHash, "DestinyInventoryItemDefinition"));
                    String icon = itemDefinition.getJSONObject("displayProperties").getString("icon");
                    String name = itemDefinition.getJSONObject("displayProperties").getString("name");
                    String itemTypeDisplayName = itemDefinition.getString("itemTypeDisplayName");
                    int ammoType = itemDefinition.getJSONObject("equippingBlock").optInt("ammoType", -1);

                    viewHolder.titleTextView.setText(name);
                    viewHolder.typeTextView.setText(itemTypeDisplayName);
                    if (ammoType > -1) viewHolder.ammoIcon.setImageBitmap(getAmmoIcon(ammoType));

                    new LoadImages(viewHolder.imageView).execute(icon);
                }
                if (!itemInstanceId.isEmpty()) {
                    JSONObject instance = character.getItemInstances().getJSONObject(itemInstanceId);
                    Log.d("Instances", instance.toString());
                    String power = instance.getJSONObject("primaryStat").getString("value");
                    String damageTypeHash = instance.optString("damageTypeHash");

                    viewHolder.powerTextView.setText(power);
                    if (!damageTypeHash.isEmpty()) {
                        String signedDamageTypeHash = ActivityMain.context.getSignedHash(damageTypeHash);
                        JSONObject damageTypeDefinition = new JSONObject(ActivityMain.context.defineElement(signedDamageTypeHash, "DestinyDamageTypeDefinition"));
                        String elementIcon = damageTypeDefinition.getJSONObject("displayProperties").getString("icon");
                        new LoadImages(viewHolder.elementImageView).execute(elementIcon);
                    }
                    //String elementIcon = item.optString("elementIcon");
                    //String itemPower = item.optString("itemPower");

                    viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            try {
                                //need whole instance object to continue

                                String itemInstanceId = loadoutData.getJSONObject(viewHolder.getAdapterPosition()).getString("itemInstanceId");
                                Intent intent = new Intent(ActivityCharacter.context, ActivityItem.class);
                                intent.putExtra("instanceString", itemInstanceId);
                                ActivityCharacter.context.startActivity(intent);
                            } catch (Exception e) {
                                Log.d("Long Click Item", e.toString());
                            }
                            return true;
                        }
                    });
                }

/*
                JSONObject item = weaponsRecyclerData.getJSONObject(viewHolder.getAdapterPosition());
                final String itemHash = item.getString("itemHash");
                final String itemInstanceId = item.getString("itemInstanceId");
                String name = item.getString("name");
                String elementIcon = item.optString("elementIcon");
                String itemPower = item.optString("itemPower");
                int state = item.optInt("state");
                int ammoType = item.optInt("ammoType");
                String type = item.getString("type");

                Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.missing_icon_d2);
                viewHolder.imageView.setImageBitmap(defaultBitmap);
                viewHolder.titleTextView.setText(name);
                viewHolder.powerTextView.setText(itemPower);
                viewHolder.typeTextView.setText(type);

                if (state == 4) {
                    viewHolder.inventoryItemBackground.setColorFilter(Color.parseColor("#a0f5c242"));
                } else {
                    viewHolder.inventoryItemBackground.setColorFilter(null);
                }
                if (!elementIcon.isEmpty()) {
                    new LoadImages(viewHolder.elementImageView).execute(elementIcon);
                } else {
                    viewHolder.elementImageView.setImageBitmap(null);
                }
                if (item.has("ammoType") && ammoType != 0) {
                    viewHolder.ammoIcon.setImageBitmap(getAmmoIcon(ammoType));
                } else {
                    viewHolder.ammoIcon.setImageBitmap(null);
                }
                new LoadImages(viewHolder.imageView).execute(icon);
*/
                viewHolder.swapImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<String> itemCategories = new ArrayList<>();

                        switch (viewHolder.getAdapterPosition()) {
                            case 0: //kinetic
                                itemCategories.add("2");
                                break;
                            case 1: //energy
                                itemCategories.add("3");
                                break;
                            //warlock armor 21
                            //titan armor 22
                            //hunter armor 23
                            case 2: //power
                                itemCategories.add("4");
                                break;
                            case 3: //hat 45
                                try {
                                    JSONObject characterData = character.getCharacters();
                                    int classType = characterData.getJSONObject("data").getJSONObject(ActivityCharacter.context.characterId).getInt("classType");
                                    itemCategories.add(armorTypes.get(classType));
                                    itemCategories.add("45");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 4: //arms 46
                                try {
                                    JSONObject characterData = character.getCharacters();
                                    int classType = characterData.getJSONObject("data").getJSONObject(ActivityCharacter.context.characterId).getInt("classType");
                                    itemCategories.add(armorTypes.get(classType));
                                    itemCategories.add("46");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 5: //chest 47
                                try {
                                    JSONObject characterData = character.getCharacters();
                                    int classType = characterData.getJSONObject("data").getJSONObject(ActivityCharacter.context.characterId).getInt("classType");
                                    itemCategories.add(armorTypes.get(classType));
                                    itemCategories.add("47");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 6: //legs 48
                                try {
                                    JSONObject characterData = character.getCharacters();
                                    int classType = characterData.getJSONObject("data").getJSONObject(ActivityCharacter.context.characterId).getInt("classType");
                                    itemCategories.add(armorTypes.get(classType));
                                    itemCategories.add("48");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 7: //class item 49
                                try {
                                    JSONObject characterData = character.getCharacters();
                                    int classType = characterData.getJSONObject("data").getJSONObject(ActivityCharacter.context.characterId).getInt("classType");
                                    itemCategories.add(armorTypes.get(classType));
                                    itemCategories.add("49");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                        }

                        selectItem(itemCategories, viewHolder.getAdapterPosition());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return 8;
        }
    }

    private void selectItem (List<String> categories, final int itemPosition) {
        //JSONObject itemData = new JSONObject();

        //todo change the input requirement. maybe define filters from the whole list of assets
        DialogItemSelect dialogItemSelect = new DialogItemSelect(ActivityLoadout.this, categories);
        dialogItemSelect.setReturnSelection(new DialogItemSelect.ReturnSelection() {
            @Override
            public void selectObject(JSONObject itemData) {
                try {
                    loadoutItems.put(itemPosition, itemData);
                    loadoutAdapter.notifyDataSetChanged();

                    loadoutObject.put("items", loadoutItems);
                    saveLoadout(position);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        dialogItemSelect.show();
        Window window = dialogItemSelect.getWindow();
        if (window != null) window.setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);

    }

    public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

        private LoadoutAdapter mAdapter;

        private SwipeToDeleteCallback(LoadoutAdapter adapter) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mAdapter = adapter;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            mAdapter.deleteItem(position);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
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

    private void saveLoadout (int position) {
        try {
            String characterId = ActivityCharacter.context.characterId;
            String savedLoadouts = sharedPreferences.getString(characterId, null);
            JSONArray loadoutsArray = new JSONArray(savedLoadouts);
            loadoutsArray.put(position, loadoutObject);
            editor.putString(characterId, loadoutsArray.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getAmmoIcon (int ammoType) {
        switch (ammoType){
            case 1:
                return icon_primary;
            case 2:
                return icon_special;
            case 3:
                return icon_heavy;
        }
        return null;
    }
}
