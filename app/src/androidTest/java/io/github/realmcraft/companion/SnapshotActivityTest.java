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
    @Override protected void setUp()throws Exception{super.setUp();getInstrumentation().getTargetContext().getSharedPreferences("map-world-42-12345",0).edit().clear().commit();}
    public void testNotebookReferenceComparisonAndPersistence()throws Exception {
        Context c=getInstrumentation().getTargetContext();SnapshotStore store=new SnapshotStore(new File(c.getFilesDir(),"snapshots"));SnapshotStore.Snapshot sample=store.createDemo();store.createDemo();
        Intent intent=new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SnapshotActivity a=(SnapshotActivity)getInstrumentation().startActivitySync(intent);
        try{await(()->a.findViewById(SnapshotActivity.MAP_VIEW)!=null);capture(a,"unused");
            getInstrumentation().runOnMainSync(()->{a.editMarker(null);((android.widget.EditText)a.featureDialog.findViewById(R.id.marker_name)).setText("Synthetic base");((android.widget.EditText)a.featureDialog.findViewById(R.id.coordinate_x)).setText("12");a.featureDialog.getButton(-1).performClick();});
            assertTrue(new MapNotebook(c,"42-12345").list().stream().anyMatch(m->m.name.equals("Synthetic base")&&m.x==12));
            getInstrumentation().runOnMainSync(()->{a.editReference();((android.widget.EditText)a.featureDialog.findViewById(R.id.coordinate_x)).setText("10");((android.widget.EditText)a.featureDialog.findViewById(R.id.coordinate_z)).setText("20");a.featureDialog.getButton(-1).performClick();});capture(a,"unused");
            assertEquals(10,c.getSharedPreferences("map-world-42-12345",0).getInt("refX",0));
            getInstrumentation().runOnMainSync(a::chooseComparison);await(()->a.featureDialog!=null&&a.featureDialog.isShowing()&&a.featureDialog.getListView()!=null);
            getInstrumentation().runOnMainSync(()->a.featureDialog.getListView().performItemClick(null,0,0));await(()->a.featureDialog!=null&&a.featureDialog.isShowing()&&contains(a.featureDialog.getWindow().getDecorView(),"0 / 0"));
            getInstrumentation().runOnMainSync(()->{a.featureDialog.dismiss();((SurfaceMapView)a.findViewById(SnapshotActivity.MAP_VIEW)).centerOn(12,20);});
        }finally{getInstrumentation().runOnMainSync(a::finish);}
        SnapshotActivity reopened=(SnapshotActivity)getInstrumentation().startActivitySync(intent);
        try{await(()->reopened.findViewById(SnapshotActivity.MAP_VIEW)!=null);capture(reopened,"unused");assertEquals(12,((SurfaceMapView)reopened.findViewById(SnapshotActivity.MAP_VIEW)).viewport()[0],0.001);assertEquals(View.GONE,reopened.findViewById(SnapshotActivity.MAP_TAB).getParent() instanceof View?((View)reopened.findViewById(SnapshotActivity.MAP_TAB).getParent()).getVisibility():-1);}
        finally{getInstrumentation().runOnMainSync(reopened::finish);c.getSharedPreferences("notebook-42-12345",0).edit().clear().commit();c.getSharedPreferences("map-world-42-12345",0).edit().clear().commit();}
    }
    public void testPackingAndBookmarkRestore()throws Exception {
        Context c=getInstrumentation().getTargetContext();SnapshotStore.Snapshot sample=new SnapshotStore(new File(c.getFilesDir(),"snapshots")).createDemo();
        SnapshotActivity a=(SnapshotActivity)getInstrumentation().startActivitySync(new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{await(()->a.findViewById(SnapshotActivity.MAP_VIEW)!=null);capture(a,"unused");
            getInstrumentation().runOnMainSync(()->{a.packingItem(3157,12);a.featureDialog.getButton(-1).performClick();});
            assertTrue(contains(a.featureDialog.getWindow().getDecorView(),"5 / 12"));assertTrue("Packing materials need resource icons",hasIcon(a.featureDialog.getWindow().getDecorView()));
            getInstrumentation().runOnMainSync(()->{a.featureDialog.dismiss();((SurfaceMapView)a.findViewById(SnapshotActivity.MAP_VIEW)).centerOn(18,22);a.saveBookmark();((android.widget.EditText)a.featureDialog.findViewById(R.id.bookmark_name)).setText("Synthetic view");a.featureDialog.getButton(-1).performClick();});
            getInstrumentation().runOnMainSync(()->{a.featureDialog.dismiss();((SurfaceMapView)a.findViewById(SnapshotActivity.MAP_VIEW)).centerOn(-15,-20);a.bookmarks();a.featureDialog.getListView().performItemClick(null,0,0);a.featureDialog.getListView().performItemClick(null,0,0);});
            await(()->a.findViewById(SnapshotActivity.MAP_VIEW)!=null);capture(a,"unused");assertEquals(18,((SurfaceMapView)a.findViewById(SnapshotActivity.MAP_VIEW)).viewport()[0],.001);
            ExplorationPlans plans=new ExplorationPlans(c.getSharedPreferences("map-world-42-12345",0));assertEquals(1,plans.read("packing").length());assertEquals(1,plans.read("bookmarks").length());plans.remove("packing","3157");assertEquals(0,plans.read("packing").length());
        }finally{getInstrumentation().runOnMainSync(a::finish);c.getSharedPreferences("map-world-42-12345",0).edit().clear().commit();}
    }
    public void testStreamingPanLoadsDistantChunksWithoutMovingCamera()throws Exception{
        Context c=getInstrumentation().getTargetContext();c.getSharedPreferences("map-world-43-34567",0).edit().clear().commit();SnapshotStore.Snapshot sample=new SnapshotStore(new File(c.getFilesDir(),"snapshots")).importZip(new ByteArrayInputStream(io.github.realmcraft.companion.core.SyntheticFeatureData.wideArchive()));
        SnapshotActivity a=(SnapshotActivity)getInstrumentation().startActivitySync(new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{await(()->a.findViewById(SnapshotActivity.MAP_VIEW)!=null);capture(a,"unused");SurfaceMapView map=a.findViewById(SnapshotActivity.MAP_VIEW);assertFalse(map.hasChunk(4000,0));getInstrumentation().runOnMainSync(()->map.centerOn(4000,0));await(()->map.hasChunk(4000,0));assertEquals(4000,map.viewport()[0],.001);getInstrumentation().runOnMainSync(()->{long time=android.os.SystemClock.uptimeMillis();float x=map.getWidth()/2f,y=map.getHeight()/2f,dx=(float)(800*map.viewport()[2]);MotionEvent down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,x,y,0),move=MotionEvent.obtain(time,time+20,MotionEvent.ACTION_MOVE,x+dx,y,0),up=MotionEvent.obtain(time,time+40,MotionEvent.ACTION_UP,x+dx,y,0);map.onTouchEvent(down);map.onTouchEvent(move);map.onTouchEvent(up);down.recycle();move.recycle();up.recycle();});await(()->map.hasChunk(3200,0));assertEquals(3200,map.viewport()[0],.001);
        }finally{getInstrumentation().runOnMainSync(a::finish);c.getSharedPreferences("map-world-43-34567",0).edit().clear().commit();}
    }
    public void testNotebookTransferRejectsWrongWorldAndInvalidTargets()throws Exception{
        org.json.JSONObject root=new org.json.JSONObject().put("format",1).put("world","42").put("seed","12345");for(String key:new String[]{"markers","bookmarks","packing","projects"})root.put(key,new org.json.JSONArray());assertNotNull(NotebookTransfer.validate(root.toString(),"42","12345"));try{NotebookTransfer.validate(root.toString(),"43","12345");fail();}catch(org.json.JSONException expected){}root.getJSONArray("packing").put(new org.json.JSONObject().put("id","3157").put("item",3157).put("needed",-1));try{NotebookTransfer.validate(root.toString(),"42","12345");fail();}catch(org.json.JSONException expected){}
    }
    public void testMapPlayerInventoryAndReopen() throws Exception {
        Context context=getInstrumentation().getTargetContext();
        SnapshotStore.Snapshot sample=new SnapshotStore(new File(context.getFilesDir(),"snapshots")).createDemo();
        Intent intent=new Intent().setClassName(context.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity=getInstrumentation().startActivitySync(intent);
        try {
            await(()->activity.findViewById(SnapshotActivity.MAP_VIEW)!=null);
            assertTrue(activity.findViewById(SnapshotActivity.MAP_VIEW) instanceof SurfaceMapView);
            capture(activity,"qa-map.png");
            getInstrumentation().runOnMainSync(()->findButton(activity.getWindow().getDecorView(),"›").performClick());
            SnapshotActivity snapshotActivity=(SnapshotActivity)activity;
            await(()->snapshotActivity.featureDialog!=null&&snapshotActivity.featureDialog.isShowing());
            assertTrue(contains(snapshotActivity.featureDialog.getWindow().getDecorView(),"Workshop"));
            getInstrumentation().runOnMainSync(()->snapshotActivity.featureDialog.dismiss());
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
    public void testLargeMapPreservesViewportAndBack()throws Exception {
        Context c=getInstrumentation().getTargetContext();
        SnapshotStore.Snapshot sample=new SnapshotStore(new File(c.getFilesDir(),"snapshots")).createDemo();
        Activity a=getInstrumentation().startActivitySync(new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            await(()->a.findViewById(SnapshotActivity.MAP_VIEW)!=null);capture(a,"qa-small.png");
            SurfaceMapView map=a.findViewById(SnapshotActivity.MAP_VIEW);
            assertTrue("Map must occupy at least 75 percent of the content",map.getHeight()>=a.findViewById(SnapshotActivity.CONTENT).getHeight()*.75);
            getInstrumentation().runOnMainSync(()->{map.centerOn(8,12);map.zoom(2);});double[] before=map.viewport();
            getInstrumentation().runOnMainSync(()->{a.findViewById(SnapshotActivity.PLAYER_TAB).performClick();a.findViewById(SnapshotActivity.MAP_TAB).performClick();});capture(a,"qa-return.png");
            SurfaceMapView restored=a.findViewById(SnapshotActivity.MAP_VIEW);
            assertEquals(before[0],restored.viewport()[0],0.0001);assertEquals(before[2],restored.viewport()[2],0.0001);
        }finally{getInstrumentation().runOnMainSync(a::finish);}
    }
    public void testPointAndChestSearchNavigatesToResult()throws Exception {
        Context c=getInstrumentation().getTargetContext();SnapshotStore.Snapshot sample=new SnapshotStore(new File(c.getFilesDir(),"snapshots")).createDemo();
        SnapshotActivity a=(SnapshotActivity)getInstrumentation().startActivitySync(new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{await(()->a.findViewById(SnapshotActivity.SEARCH_POINTS)!=null);
            getInstrumentation().runOnMainSync(()->{a.search(false);((android.widget.EditText)a.searchDialog.findViewById(SnapshotActivity.SEARCH_QUERY)).setText("workshop");});
            getInstrumentation().runOnMainSync(()->{android.widget.ListView list=a.searchDialog.findViewById(SnapshotActivity.SEARCH_RESULTS);assertEquals(1,list.getAdapter().getCount());a.searchDialog.dismiss();a.search(true);((android.widget.EditText)a.searchDialog.findViewById(SnapshotActivity.SEARCH_QUERY)).setText("3157");});
            getInstrumentation().runOnMainSync(()->{android.widget.ListView list=a.searchDialog.findViewById(SnapshotActivity.SEARCH_RESULTS);assertEquals(1,list.getAdapter().getCount());assertTrue(list.getAdapter().getItem(0).toString().contains("× 5"));list.performItemClick(null,0,0);});
            getInstrumentation().waitForIdleSync();getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);capture(a,"qa-search-result.png");
            assertEquals(4.5,((SurfaceMapView)a.findViewById(SnapshotActivity.MAP_VIEW)).viewport()[0],0.001);
        }finally{getInstrumentation().runOnMainSync(a::finish);}
    }
    public void testQuestPointTapAndIcons()throws Exception{
        Context c=getInstrumentation().getTargetContext();WorldCatalog catalog=new WorldCatalog(c);assertTrue(catalog.icon(3157) instanceof android.graphics.drawable.BitmapDrawable);assertTrue(catalog.icon(3047) instanceof android.graphics.drawable.BitmapDrawable);assertNotNull(catalog.icon(999999));
        SnapshotStore.Snapshot sample=new SnapshotStore(new File(c.getFilesDir(),"snapshots")).createDemo();SnapshotActivity a=(SnapshotActivity)getInstrumentation().startActivitySync(new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{await(()->a.findViewById(SnapshotActivity.MAP_VIEW)!=null);capture(a,"unused");SurfaceMapView map=a.findViewById(SnapshotActivity.MAP_VIEW);
            getInstrumentation().runOnMainSync(()->{map.centerOn(4,2);map.zoom(8);double[] v=map.viewport();float x=(float)(map.getWidth()/2+.5*v[2]),y=(float)(map.getHeight()/2+.5*v[2]);long time=android.os.SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(time,time,0,x,y,0),up=MotionEvent.obtain(time,time+20,1,x,y,0);map.onTouchEvent(down);map.onTouchEvent(up);down.recycle();up.recycle();});
            await(()->a.featureDialog!=null&&a.featureDialog.isShowing());assertTrue(contains(a.featureDialog.getWindow().getDecorView(),"× 5"));
            assertTrue("Chest detail must display an icon",hasIcon(a.featureDialog.getWindow().getDecorView()));getInstrumentation().runOnMainSync(()->a.featureDialog.dismiss());
            getInstrumentation().runOnMainSync(()->a.findViewById(SnapshotActivity.INVENTORY_TAB).performClick());assertTrue(hasIcon(a.findViewById(SnapshotActivity.CONTENT)));
        }finally{getInstrumentation().runOnMainSync(a::finish);}
    }
    private static boolean hasIcon(View v){if(v instanceof TextView)for(android.graphics.drawable.Drawable d:((TextView)v).getCompoundDrawables())if(d!=null)return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(hasIcon(((ViewGroup)v).getChildAt(i)))return true;return false;}
    public void testRenderPreferencesAndTextures()throws Exception {
        Context c=getInstrumentation().getTargetContext();c.getSharedPreferences("map-world-42-12345",0).edit().clear().putInt("chunks",2).putInt("ceiling",30).putBoolean("textures",true).commit();
        SnapshotStore.Snapshot sample=new SnapshotStore(new File(c.getFilesDir(),"snapshots")).createDemo();
        Intent intent=new Intent().setClassName(c.getPackageName(),SnapshotActivity.class.getName()).putExtra("snapshot",sample.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity a=getInstrumentation().startActivitySync(intent);
        try {await(()->contains(a.findViewById(SnapshotActivity.CONTENT),"2 / 3"));assertTrue(contains(a.findViewById(SnapshotActivity.CONTENT),"30"));capture(a,"qa-textures.png");}
        finally{getInstrumentation().runOnMainSync(a::finish);c.getSharedPreferences("map-world-42-12345",0).edit().clear().commit();}
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
            // Screenshot creation is paused; retain functional layout assertions only.
        });
    }
}
