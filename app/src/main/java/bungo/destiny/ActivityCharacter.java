package bungo.destiny;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

//import android.os.Handler;
import android.util.Log;
import android.view.Window;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActivityCharacter extends AppCompatActivity {

    public static ActivityCharacter context;

    String characterId;
    public JSONArray aggregateEquipment;
    //Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_character);
        context = this;

        ActivityCharacter.ViewPagerAdapter viewPagerAdapter;
        ViewPager viewPager = findViewById(R.id.character_viewPager);
        viewPagerAdapter = new ActivityCharacter.ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(4);

        Intent receivedIntent = getIntent();
        characterId = receivedIntent.getStringExtra("characterId");
        Log.d("Selected Character", characterId);

        viewPagerAdapter.addFragment(new FragmentLoadouts());
        viewPagerAdapter.addFragment(new FragmentGearSelect());
        viewPagerAdapter.addFragment(new FragmentPursuits());
        viewPagerAdapter.addFragment(new FragmentPostmaster());
        viewPagerAdapter.addFragment(new FragmentInventory()); //fragment causes crash leaving character and re-entering
        viewPagerAdapter.notifyDataSetChanged();

        viewPager.setCurrentItem(1);

        ActivityMain.threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                setAggregateEquipment();
            }
        });
    }

    @Override
    public void onStart () {
        super.onStart();

    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();

        private ViewPagerAdapter (FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem (int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        private void addFragment(Fragment fragment) {
            mFragmentList.add(fragment);
        }

    }

    private void setAggregateEquipment () {
        aggregateEquipment = new JSONArray();
        try {
            JSONObject getProfileInventory = ActivityMain.context.profile.getProfileInventory();
            JSONObject getCharacterEquipment = ActivityMain.context.character.getCharacterEquipment();
            JSONObject getCharacterInventory = ActivityMain.context.character.getCharacterInventories();

            JSONArray characterEquipment;
            JSONArray characterInventory;
            JSONArray workingList = new JSONArray();
            JSONArray profileInventory = getProfileInventory.getJSONObject("data").getJSONArray("items");
            JSONArray characterIds = ActivityMain.context.profile.getProfile().getJSONObject("data").getJSONArray("characterIds");
            for (int i = 0; i < characterIds.length(); i++) {
                characterEquipment = getCharacterEquipment.getJSONObject("data").getJSONObject(characterIds.getString(i)).getJSONArray("items");
                characterInventory = getCharacterInventory.getJSONObject("data").getJSONObject(characterIds.getString(i)).getJSONArray("items");
                workingList.put(characterEquipment);
                workingList.put(characterInventory);
            }
            workingList.put(profileInventory);
            for (int i = 0; i < workingList.length(); i++) {
                JSONArray jsonArray = workingList.getJSONArray(i);
                for (int j = 0; j < jsonArray.length(); j++) {
                    aggregateEquipment.put(jsonArray.getJSONObject(j));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
