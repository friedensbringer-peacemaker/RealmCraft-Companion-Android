package io.github.realmcraft.companion;

import android.content.Context;
import android.graphics.Color;
import org.json.*;
import java.io.*;
import java.util.*;

final class WorldCatalog {
    private final JSONObject names, colors;
    WorldCatalog(Context context) throws IOException,JSONException {names=load(context,"item_names.json");colors=load(context,"block_colors.json");}
    private static JSONObject load(Context context,String asset) throws IOException,JSONException {
        try(InputStream in=context.getAssets().open(asset);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new JSONObject(out.toString("UTF-8"));
        }
    }
    String name(int id,boolean german) {JSONObject row=names.optJSONObject(Integer.toString(id));return row==null?"ID "+id:row.optString(german?"de":"en","ID "+id);}
    int color(int id) {try{return Color.parseColor(colors.optString(Integer.toString(id),"#db5bc0"));}catch(IllegalArgumentException e){return 0xffdb5bc0;}}
}
