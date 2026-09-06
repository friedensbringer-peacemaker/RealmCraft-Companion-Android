package io.github.realmcraft.companion;

import android.content.Context;
import android.graphics.Color;
import org.json.*;
import java.io.*;
import java.util.*;

final class WorldCatalog {
    private final JSONObject names, colors, textures, pixel;
    private final Context context;private final Map<Integer,android.graphics.Bitmap> images=new HashMap<>();
    WorldCatalog(Context context) throws IOException,JSONException {this.context=context;names=load(context,"item_names.json");colors=load(context,"block_colors.json");textures=load(context,"kenney-map.json");pixel=load(context,"pixel-map.json");}
    private static JSONObject load(Context context,String asset) throws IOException,JSONException {
        try(InputStream in=context.getAssets().open(asset);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new JSONObject(out.toString("UTF-8"));
        }
    }
    android.graphics.Bitmap texture(int id) {
        if(images.containsKey(id))return images.get(id);
        String path=textures.optString(Integer.toString(id),"");android.graphics.Bitmap image=null;
        if(!path.isEmpty())try(InputStream in=context.getAssets().open("kenney/"+path)){
            android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inSampleSize=4;
            image=android.graphics.BitmapFactory.decodeStream(in,null,options);
        }catch(IOException ignored){}images.put(id,image);return image;
    }
    android.graphics.drawable.Drawable icon(int id){android.graphics.Bitmap bitmap=null;String path=context.getSharedPreferences("map-settings",0).getBoolean("pixelIcons",true)?pixel.optString(Integer.toString(id),""):"";if(!path.isEmpty())try(InputStream in=context.getAssets().open("pixel/"+path)){bitmap=android.graphics.BitmapFactory.decodeStream(in);}catch(IOException ignored){}if(bitmap==null)bitmap=texture(id);if(bitmap!=null&&bitmap.getHeight()>bitmap.getWidth())bitmap=android.graphics.Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getWidth());if(bitmap!=null){android.graphics.drawable.BitmapDrawable d=new android.graphics.drawable.BitmapDrawable(context.getResources(),bitmap);d.setFilterBitmap(false);return d;}return new android.graphics.drawable.Drawable(){final android.graphics.Paint p=new android.graphics.Paint(3);public void draw(android.graphics.Canvas c){android.graphics.Rect b=getBounds();p.setColor(0xff46645a);c.drawRoundRect(new android.graphics.RectF(b),8,8,p);p.setColor(0xffe5f5ee);p.setTextAlign(android.graphics.Paint.Align.CENTER);p.setTextSize(b.height()*.25f);c.drawText("ID",b.exactCenterX(),b.top+b.height()*.43f,p);c.drawText(Integer.toString(id),b.exactCenterX(),b.top+b.height()*.76f,p);}public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(android.graphics.ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}};}
    String name(int id,boolean german) {JSONObject row=names.optJSONObject(Integer.toString(id));return row==null?"ID "+id:row.optString(german?"de":"en","ID "+id);}
    int color(int id) {try{return Color.parseColor(colors.optString(Integer.toString(id),"#db5bc0"));}catch(IllegalArgumentException e){return 0xffdb5bc0;}}
}
