package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class FragmentLoadouts extends Fragment {

    private RecyclerView loadoutsRecycler;
    private RecyclerView.Adapter loadoutsAdapter;

    private SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityLogin.getContext());
    private SharedPreferences.Editor editor = sharedPreferences.edit();

    private JSONArray loadouts;
    String characterId;
    String membershipId;

    private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

    public FragmentLoadouts() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        characterId = ActivityCharacter.context.characterId;
        membershipId = sharedPreferences.getString("destiny_membership_id", null);

        Log.d(membershipId, characterId); //4611686018431570103: 2305843009267660573

        final DatabaseReference databaseReference = firebaseDatabase.getReference(membershipId).child(characterId);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(membershipId)) {
                    Log.d("Firebase", membershipId + " membership not found");
                    databaseReference.setValue(membershipId);
                    databaseReference.child(membershipId).setValue(characterId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    Gson gson = new Gson();
                    JSONObject jsonObject = new JSONObject(gson.toJson(dataSnapshot.getValue()));
                    Log.d("Event", jsonObject.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        //databaseReference.setValue(characterId);
        //editor.remove(characterId);
        //editor.apply();
        try {
            if (!sharedPreferences.contains(characterId)) {
                editor.putString(characterId, new JSONArray().toString());
                editor.apply();

                loadouts = new JSONArray();
            } else {
                String loadoutsString = sharedPreferences.getString(characterId, null);
                loadouts = new JSONArray(loadoutsString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        try {
            String loadoutsString = sharedPreferences.getString(characterId, null);
            loadouts = new JSONArray(loadoutsString);
            loadoutsAdapter = new LoadoutAdapter(loadouts);
            loadoutsRecycler.setAdapter(loadoutsAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_loadouts, container, false);
        FloatingActionButton fab = layout.findViewById(R.id.new_item_fab);
        loadoutsRecycler = layout.findViewById(R.id.loadout_recycler);

        RecyclerView.LayoutManager loadoutLayoutManager = new LinearLayoutManager(ActivityCharacter.context, RecyclerView.VERTICAL, false);
        loadoutsRecycler.setLayoutManager(loadoutLayoutManager);
        loadoutsAdapter = new LoadoutAdapter(loadouts);
        loadoutsRecycler.setAdapter(loadoutsAdapter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONArray jsonArray = new JSONArray();
                    for (int i = 0; i < 8; i++) {
                        jsonArray.put(i,new JSONObject());
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("title", "");
                    jsonObject.put("items", jsonArray);

                    int loadoutPosition = loadouts.length();
                    loadouts.put(loadoutPosition, jsonObject);
                    editor.putString(characterId, loadouts.toString());
                    editor.apply();

                    Intent intent = new Intent(getContext(), ActivityLoadout.class);
                    intent.putExtra("loadoutData", loadoutPosition);
                    startActivity(intent);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return layout;
    }

    public class LoadoutAdapter extends RecyclerView.Adapter<LoadoutAdapter.ViewHolder> {

        JSONArray loadoutsArray;

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            ImageView configureImageView;
            ImageView equipImageView;
            View view;
            private ViewHolder(final View view) {
                super(view);
                titleTextView = view.findViewById(R.id.loadout_name);
                configureImageView = view.findViewById(R.id.loadout_configure);
                equipImageView = view.findViewById(R.id.loadout_equip);
                this.view = view;
            }
        }

        LoadoutAdapter(JSONArray data) {
            this.loadoutsArray = data;
        }

        @Override
        @NonNull
        public LoadoutAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int viewType) {
            View view = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.loadout_row, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
            try {
                String title = loadoutsArray.getJSONObject(position).optString("title");
                if (title.isEmpty()) {
                    title = "<unnamed loadout>";
                }
                viewHolder.titleTextView.setText(title);
            } catch (Exception e) {
                e.printStackTrace();
            }
            viewHolder.equipImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        ActivityMain.threadPoolExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONArray loadoutItems = loadoutsArray.getJSONObject(viewHolder.getAdapterPosition()).getJSONArray("items");
                                    ArrayList<String> itemsList = new ArrayList<>();
                                    for (int i = 0; i < loadoutItems.length(); i++) {
                                        if (loadoutItems.getJSONObject(i).has("itemInstanceId")) {
                                            itemsList.add(loadoutItems.getJSONObject(i).getString("itemInstanceId"));
                                        }
                                    }
                                    String[] params = new String[2];
                                    params[0] = itemsList.toString();
                                    params[1] = characterId;
                                    JSONObject apiRequest = new DestinyAPI().equipItems(params);
                                    Log.d("Equip", apiRequest.toString());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                }
            });
            viewHolder.configureImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), ActivityLoadout.class);
                    int loadoutPosition = viewHolder.getAdapterPosition();
                    intent.putExtra("loadoutData", loadoutPosition);
                    startActivity(intent);
                }
            });
            viewHolder.view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //TODO add confirm delete dialog box
                    Vibrator vibrator = (Vibrator) ActivityCharacter.context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(200);
                    }
                    loadouts.remove(viewHolder.getAdapterPosition());
                    editor.putString(characterId, loadouts.toString());
                    editor.apply();

                    loadoutsAdapter = new LoadoutAdapter(loadouts);
                    loadoutsRecycler.setAdapter(loadoutsAdapter);

                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return loadoutsArray.length();
        }
    }
}
