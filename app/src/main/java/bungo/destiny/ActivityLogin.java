package bungo.destiny;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static android.content.ContentValues.TAG;

public class ActivityLogin extends Activity{

    String X_API_KEY;
    String CLIENT_ID;
    String REQUEST_AUTH_URL;
    String REQUEST_TOKEN_URL;
    String REQUEST_URL;

    SharedPreferences sharedPreferences;

    JSONObject destinyAccountData = new JSONObject();

    public static ActivityLogin context;

    public static ThreadPoolExecutor threadPoolExecutor;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1000;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MILLISECONDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context = this;
    }

    @Override
    protected void onResume(){
        super.onResume();

        X_API_KEY = getResources().getString(R.string.x_api_key);
        CLIENT_ID = getResources().getString(R.string.client_id);
        REQUEST_AUTH_URL = getResources().getString(R.string.request_auth_url)
                + CLIENT_ID
                + "&response_type=code";
        REQUEST_TOKEN_URL = getResources().getString(R.string.request_token_url);
        REQUEST_URL = getResources().getString(R.string.request_url);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        threadPoolExecutor = new ThreadPoolExecutor(
                NUMBER_OF_CORES + 5,
                NUMBER_OF_CORES + 8,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                new LinkedBlockingDeque<Runnable>()
        );

        //handle response from Bungie.net api callback
        Uri data = this.getIntent().getData();
        if (data != null) {
            //Log.d("Data", data.getQueryParameter("state"));
            String authString = data.getQueryParameter("code");
            Log.d("Received from webIntent", authString);
            new handleAuthCode().execute(authString);

        } else {
            if (sharedPreferences.contains("access_token")) {
                if (isAuthExpired()) {
                    Log.d("Auth Error", "Expired Token");
                    requestAuth("login");
                } else {
                    String membershipId = sharedPreferences.getString("membership_id", null);
                    String[] params = new String[1];
                    params[0] = membershipId;
                    new getMembershipsById().execute(params);
                }
            } else {
                requestAuth("login");
            }
        }
    }

    private class handleAuthCode extends AsyncTask <String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground (String... params) {

            String auth_code = params[0];
            if (auth_code != null) {
                JSONObject tokenParams = new JSONObject();
                try {
                    tokenParams.put("post_params", "client_id=" + CLIENT_ID + "&grant_type=authorization_code&code=" + auth_code);
                    tokenParams.put("method", "POST");
                    tokenParams.put("x_api_key", X_API_KEY);
                    tokenParams.put("url", REQUEST_TOKEN_URL);

                    JSONObject tokenResponse = new HttpCall().httpExecute(tokenParams);

                    String access_token = tokenResponse.getString("access_token");
                    String token_type = tokenResponse.getString("token_type");
                    String expires_in = tokenResponse.getString("expires_in");
                    String membership_id = tokenResponse.getString("membership_id");

                    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityLogin.getContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("access_token", access_token);
                    editor.putString("token_type", token_type);
                    editor.putString("expires_in", expires_in);
                    editor.putString("membership_id", membership_id);
                    editor.putLong("acquired_time", System.currentTimeMillis());
                    editor.apply();

                    return tokenResponse;

                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            } else {
                Log.d("shit guys", "code was null");
            }
            return null;
        }

        @Override
        protected void onPostExecute (JSONObject tokenObject) {
            try {
                String[] params = new String[1];
                params[0] = tokenObject.getString("membership_id");
                new getMembershipsById().execute(params);
            } catch (Exception e) {
                Log.d("Error", "Unable to retrieve membership id");
            }
        }
    }

    public static Context getContext() {
        return context;
    }

    public boolean isAuthExpired() {
        Long currentTime = System.currentTimeMillis();
        Long acquiredTime = sharedPreferences.getLong("acquired_time", 0);
        Long expireLength = Long.valueOf(sharedPreferences.getString("expires_in", null));

        return ((currentTime - acquiredTime)/1000 > expireLength);
    }

    public void requestAuth(String state) {
        Log.d("requestAuth", "Retrieving auth token");
        String url = REQUEST_AUTH_URL;// + "&state=" + state;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    private class getMembershipsById extends AsyncTask<String, Void, JSONObject> {

        String bungieId;
        @Override
        protected JSONObject doInBackground (String... getMembershipParams){
            bungieId = getMembershipParams[0];
            try {
                JSONObject params = new JSONObject();
                params.put("method", "GET");
                params.put("url", REQUEST_URL + "User/GetMembershipsById/" + bungieId + "/-1/");
                params.put("x_api_key", X_API_KEY);

                Log.d("Access Token", sharedPreferences.getString("access_token", null));

                HttpCall httpCall = new HttpCall();
                return httpCall.httpExecute(params);


            } catch (Exception e) {
                Log.d("getMembershipError", e.toString());
            }
            return null;
        }

        protected void onPostExecute (JSONObject membership_data) {

            if (membership_data == null) {
                Log.d("Error", "Unable to retrieve membership data");
                Snackbar snackbar = Snackbar.make(context.findViewById(R.id.login_layout), "Error getting membership data", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Retry", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String[] params = new String[1];
                        params[0] = bungieId;
                        new getMembershipsById().execute(params);
                    }
                });
                snackbar.show();
                return;
            }

            Log.d("member data", membership_data.toString());
            destinyAccountData = membership_data;
            try {
                //check for database update here
                String locale = membership_data
                        .getJSONObject("Response")
                        .getJSONObject("bungieNetUser")
                        .getString("locale");

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("locale", locale);
                editor.apply();

                String[] manifestParams = new String[2];
                manifestParams[0] = REQUEST_URL;
                manifestParams[1] = X_API_KEY;
                new getManifest().execute(manifestParams);

            } catch (Exception e){
                Log.d("Manifest Check", e.toString());
            }
        }
    }

    public void startMainActivity (JSONObject destinyAccountData){
        TextView textView = findViewById(R.id.login_text);
        textView.setText(getResources().getText(R.string.start_main));
        Intent intent = new Intent(this, ActivityMain.class);
        intent.putExtra("memberData", destinyAccountData.toString());
        startActivity(intent);

    }

    private class getManifest extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected void onPreExecute () {
            TextView textView = findViewById(R.id.login_text);
            textView.setText(getResources().getText(R.string.get_manifest));
        }

        @Override
        protected JSONObject doInBackground (String... params) {
            String requestUrl = params[0];
            String xApiKey = params[1];
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("method", "GET");
                jsonObject.put("url", requestUrl + "Destiny2/Manifest/");
                jsonObject.put("x_api_key", xApiKey);

                HttpCall httpCall = new HttpCall();
                return httpCall.httpExecute(jsonObject);
            } catch (Exception e){
                Log.d("getManifest Error", e.toString());
                //new getManifest().execute(params);
            }
            Log.d("Manifest", "Returning null");
            return null;
        }

        @Override
        protected void onPostExecute (JSONObject manifest) {

            if (manifest == null) {
                Log.d("Error", "Unable to retrieve membership data");
                Snackbar snackbar = Snackbar.make(context.findViewById(R.id.login_layout), "Error retrieving manifest data", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAction("Retry", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String[] manifestParams = new String[2];
                        manifestParams[0] = REQUEST_URL;
                        manifestParams[1] = X_API_KEY;
                        new getManifest().execute(manifestParams);
                        new getMembershipsById().execute(manifestParams);
                    }
                });
                snackbar.show();
                return;
            }

            try {
                String localManifestDate = sharedPreferences.getString("ManifestDate", null);
                String localDatabaseFilename = sharedPreferences.getString("Database", null);
                String locale = sharedPreferences.getString("locale", null);

                String currentManifestDate;
                currentManifestDate = manifest
                        .getJSONObject("Response")
                        .getString("version");
                String mobileWorldContentPath = manifest
                        .getJSONObject("Response")
                        .getJSONObject("mobileWorldContentPaths")
                        .getString(locale);

                if (localManifestDate == null || !localManifestDate.equals(currentManifestDate)) {
                    if (localManifestDate != null && !localManifestDate.equals(currentManifestDate)){
                        Log.d("Local Manifest", "Deleting old database");
                        File definitionDatabase = new File(getContext().getDir("Files", Context.MODE_PRIVATE), localDatabaseFilename);
                        definitionDatabase.delete();
                    }

                    Log.d("Manifest", "Retrieving database");

                    String[] mobileWorldContentParams = new String[2];
                    mobileWorldContentParams[0] = mobileWorldContentPath;
                    mobileWorldContentParams[1] = currentManifestDate;

                    new CheckMobileWorldContent().execute(mobileWorldContentParams);

                } else {
                    //use existing database here ->> or not. but at least we'll have the current data set from Bungie
                    Log.d("Manifest", "Using existing database");
                    startMainActivity(destinyAccountData);
                }
            } catch (Exception e) {
                Log.d("Manifest Error", e.toString());
            }
        }
    }

    public class CheckMobileWorldContent extends AsyncTask<String, String, Void> {

        TextView textView;
        @Override
        protected void onPreExecute() {
            textView = ((Activity)context).findViewById(R.id.login_text);
            textView.setText(context.getResources().getText(R.string.get_database));
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            String uiUpdate = context.getResources().getText(R.string.get_database) + "  " + values[0];
            textView.setText(uiUpdate);

        }

        @Override
        protected Void doInBackground (String... params) {
            String contentPath = params[0];
            String contentDate = params[1];
            try {
                String filename = contentPath.substring(contentPath.lastIndexOf("/") + 1);
                //Log.d("Filename",filename);
                URL url = new URL("https://www.bungie.net" + contentPath);

                URLConnection ucon = url.openConnection();
                ucon.setReadTimeout(20000);
                ucon.setConnectTimeout(20000);

                InputStream is = ucon.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

                float fileSize = (float) ucon.getContentLength();
                File file = new File(getContext().getDir("Files", Context.MODE_PRIVATE) + filename);

                FileOutputStream outStream = new FileOutputStream(file);
                byte[] buff = new byte[5 * 1024];

                int len;

                while ((len = inStream.read(buff)) != -1) {
                    publishProgress(String.format("%.2f", ((float) file.length() / fileSize) * 100) + "%");
                    outStream.write(buff, 0, len);
                }

                outStream.flush();
                outStream.close();
                inStream.close();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("ManifestDate", contentDate);
                editor.putString("Database", filename);
                editor.apply();

                unzipDatabase(context, file);

            } catch (Exception e) {
                Log.d("FileStream Error", e.toString());
            }
            return null;
        }
    }

    public void unzipDatabase(Context context, File zipFile) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry ze;
        int count;
        byte[] buffer = new byte[8192];
        while ((ze = zis.getNextEntry()) != null) {
            File file = new File(getContext().getDir("Files", Context.MODE_PRIVATE), ze.getName());
            File dir = ze.isDirectory() ? file : file.getParentFile();
            if (!dir.isDirectory() && !dir.mkdirs())
                throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
            if (ze.isDirectory())
                continue;
            FileOutputStream fout = new FileOutputStream(file);
            while ((count = zis.read(buffer)) != -1)
                fout.write(buffer, 0, count);
            fout.close();

        }
        zis.close();
        Log.d("Database Util", "Unzipped");
        startMainActivity(destinyAccountData);
    }
}
