package io.github.realmcraft.companion;
import android.content.SharedPreferences;
import org.json.*;
/** Small local per-world lists. No game writes or shared exports. */
final class ExplorationPlans {
    private final SharedPreferences prefs;
    ExplorationPlans(SharedPreferences prefs){this.prefs=prefs;}
    JSONArray read(String key){try{return new JSONArray(prefs.getString(key,"[]"));}catch(JSONException e){return new JSONArray();}}
    void put(String key,JSONObject value,int limit)throws JSONException{
        JSONArray list=read(key),next=new JSONArray();String id=value.getString("id");
        for(int i=0;i<list.length();i++)if(!list.getJSONObject(i).getString("id").equals(id))next.put(list.getJSONObject(i));
        if(next.length()>=limit)throw new JSONException("List limit reached / Listenlimit erreicht");next.put(value);prefs.edit().putString(key,next.toString()).apply();
    }
    void remove(String key,String id)throws JSONException{JSONArray list=read(key),next=new JSONArray();for(int i=0;i<list.length();i++)if(!list.getJSONObject(i).getString("id").equals(id))next.put(list.getJSONObject(i));prefs.edit().putString(key,next.toString()).apply();}
}
