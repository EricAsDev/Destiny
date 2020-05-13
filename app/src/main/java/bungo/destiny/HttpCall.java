package bungo.destiny;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpCall {

    public JSONObject httpExecute (JSONObject parameters) {
        try {
            String base_url = parameters.getString("url");
            String method = parameters.getString("method");
            String x_api_key = parameters.getString("x_api_key");
            String content_type = parameters.optString("content_type");

            URL url = new URL(base_url);
            Log.d("requesting url", url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            HttpURLConnection.setFollowRedirects(true);
            conn.setReadTimeout(20000);
            conn.setConnectTimeout(20000);
            conn.setRequestProperty("Accept-Charset", "UTF-8");

            if (method.equals("GET")) {
                if (content_type.isEmpty()) content_type = "application/json";
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", content_type);
                conn.setRequestProperty("x-api-key", x_api_key);
                if (parameters.has("access_token")) {
                    conn.setRequestProperty("Authorization", "Bearer " + parameters.getString("access_token"));
                }
                conn.setInstanceFollowRedirects(true);

            } else if (method.equals("POST")) {
                if (content_type.isEmpty()) content_type = "application/x-www-form-urlencoded";
                String post_params = parameters.getString("post_params");
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", content_type);
                if (parameters.has("x_api_key")) conn.setRequestProperty("x-api-key", x_api_key);
                if (parameters.has("access_token")) {
                    conn.setRequestProperty("Authorization", "Bearer " + parameters.getString("access_token"));
                } else {
                    Log.d("POST", "Token not found");
                }

                OutputStream outputStream = conn.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
                bufferedWriter.write(post_params);
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();
                //conn.connect();
            }

            int httpResponseCode = conn.getResponseCode();

            if (httpResponseCode == 301) {
                String newUrl = conn.getHeaderField("Location");
                conn.disconnect();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("method", "GET");
                    jsonObject.put("url", newUrl);
                    jsonObject.put("x_api_key", parameters.getString("x_api_key"));

                    this.httpExecute(jsonObject);
                } catch (Exception e) {
                    Log.d("Redirect Error", e.toString());
                }

            } else if (httpResponseCode == 307) {
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                Log.d("Response 307", result.toString());
                reader.close();
                is.close();
                conn.disconnect();

            } else if (httpResponseCode == 200) {
                StringBuilder result = new StringBuilder();
                JSONObject responseObject = new JSONObject();
                InputStream in = new BufferedInputStream(conn.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    try {
                        responseObject = new JSONObject(result.toString());

                    } catch (Throwable t) {
                        Log.e("Error", "Could not create JSON object");

                    }
                    reader.close();
                    in.close();
                    conn.disconnect();
                    return responseObject;

                } catch (Throwable t) {
                    Log.e("HttpCall", "Could not create JSON object");
                }

            } else if (httpResponseCode == 500){

                InputStream is = conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                JSONObject responseObject = new JSONObject();
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    try {
                        responseObject = new JSONObject(result.toString());

                    } catch (Throwable t) {
                        Log.e("Error", "Could not create JSON object");

                    }
                    reader.close();
                    is.close();
                    conn.disconnect();
                    return responseObject;

                } catch (Throwable t) {
                    Log.e("HttpCall", "Could not create JSON object");
                }


                /*
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                Log.d("Response 500", result.toString());
                reader.close();
                is.close();
                conn.disconnect();
                return responseObject;
                */

            } else {
                Log.d("Response Code " + httpResponseCode, conn.getResponseMessage());
            }


            /*




            response 503 crashes the app






             */
        } catch (SocketTimeoutException s) {
            Log.d("SocketTimeoutException", s.toString());
        } catch (Exception d) {
            Log.d("JSON Error", d.toString());
        }
        return null;
    }
}
