package bungo.destiny;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.util.IOUtils;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DatabaseResources extends Activity {

    Context context = ActivityLogin.getContext();
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
                ucon.setReadTimeout(5000);
                ucon.setConnectTimeout(10000);

                InputStream is = ucon.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

                float fileSize = (float) ucon.getContentLength();
                File file = new File(context.getDir("Files", Context.MODE_PRIVATE) + filename);

                FileOutputStream outStream = new FileOutputStream(file);
                byte[] buff = new byte[5 * 1024];

                int len;

                while ((len = inStream.read(buff)) != -1) {
                    //Log.d("Progress", String.valueOf(file.length()));
                    //Thread.sleep(20);
                    publishProgress(String.format("%.2f", ((float) file.length() / fileSize) * 100) + "%");
                    outStream.write(buff, 0, len);
                }
                //Log.d("getDatabase", filename + "Completed");

                outStream.flush();
                outStream.close();
                inStream.close();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("ManifestDate", contentDate);
                editor.putString("Database", filename);
                editor.apply();

                unzipDatabase(context, file);
                //return databaseUnzipped;

            } catch (Exception e) {
                Log.d("FileStream Error", e.toString());
            }
            //return false;
            return null;
        }

        //@Override
        protected void onPostExecute (JSONObject database) {

        }
    }

    public void unzipDatabase(Context context, File zipFile) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry ze;
        int count;
        byte[] buffer = new byte[8192];
        while ((ze = zis.getNextEntry()) != null) {
            File file = new File(context.getDir("Files", Context.MODE_PRIVATE), ze.getName());
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
    }

    public String defineElement (String hash, String definitionType) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String filename = sharedPreferences.getString("databaseFilename", null);
        if (filename!=null) {
            File file = new File(getApplicationContext().getDir("Files", Context.MODE_PRIVATE), filename);
            SQLiteDatabase sqLiteDatabase = openOrCreateDatabase(file.toString(), MODE_PRIVATE, null);
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM " + definitionType + " WHERE id = " + hash, null);
            cursor.moveToFirst();
            String data = cursor.getString(1);
            sqLiteDatabase.close();
            cursor.close();
            return data;
        }
        return null;
    }


}
