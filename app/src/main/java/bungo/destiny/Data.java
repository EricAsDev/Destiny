package bungo.destiny;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

class Data {

    class Account {
        private JSONObject accountData = new JSONObject();

        void setAccountData (JSONObject accountDataObject) {
            try {
                this.accountData = accountDataObject;
            } catch (Exception e) {
                Log.d("Data.Account error", e.toString());
            }
        }

        JSONObject getAccountData () {return this.accountData;}
    }

    //100 Level
    class Profile {
        private JSONObject profile = new JSONObject();
        private JSONObject profileCurrencies = new JSONObject();
        private JSONObject profileInventory = new JSONObject();
        private JSONObject profileProgression = new JSONObject(); //checklists and seasonalArtifact
        private JSONObject profileVendorReceipts = new JSONObject();
        private JSONObject itemComponents = new JSONObject();

        void setProfileObjects (JSONObject profileData) {
            try {
                this.profile = profileData.getJSONObject("Response").optJSONObject("profile"); //100
                this.profileVendorReceipts = profileData.getJSONObject("Response").optJSONObject("vendorReceipts"); //101
                this.profileInventory = profileData.getJSONObject("Response").optJSONObject("profileInventory"); //102
                this.profileCurrencies = profileData.getJSONObject("Response").optJSONObject("profileCurrencies"); //103
                this.profileProgression = profileData.getJSONObject("Response").optJSONObject("profileProgression"); //104
                this.itemComponents = profileData.getJSONObject("Response").optJSONObject("itemComponents"); //300

            }catch (Exception e) {
                Log.d("Data.Profile error", e.toString());
            }
        }

        void setProfileInventory (JSONObject profileInventory) {
            this.profileInventory = profileInventory;
        }

        JSONObject getProfileCurrencies () { return this.profileCurrencies; }
        JSONObject getProfile () { return this.profile; }
        JSONObject getProfileVendorReceipts () {return this.profileVendorReceipts; }
        JSONObject getProfileInventory () { return this.profileInventory; }
        JSONObject getProfileProgression () { return this.profileProgression; }
        JSONObject getItemComponents () { return this.itemComponents; }

    }

    //200 Level
    class Character {
        private JSONObject characters;
        private JSONObject characterInventories;
        private JSONObject characterProgressions;
        private JSONObject characterActivities;
        private JSONObject characterEquipment;

        private JSONObject itemObjectives;
        private JSONObject itemInstances;


                void setCharacters (JSONObject data) {
                    try {
                        this.characters = data.getJSONObject("Response").optJSONObject("characters");
                    } catch (Exception e) {
                        Log.d("Set Character", e.toString());
                    }
                }

                void setCharacterInventories (JSONObject data) {
                    try {
                        this.characterInventories = data.getJSONObject("Response").getJSONObject("characterInventories");
                        this.characterEquipment = data.getJSONObject("Response").getJSONObject("characterEquipment");
                        this.itemInstances = data.getJSONObject("Response").getJSONObject("itemComponents").getJSONObject("instances").getJSONObject("data");

                    } catch (Exception e) {
                        Log.d("setCharacterInv", e.toString());
                    }
                }

        void setCharacterObjects (JSONObject charactersDataObject) {
            try {
                this.characters = charactersDataObject.getJSONObject("Response").optJSONObject("characters"); //200
                this.characterInventories = charactersDataObject.getJSONObject("Response").optJSONObject("characterInventories"); //201
                this.characterProgressions = charactersDataObject.getJSONObject("Response").optJSONObject("characterProgressions"); //202
                this.characterActivities = charactersDataObject.getJSONObject("Response").optJSONObject("characterActivities"); //204
                this.characterEquipment = charactersDataObject.getJSONObject("Response").optJSONObject("characterEquipment"); //205

                this.itemInstances = charactersDataObject.getJSONObject("Response").getJSONObject("itemComponents").getJSONObject("instances").getJSONObject("data"); //300
                this.itemObjectives = charactersDataObject.getJSONObject("Response").getJSONObject("itemComponents").getJSONObject("objectives").getJSONObject("data"); //301

            } catch (Exception e) {
                Log.d("Data.Character error", e.toString());
            }
        }

        void setCharacterProgressions (JSONObject characterProgressions) {
            this.characterProgressions = characterProgressions;
        }

        JSONObject getCharacters () {
            return this.characters;
        }

        JSONObject getCharacterInventories () {
            return this.characterInventories;
        }

        JSONObject getCharacterProgressions () {
            return this.characterProgressions;
        }

        JSONObject getCharacterActivities () {
            return this.characterActivities;
        }

        JSONObject getCharacterEquipment () {
            return this.characterEquipment;
        }

        JSONObject getItemObjectives () { return this.itemObjectives; }

        JSONObject getItemInstances () { return this.itemInstances; }

    }

    //400 Level
    class Vendors {
        private JSONObject vendor = new JSONObject();
        private JSONObject vendorSales = new JSONObject();
        private JSONObject vendorGroups = new JSONObject();

        boolean setVendorData (JSONObject vendorDataObject) {
            if (vendorDataObject == null) { return false; }
            try {
                this.vendor = vendorDataObject.getJSONObject("Response").getJSONObject("vendors");
                this.vendorGroups = vendorDataObject.getJSONObject("Response").getJSONObject("vendorGroups");
                //Log.d("Data.Vendors", "Loaded");
            } catch (Exception e) {
                Log.d("Data.Vendor error", e.toString());
            }
            return true;
        }

        JSONObject getVendorData () {return this.vendor;}
        JSONObject getVendorSales () {return this.vendorSales;}
        JSONObject getVendorGroups () { return this.vendorGroups;}
    }

    //700 Level
    class PresentationNodes{
        private JSONObject profilePresentationNodes = new JSONObject();

        void setProfilePresentationNodes (JSONObject presentationNodesData) {
            try {
                this.profilePresentationNodes = presentationNodesData.getJSONObject("Response")
                        .getJSONObject("profilePresentationNodes")
                        .getJSONObject("data")
                        .getJSONObject("nodes");
            } catch (Exception e) {
                Log.d("Data.PresentationNodes", e.toString());
            }
        }

        JSONObject getProfilePresentationNodes () {return this.profilePresentationNodes;}
    }

    //900 Level
    class Records {
        private JSONObject records = new JSONObject();

        void setRecordsObjects (JSONObject recordData) {
            try {
                this.records = recordData.getJSONObject("Response").getJSONObject("profileRecords").getJSONObject("data");
            }catch (Exception e) {
                Log.d("Data.Records error", e.toString());
            }
        }

        JSONObject getRecords () {
            return this.records;
        }

    }

    class Clan {
        private JSONObject clanData = new JSONObject();

        boolean setClanData (JSONObject clanDataObject) {
            if (clanDataObject == null) { return false; }
            try {
                this.clanData = clanDataObject.getJSONObject("Response");
                //Log.d("Data.Clan", "Loaded");
            } catch (Exception e) {
                Log.d("Data.Clan error", e.toString());
            }
            return true;
        }

        JSONObject getClanData () {return this.clanData;}
    }

}
