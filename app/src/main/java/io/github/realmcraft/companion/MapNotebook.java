package io.github.realmcraft.companion;
import android.content.*;
import org.json.*;
import java.util.*;
/** Local user annotations, shared by snapshots with the same world identity. Never writes game files. */
final class MapNotebook {
    static final class Marker {String id,name;int x,y,z,dimension;boolean favorite;}
    private final SharedPreferences prefs;
    MapNotebook(Context context,String world){prefs=context.getSharedPreferences("notebook-"+world,0);}
    List<Marker> list(){List<Marker> result=new ArrayList<>();try{JSONArray a=new JSONArray(prefs.getString("markers","[]"));for(int i=0;i<Math.min(200,a.length());i++){JSONObject o=a.getJSONObject(i);Marker m=new Marker();m.id=o.getString("id");m.name=o.getString("name");m.x=o.getInt("x");m.y=o.getInt("y");m.z=o.getInt("z");m.dimension=o.getInt("d");m.favorite=o.optBoolean("favorite");result.add(m);}}catch(JSONException ignored){}result.sort(Comparator.comparing((Marker m)->!m.favorite).thenComparing(m->m.name));return result;}
    void save(Marker marker){List<Marker> values=list();values.removeIf(m->m.id.equals(marker.id));if(values.size()>=200)throw new IllegalStateException("Maximum 200 markers / Maximal 200 Markierungen");values.add(marker);write(values);}
    String exportData(){return prefs.getString("markers","[]");}
    void replaceData(String json){prefs.edit().putString("markers",json).apply();}
    void delete(String id){List<Marker> values=list();values.removeIf(m->m.id.equals(id));write(values);}
    private void write(List<Marker> values){JSONArray a=new JSONArray();try{for(Marker m:values)a.put(new JSONObject().put("id",m.id).put("name",m.name).put("x",m.x).put("y",m.y).put("z",m.z).put("d",m.dimension).put("favorite",m.favorite));}catch(JSONException e){throw new IllegalArgumentException(e);}prefs.edit().putString("markers",a.toString()).apply();}
}
