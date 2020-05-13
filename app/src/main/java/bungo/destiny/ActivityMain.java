package bungo.destiny;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.google.android.gms.common.util.IOUtils;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.content.ContentValues.TAG;

public class ActivityMain extends AppCompatActivity {

    String X_API_KEY;
    public String REQUEST_URL;
    String ACCESS_TOKEN;

    public static ThreadPoolExecutor threadPoolExecutor;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1000;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MILLISECONDS;

    public static SharedPreferences sharedPreferences;

    public static ActivityMain context;

    Vibrator vibrator;

    ViewPagerAdapter viewPagerAdapter;
    ViewPager viewPager;

    Handler handler;
    int PROFILE_LOADED = 100;
    int CHARACTER_LOADED = 200;
    //int VENDORS_LOADED = 400;
    int NODES_LOADED = 700;
    int RECORDS_LOADED = 900;
    int CLAN_LOADED = 1000;
    int FAILURE = 1001;

    Data data = new Data();
    Data.Profile profile = data.new Profile();
    Data.Character character = data.new Character();
    Data.Clan clan = data.new Clan();
    Data.Account account = data.new Account();
    Data.Records records = data.new Records();
    Data.PresentationNodes presentationNodes = data.new PresentationNodes();
    //Data.Vendors vendors = data.new Vendors();

    FragmentVault fragmentVaultSelect;
//    Data.Items items = data.new Items();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        final FragmentTriumphsSelect fragmentTriumphsSelect = new FragmentTriumphsSelect();
        final FragmentCharacterSelect fragmentCharacterSelect = new FragmentCharacterSelect();
        //final FragmentClan fragmentClanSelect = new FragmentClan();
        fragmentVaultSelect = new FragmentVault();

        viewPager = findViewById(R.id.main_viewPager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        viewPagerAdapter.addFragment(fragmentTriumphsSelect);
        viewPagerAdapter.addFragment(fragmentCharacterSelect);
        //viewPagerAdapter.addFragment(fragmentClanSelect);
        viewPagerAdapter.addFragment(fragmentVaultSelect);

        viewPagerAdapter.notifyDataSetChanged();
        viewPager.setCurrentItem(1);

        X_API_KEY = getResources().getString(R.string.x_api_key);
        REQUEST_URL = getResources().getString(R.string.request_url);

        SharedPreferences loginSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityLogin.getContext());
        ACCESS_TOKEN = loginSharedPreferences.getString("access_token", null);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        threadPoolExecutor = new ThreadPoolExecutor(
                NUMBER_OF_CORES + 5,
                NUMBER_OF_CORES + 8,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LinkedTransferQueue<Runnable>()
                //2x slower
                //new LinkedBlockingDeque<Runnable>()
        );

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                super.handleMessage(inputMessage);
                switch (inputMessage.what) {
                    case 100:
                        Log.d("Profile", "Data Loaded");
                        try {
                            Log.d("Profile Progression", profile.getProfileProgression()
                                    .getJSONObject("data")
                                    .getJSONObject("seasonalArtifact")
                                    .toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        fragmentVaultSelect.buildInventoryList();
                        break;
                    case 200:
                        Log.d("Character", "Data Loaded");
                        //gloryHole();
                        fragmentCharacterSelect.buildCharacterFragment();
                        break;
                    case 400:
                        Log.d("Records", "Data Loaded");
                        break;
                    case 700:
                        Log.d("Presentation Nodes", "Data Loaded");
                        //fragmentTriumphsSelect.buildTriumphsAndSeals();
                        fragmentTriumphsSelect.getRecordsData();
                        break;
                    case 900:
                        Log.d("Vendors", "Data Loaded");
                        break;
                    case 1000:
                        Log.d("Clan", "Data Loaded");
                        break;
                    case 1001:
                        Snackbar snackbar = Snackbar.make(ActivityMain.context.findViewById(R.id.main_layout), "Error getting clan data", Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                initiateData();
                            }
                        });
                        snackbar.show();
                        break;
                }
            }
        };

        Intent intent = getIntent();
        if (intent != null) {
            Toast.makeText( getApplicationContext(),"Welcome, Guardian",Toast.LENGTH_SHORT).show();

            try {
                JSONObject destinyAccountDataObject = new JSONObject(intent.getStringExtra("memberData"));
                account.setAccountData(destinyAccountDataObject);

                String membershipType = destinyAccountDataObject
                        .getJSONObject("Response")
                        .getJSONArray("destinyMemberships")
                        .getJSONObject(0)
                        .getString("membershipType");
                String membershipId = destinyAccountDataObject
                        .getJSONObject("Response")
                        .getJSONArray("destinyMemberships")
                        .getJSONObject(0)
                        .getString("membershipId");

                SharedPreferences.Editor editor = loginSharedPreferences.edit();
                editor.putString("membership_type", membershipType);
                editor.putString("destiny_membership_id", membershipId);
                editor.apply();
                final DestinyAPI destinyAPI = new DestinyAPI();
                initiateData();

                ActivityMain.threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject profileDataObject = destinyAPI.getProfile();

                        if (!isSystemMaintenance(profileDataObject)) {
                            profile.setProfileObjects(profileDataObject);
                            Message profileMessage = new Message();
                            profileMessage.what = PROFILE_LOADED;
                            handler.sendMessage(profileMessage);
                        }
                    }
                });

                ActivityMain.threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject presentationNodesObject = destinyAPI.getPresentationNodes();

                        if (!isSystemMaintenance(presentationNodesObject)) {
                            presentationNodes.setProfilePresentationNodes(presentationNodesObject);
                            Message nodesMessage = new Message();
                            nodesMessage.what = NODES_LOADED;
                            handler.sendMessage(nodesMessage);
                        }
                    }
                });

                ActivityMain.threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject charactersDataObject = destinyAPI.getCharacter();

                        if (!isSystemMaintenance(charactersDataObject)) {
                            character.setCharacterObjects(charactersDataObject);
                            Message characterMessage = new Message();
                            characterMessage.what = CHARACTER_LOADED;
                            handler.sendMessage(characterMessage);
                        }
                    }
                });

                ActivityMain.threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject recordsDataObject = destinyAPI.getRecords();

                        if (!isSystemMaintenance(recordsDataObject)) {
                            records.setRecordsObjects(recordsDataObject);
                            Message recordsMessage = new Message();
                            recordsMessage.what = RECORDS_LOADED;
                            handler.sendMessage(recordsMessage);
                        }
                    }
                });

                ActivityMain.threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject transitoryDataObject = destinyAPI.getTransitory();
                        Log.d("T Data?", transitoryDataObject.toString());
                        //vendors.setVendorData(vendorDataObject);
                    }
                });

            } catch (Exception e){
                Log.d("Error", e.toString());
            }
        }



        //database tests

    }
    @Override
    public void onBackPressed () {
        if (viewPager.getCurrentItem() != 1) {
            viewPager.setCurrentItem(1);
        } else {
            finishAffinity();
            System.exit(0);
        }
    }

    public void initiateData () {
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final GroupV2API groupV2API = new GroupV2API();
                final JSONObject clanData = groupV2API.getClan();

                if (!isSystemMaintenance(clanData)) {
                    boolean clanSet = clan.setClanData(clanData);
                    if (clanSet) {
                        Message clanMessage = new Message();
                        clanMessage.what = CLAN_LOADED;
                        handler.sendMessage(clanMessage);
                    } else {
                        Message clanMessage = new Message();
                        clanMessage.what = FAILURE;
                        handler.sendMessage(clanMessage);
                    }
                }
            }
        });
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

    public class DownloadImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            String url_string = params[0];

            Bitmap bitmap = null;
            try {
                InputStream in = new URL(url_string).openStream();
                bitmap = BitmapFactory.decodeStream(in);
                in.close();
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }
    }

    public void storeImage (Bitmap image, String filename) {

        Log.d("Storing", filename);
        File pictureFile = new File(ActivityMain.context.getDir("Files", Context.MODE_PRIVATE), filename);
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
            Log.d("File", "Successfully stored");
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    public Bitmap getImage (String filename) {
        File image = new File(ActivityMain.context.getDir("Files", Context.MODE_PRIVATE), filename);
        if (image.exists()) {
            return BitmapFactory.decodeFile(image.getAbsolutePath());

        }
        return null;
    }

    public String defineElement (String hash, String definitionType) {
        String filename = sharedPreferences.getString("Database", null);
        if (filename!=null) {
            File file = new File(ActivityLogin.getContext().getDir("Files", Context.MODE_PRIVATE), filename);
            SQLiteDatabase sqLiteDatabase = openOrCreateDatabase(file.toString(), MODE_PRIVATE, null);
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + definitionType + " WHERE id = " + hash, null);
            cursor.moveToFirst();
            if (cursor.getCount() == 0) {
                Log.d("Database", "item not found " + hash);
                return null;
            }
            String data = cursor.getString(1);
            sqLiteDatabase.close();
            cursor.close();
            return data;
        }
        return null;
    }

    public String getSignedHash (String hashId) {
        Long longNumber = Long.parseLong(hashId);
        String binString = String.format("%32s", Long.toBinaryString(longNumber)).replace(' ', '0');
        long signedHash;
        if (binString.charAt(0) == '1') {
            StringBuilder onesComplementBuilder = new StringBuilder();
            for (char bit : binString.toCharArray()) {
                onesComplementBuilder.append((bit == '0') ? 1 : 0);
            }
            String onesComplement = onesComplementBuilder.toString();
            signedHash = (Long.parseLong(onesComplement, 2) + 1) * -1;
        } else {
            signedHash = (Long.parseLong(binString, 2));
        }
        return String.valueOf(signedHash);
    }

    private boolean isSystemMaintenance (JSONObject response) {
        try {
            if (response.getInt("ErrorCode") == 5) {
                Snackbar snackbar = Snackbar.make(ActivityMain.context.findViewById(R.id.main_layout),
                        response.getString("ErrorStatus") + ": " + response.getString("Message"), Snackbar.LENGTH_INDEFINITE);
                snackbar.show();
                Log.d(response.getString("ErrorStatus"), response.getString("Message"));
                return true;
            }

        } catch (Exception e) {
            Log.d("Response Error", e.toString());
        }
        return false;
    }
    public String getSymbol (String code) {
        String result = "";
        String DATABASE_NAME = "DestinyData.db";
        AssetManager assetManager = getApplicationContext().getAssets();

        try {
            File file = File.createTempFile(DATABASE_NAME,"db");
            InputStream inputStream = assetManager.open(DATABASE_NAME);
            OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copyStream(inputStream, outputStream);

            SQLiteDatabase sqLiteDatabase = openOrCreateDatabase(file.toString(), MODE_PRIVATE, null);
            //String symbolType = "Sentinal";
            String table = "DestinySymbols";
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + table + " WHERE Description = '" + code +"'", null);
            cursor.moveToFirst();
            result = cursor.getString(3);
            sqLiteDatabase.close();
            cursor.close();

        } catch (Exception e) {
            Log.d("ASSET", e.toString());
        }
        return result;
    }
}
/*
    private static String formatString(String text){

        StringBuilder json = new StringBuilder();
        String indentString = "";

        for (int i = 0; i < text.length(); i++) {
            char letter = text.charAt(i);
            switch (letter) {
                case '{':
                case '[':
                    json.append("\n" + indentString + letter + "\n");
                    indentString = indentString + "\t";
                    json.append(indentString);
                    break;
                case '}':
                case ']':
                    indentString = indentString.replaceFirst("\t", "");
                    json.append("\n" + indentString + letter);
                    break;
                case ',':
                    json.append(letter + "\n" + indentString);
                    break;

                default:
                    json.append(letter);
                    break;
            }
        }

        return json.toString();
    }

    private class inspectLine extends AsyncTask <String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String string = params[0];
            String firstCharacter = string.substring(0,1);
            switch (firstCharacter) {
                case "{":
                    //Log.d("inspectLine", "begin object");
                    break;
                case "}":
                    //Log.d("inspectLine", "end object");
                    break;
                default:
                    try {
                        int count = string.length() - string.replace("\"", "").length();
                        string = string.replace(",", "");
                        if (count == 2) {
                            String[] strings = string.split(":");
                            if (strings.length == 2) {
                                String key = strings[0].substring(1, strings[0].lastIndexOf("\""));
                                String value = strings[1];
                                Log.d(key, value);
                            } else {
                                Log.d("String", "Object key found");
                            }
                        } else if (count == 4){
                            String[] strings = string.split(":");
                            String key = strings[0].substring(1, strings[0].lastIndexOf("\""));
                            String value = strings[1].substring(1, strings[1].lastIndexOf("\""));
                            Log.d(key, value);
                        }
                    } catch (Exception e){
                        Log.d("JSON Error", e.toString());
                    }
            }
            return null;
        }
    }
*/

/*
        mainActivityTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        //float x = event.getX() + mainActivityTextView.getScrollX();
                        float y = event.getY() + mainActivityTextView.getScrollY();
                        int line = mainActivityTextView.getLayout().getLineForVertical((int) y);
                        int start = mainActivityTextView.getLayout().getLineStart(line);
                        int end = mainActivityTextView.getLayout().getLineEnd(line);
                        String string = mainActivityTextView.getText().toString().substring(start, end);
                        String[] params = new String[1];
                        params[0] = string.trim();

                        new inspectLine().execute(params);
                }


                return false;
            }
        });
*/