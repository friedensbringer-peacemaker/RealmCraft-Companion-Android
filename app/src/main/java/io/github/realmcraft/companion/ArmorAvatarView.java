package io.github.realmcraft.companion;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import io.github.realmcraft.companion.core.PlayerReader;

final class ArmorAvatarView extends View {
    private final PlayerReader player;private final WorldCatalog catalog;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    ArmorAvatarView(Context c,PlayerReader p,WorldCatalog catalog){super(c);player=p;this.catalog=catalog;setContentDescription("Schematic equipped armor / Schematische angelegte Rüstung");}
    private int color(int slot){for(PlayerReader.Item item:player.armor)if(item.slot==slot){String name=catalog.name(item.id,false).toLowerCase(java.util.Locale.ROOT);
        if(name.contains("diamond"))return 0xff43cdd8;if(name.contains("gold"))return 0xffe8ba49;if(name.contains("iron"))return 0xffc1cedb;if(name.contains("leather"))return 0xff97633b;if(name.contains("chain"))return 0xff85959f;return 0xffb18ec9;}return 0xff687785;}
    private void block(Canvas c,float x,float y,float w,float h,int color){paint.setColor(color);c.drawRect(x,y,x+w,y+h,paint);paint.setColor(0x33ffffff);c.drawRect(x+3,y+3,x+w-3,y+7,paint);paint.setColor(0x55000000);c.drawRect(x+w-5,y+7,x+w,y+h,paint);}
    @Override protected void onDraw(Canvas c){float s=Math.min(getWidth()/220f,getHeight()/270f);c.save();c.translate((getWidth()-220*s)/2,(getHeight()-270*s)/2);c.scale(s,s);
        block(c,79,15,62,58,color(4));block(c,75,83,70,85,color(3));block(c,43,83,25,100,color(3));block(c,152,83,25,100,color(3));
        block(c,75,176,31,58,color(2));block(c,114,176,31,58,color(2));block(c,73,232,33,26,color(1));block(c,114,232,33,26,color(1));
        paint.setColor(0xff35414c);c.drawRect(87,44,133,63,paint);paint.setColor(0xffd3ede9);c.drawRect(93,49,101,55,paint);c.drawRect(120,49,128,55,paint);c.restore();}
}
