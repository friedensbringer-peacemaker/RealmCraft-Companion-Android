package io.github.realmcraft.companion;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import io.github.realmcraft.companion.core.ChunkSurface;
import java.util.*;

/** Small independent chunk bitmaps keep distant regions from allocating a giant canvas. */
public final class SurfaceMapView extends View {
    interface Selection {void selected(String text);}
    private static final class Tile {ChunkSurface chunk;Bitmap bitmap;Tile(ChunkSurface c,Bitmap b){chunk=c;bitmap=b;}}
    private final List<Tile> tiles=new ArrayList<>();private final Paint paint=new Paint();
    private final WorldCatalog catalog;private final Selection selection;private final boolean german;
    private final ScaleGestureDetector gestures;
    private double centerX,centerZ,scale=1;private float lastX,lastY,downX,downY;private boolean moved;
    private int dimension;private boolean ready;
    SurfaceMapView(Context context,List<ChunkSurface> chunks,WorldCatalog catalog,boolean german,Selection selection) {
        super(context);this.catalog=catalog;this.german=german;this.selection=selection;
        setContentDescription(german?"Weltkarte. Ziehen zum Verschieben, Plus und Minus zum Zoomen.":"World map. Drag to pan; plus and minus to zoom.");
        for(ChunkSurface c:chunks) {int[] pixels=new int[256];
            for(int i=0;i<256;i++) {int base=catalog.color(c.ids[i]);float light=0.72f+Math.max(0,c.heights[i])/512f;
                pixels[i]=c.heights[i]<0?0xff172b30:Color.rgb(Math.min(255,(int)(Color.red(base)*light)),Math.min(255,(int)(Color.green(base)*light)),Math.min(255,(int)(Color.blue(base)*light)));}
            tiles.add(new Tile(c,Bitmap.createBitmap(pixels,16,16,Bitmap.Config.ARGB_8888)));
        }
        gestures=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoom(d.getScaleFactor());moved=true;return true;}});
    }
    public void setDimension(int value){dimension=value;fit();}
    public void zoom(double factor){scale=Math.max(0.005,Math.min(48,scale*factor));invalidate();}
    public void fit(){
        double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
        for(Tile t:tiles)if(t.chunk.dimension==dimension){minX=Math.min(minX,t.chunk.x);minZ=Math.min(minZ,t.chunk.z);maxX=Math.max(maxX,(double)t.chunk.x+16);maxZ=Math.max(maxZ,(double)t.chunk.z+16);}
        if(Double.isFinite(minX)){centerX=(minX+maxX)/2;centerZ=(minZ+maxZ)/2;scale=Math.max(0.00000001,Math.min((getWidth()-32)/Math.max(16,maxX-minX),(getHeight()-32)/Math.max(16,maxZ-minZ)));}
        ready=true;invalidate();
    }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){fit();}
    @Override protected void onDraw(Canvas canvas){
        canvas.drawColor(0xff0d191e);if(!ready)fit();int shown=0;paint.setFilterBitmap(false);
        for(Tile t:tiles){ChunkSurface c=t.chunk;if(c.dimension!=dimension)continue;shown++;
            float x=(float)((c.x-centerX)*scale+getWidth()/2.0),z=(float)((c.z-centerZ)*scale+getHeight()/2.0),size=(float)(16*scale);
            if(x>getWidth()||z>getHeight()||x+size<0||z+size<0)continue;
            canvas.drawBitmap(t.bitmap,null,new RectF(x,z,x+size,z+size),paint);
        }
        paint.setColor(0xffe1efe8);paint.setTextSize(14*getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText(shown==0?(german?"Keine lesbaren Chunks in dieser Dimension":"No readable chunks in this dimension"):"N ↑  ·  X →  ·  Z ↓",16,28,paint);
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        gestures.onTouchEvent(e);
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:lastX=downX=e.getX();lastY=downY=e.getY();moved=false;getParent().requestDisallowInterceptTouchEvent(true);return true;
            case MotionEvent.ACTION_POINTER_DOWN:moved=true;return true;
            case MotionEvent.ACTION_MOVE:
                if(Math.hypot(e.getX()-downX,e.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())moved=true;
                if(!gestures.isInProgress()){centerX-=(e.getX()-lastX)/scale;centerZ-=(e.getY()-lastY)/scale;invalidate();}
                lastX=e.getX();lastY=e.getY();return true;
            case MotionEvent.ACTION_POINTER_UP:lastX=e.getX(e.getActionIndex()==0?1:0);lastY=e.getY(e.getActionIndex()==0?1:0);return true;
            case MotionEvent.ACTION_UP:if(!moved){performClick();select(e.getX(),e.getY());}getParent().requestDisallowInterceptTouchEvent(false);return true;
            case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private void select(float sx,float sy){
        double wx=Math.floor(centerX+(sx-getWidth()/2.0)/scale),wz=Math.floor(centerZ+(sy-getHeight()/2.0)/scale);
        for(Tile t:tiles){ChunkSurface c=t.chunk;if(c.dimension!=dimension||wx<c.x||wx>=(double)c.x+16||wz<c.z||wz>=(double)c.z+16)continue;
            int i=((int)(wz-c.z))*16+(int)(wx-c.x);
            selection.selected(String.format(Locale.ROOT,"X %.0f · Z %.0f · Y %d\n%s",wx,wz,c.heights[i],catalog.name(c.ids[i],german)));return;}
        selection.selected(german?"Hier sind keine Kartendaten gespeichert.":"No saved map data here.");
    }
}
