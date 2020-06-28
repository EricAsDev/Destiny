package bungo.destiny;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

class DestinyAPI {
    private SharedPreferences loginSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ActivityLogin.getContext());

    private String membershipId = loginSharedPreferences.getString("destiny_membership_id", null);
    private String membershipType = loginSharedPreferences.getString("membership_type", null);
    private String requestUrl = ActivityMain.context.REQUEST_URL;
    private String statsUrl = ActivityMain.context.getResources().getString(R.string.stats_url);
    private String xApiKey = ActivityMain.context.X_API_KEY;
    private String accessToken = ActivityMain.context.ACCESS_TOKEN;

    //update equipped items 205
    //update unequipped items 201
    //update item instances 300
    //update pursuits
    //update records
    //update vault inventory - Profile 102
    //update character information 200

    JSONObject initiateAWA () {
        try {

            JSONObject postParams = new JSONObject();
            postParams.put("type", 1);
            postParams.put("characterId", "2305843009267660573");
            postParams.put("membershipType", membershipType);
            postParams.put("affectedItemId", "6917529150524277605");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "POST");
            jsonObject.put("url", requestUrl + "Destiny2/Awa/Initialize/");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);
            jsonObject.put("post_params", postParams);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e) {
            Log.d("Vault Transfer", e.toString());
        }
        return null;

    }

    JSONObject getActivities (String... params) {
        // /Destiny2/{membershipType}/Account/{destinyMembershipId}/Character/{characterId}/Stats/Activities/
        String characterId = params[0];
        String count = params[1];
        String mode = params[2];
        String page = params[3];
        String url = requestUrl
                + "Destiny2/"
                + membershipType
                + "/Account/"
                + membershipId
                + "/Character/"
                + characterId
                + "/Stats/Activities/";

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", url
                    + "?count=" + count
                    + "&mode=" + mode
                    + "&page=" + page);

                    //\No count provides most recent 25 matches.
                    //todo set up selectable count and save to sharedpreferences in ActivityActivityList.java
                    //+ "?mode=" + mode
                    //+ "&page=" + page);
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    JSONObject getCharacter () {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components=200,201,202,204,205,300,301");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("getCharacterData Error", e.toString());
        }
        return null;
    }

    JSONObject getMetrics () {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components=1100");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("getMetrics Error", e.toString());
        }
        return null;
    }

    JSONObject getTransitory () {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components=1000");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("getTransitory Error", e.toString());
        }
        return null;
    }

    JSONObject getEntityDefinition (String... params) {
        ///Destiny2/Manifest/{entityType}/{hashIdentifier}/
        String entityType = params[0];
        String hashIdentifier = params[1];

        String url = requestUrl
                + "Destiny2/"
                + "Manifest/"
                + entityType
                + "/"
                + hashIdentifier
                + "/";

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", url);
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    JSONObject getItemInstance (String itemInstanceId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/Profile/"
                    + membershipId
                    + "/Item/"
                    + itemInstanceId
                    + "/?components=302,304,305");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);
        } catch (Exception e) {
            Log.d("Get Item Instance", e.toString());
        }
        return null;
    }

    JSONObject getPostGameCarnageReport (String activityId) {
        String url = statsUrl
                + "Destiny2/"
                + "Stats/"
                + "PostGameCarnageReport/"
                + activityId
                + "/";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", url);
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    JSONObject getPresentationNodes () {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components=700");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("getPres.Nodes Error", e.toString());
        }
        return null;
    }

    JSONObject getProfile () {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components=100,101,102,103,104,300");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("getProfile Error", e.toString());
        }
        return null;
    }

    JSONObject getRecords () {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components=900");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("getRecordsData Error", e.toString());
        }
        return null;
    }

    JSONObject getVendors () {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/Profile/"
                    + membershipId
                    + "/Character/2305843009267660573/Vendors/?components=400");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("getVendorsData Error", e.toString());
        }
        return null;
    }

    JSONObject transferToVault (String... params) {
        try {
            int membershipType = Integer.parseInt(params[0]);
            long characterId = Long.parseLong(params[1]);
            long itemId = Long.parseLong(params[2]);
            long itemReferenceHash = Long.parseLong(params[3]);
            int stackSize = Integer.parseInt(params[4]);
            boolean toVault = Boolean.parseBoolean(params[5]);

            JSONObject postParams = new JSONObject();
            postParams.put("itemId", itemId);
            postParams.put("characterId", characterId);
            postParams.put("membershipType", membershipType);
            postParams.put("itemReferenceHash", itemReferenceHash);
            postParams.put("stackSize", stackSize);
            postParams.put("transferToVault", toVault);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "POST");
            jsonObject.put("url", requestUrl + "Destiny2/Actions/Items/TransferItem/");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);
            jsonObject.put("post_params", postParams);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e) {
            Log.d("Vault Transfer", e.toString());
        }
        return null;
    }

    JSONObject equipItem(String... params) {
        try {
            String itemId = params[0];
            String characterId = params[1];

            JSONObject postParams = new JSONObject();
            postParams.put("itemId", itemId);
            postParams.put("characterId", characterId);
            postParams.put("membershipType", membershipType);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "POST");
            jsonObject.put("url", requestUrl + "Destiny2/Actions/Items/EquipItem/");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);
            jsonObject.put("post_params", postParams);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("Equip Item Error", e.toString());
        }
        return null;
    }

    JSONObject equipItems (String... params) {
        try {
            //String itemIds = params[0];
            JSONArray itemIds = new JSONArray(params[0]);
            String characterId = params[1];

            JSONObject postParams = new JSONObject();
            postParams.put("itemIds", itemIds);
            postParams.put("characterId", characterId);
            postParams.put("membershipType", membershipType);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "POST");
            jsonObject.put("content_type", "application/json");
            jsonObject.put("url", requestUrl + "Destiny2/Actions/Items/EquipItems/");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);
            jsonObject.put("post_params", postParams);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    JSONObject searchPlayer (String name) {
        String url = requestUrl
                + "Destiny2/SearchDestinyPlayer/"
                + "-1/"
                + name + "/";

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", url);
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    JSONObject simpleProfile (String membershipId) {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + "1"
                    + "/profile/"
                    + membershipId
                    + "/?components=100");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    JSONObject updateCharacter (String components) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components="
                    + components);
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("Update Character", e.toString());
        }
        return null;
    }

    JSONObject updateProfileInventory () {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components=102");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("Update Profile Inv", e.toString());
        }
        return null;
    }

    JSONObject updateUnequippedInventory () {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", requestUrl
                    + "Destiny2/"
                    + membershipType
                    + "/profile/"
                    + membershipId
                    + "/?components=201,205,300");
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e){
            Log.d("Update Unequipped", e.toString());
        }
        return null;
    }

    JSONObject getHistoricalStats () {

        String url = requestUrl
                + "Destiny2/"
                + membershipType
                + "/Account/"
                + membershipId
                + "/Stats/";

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("method", "GET");
            jsonObject.put("url", url);
            jsonObject.put("x_api_key", xApiKey);
            jsonObject.put("access_token", accessToken);

            HttpCall httpCall = new HttpCall();
            return httpCall.httpExecute(jsonObject);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
