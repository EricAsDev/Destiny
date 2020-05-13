package bungo.destiny;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONObject;

public class GroupV2API {
    private SharedPreferences loginSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityLogin.getContext());

    private String membershipId = loginSharedPreferences.getString("destiny_membership_id", null);
    private String membershipType = loginSharedPreferences.getString("membership_type", null);
    private String requestUrl = ActivityMain.context.REQUEST_URL;
    private String xApiKey = ActivityMain.context.X_API_KEY;
    private String accessToken = ActivityMain.context.ACCESS_TOKEN;

    public JSONObject getClan () {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "GroupV2/User/"
                    + membershipType
                    + "/"
                    + membershipId
                    + "/0/1/");
            jsonObject.put("x_api_key", xApiKey);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("getClan Error", e.toString());
        }

        return null;
    }
}
