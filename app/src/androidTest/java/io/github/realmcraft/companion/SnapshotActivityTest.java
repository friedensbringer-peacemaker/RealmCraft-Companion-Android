package io.github.realmcraft.companion;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.TextView;
import io.github.realmcraft.companion.core.SnapshotStore;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("deprecation")
public final class SnapshotActivityTest extends InstrumentationTestCase {
    public void testMapPlayerInventoryAndReopen() throws Exception {
        Context context=getInstrumentation().getTargetContext();
        SnapshotStore.Snapshot sample=new SnapshotStore(new File(context.getFilesDir(),"snapshots")).createDemo();
        Intent intent=new Intent().setClassName(context.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity=getInstrumentation().startActivitySync(intent);
        try {
            await(()->activity.findViewById(SnapshotActivity.MAP_VIEW)!=null);
            assertTrue(activity.findViewById(SnapshotActivity.MAP_VIEW) instanceof SurfaceMapView);
            capture(activity,"qa-map.png");
            getInstrumentation().runOnMainSync(()->findButton(activity.getWindow().getDecorView(),"Weiter ›","Next ›").performClick());
            await(()->contains(activity.findViewById(SnapshotActivity.CONTENT),"X 2"));
            getInstrumentation().runOnMainSync(()->findButton(activity.getWindow().getDecorView(),"Punktdetails","Point details").performClick());
            getInstrumentation().waitForIdleSync();getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            getInstrumentation().runOnMainSync(()->findButton(activity.getWindow().getDecorView(),"Einstellungen …","Settings…").performClick());
            getInstrumentation().waitForIdleSync();getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);

            getInstrumentation().runOnMainSync(()->activity.findViewById(SnapshotActivity.PLAYER_TAB).performClick());
            await(()->contains(activity.findViewById(SnapshotActivity.CONTENT),"Level 7"));capture(activity,"qa-player.png");
            getInstrumentation().runOnMainSync(()->activity.findViewById(SnapshotActivity.INVENTORY_TAB).performClick());
            await(()->contains(activity.findViewById(SnapshotActivity.CONTENT),"× 12"));capture(activity,"qa-inventory.png");
            getInstrumentation().runOnMainSync(()->activity.findViewById(SnapshotActivity.DETAILS_TAB).performClick());
            await(()->contains(activity.findViewById(SnapshotActivity.CONTENT),"12345"));
        } finally {getInstrumentation().runOnMainSync(activity::finish);}
        Activity reopened=getInstrumentation().startActivitySync(intent);
        try{await(()->reopened.findViewById(SnapshotActivity.MAP_VIEW)!=null);}finally{getInstrumentation().runOnMainSync(reopened::finish);}
    }
    public void testRenderPreferencesAndTextures()throws Exception {
        Context c=getInstrumentation().getTargetContext();c.getSharedPreferences("map-settings",0).edit().putInt("chunks",2).putInt("ceiling",30).putBoolean("textures",true).commit();
        SnapshotStore.Snapshot sample=new SnapshotStore(new File(c.getFilesDir(),"snapshots")).createDemo();
        Intent intent=new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity a=getInstrumentation().startActivitySync(intent);
        try {await(()->contains(a.findViewById(SnapshotActivity.CONTENT),"2 / 3"));assertTrue(contains(a.findViewById(SnapshotActivity.CONTENT),"30"));capture(a,"qa-textures.png");}
        finally{getInstrumentation().runOnMainSync(a::finish);c.getSharedPreferences("map-settings",0).edit().clear().commit();}
    }
    public void testUnknownSnapshotFailsWithoutCrash()throws Exception{
        Context c=getInstrumentation().getTargetContext();Intent intent=new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot","../outside").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity a=getInstrumentation().startActivitySync(intent);try{await(()->contains(a.getWindow().getDecorView(),"Invalid snapshot ID"));}finally{getInstrumentation().runOnMainSync(a::finish);}
    }
    static android.widget.Button findButton(View v,String... labels){
        if(v instanceof android.widget.Button)for(String label:labels)if(((TextView)v).getText().toString().equals(label))return (android.widget.Button)v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){android.widget.Button b=findButton(g.getChildAt(i),labels);if(b!=null)return b;}}return null;
    }
    interface Condition{boolean test();}
    void await(Condition condition)throws Exception{long end=System.currentTimeMillis()+20000;AtomicBoolean ok=new AtomicBoolean();do{getInstrumentation().runOnMainSync(()->ok.set(condition.test()));if(ok.get())return;Thread.sleep(50);}while(System.currentTimeMillis()<end);fail("Feature page did not become ready");}
    static boolean contains(View v,String text){if(v instanceof TextView && ((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(contains(g.getChildAt(i),text))return true;}return false;}
    void capture(Activity a,String name)throws Exception {
        getInstrumentation().runOnMainSync(()->{
            // Quest may hide its panel while the headset is not worn. Explicit measurement
            // lets this synthetic render test inspect the real view tree offscreen as well.
            View view=a.getWindow().getDecorView();
            int width=1280,height=800;
            view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            view.layout(0,0,width,height);
            ViewGroup body=a.findViewById(SnapshotActivity.CONTENT);
            assertTrue("Feature content must receive usable height",body.getHeight()>150);
            assertTrue("Feature page must be laid out",body.getChildAt(0).getHeight()>100);
            Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bitmap));
            try(OutputStream out=new FileOutputStream(new File(a.getFilesDir(),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
            catch(IOException e){throw new AssertionError(e);}finally{bitmap.recycle();}
        });
    }
}
