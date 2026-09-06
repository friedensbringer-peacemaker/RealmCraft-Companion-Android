package io.github.realmcraft.companion;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import io.github.realmcraft.companion.core.ChunkSurface;
import io.github.realmcraft.companion.core.MapPoint;
import java.util.*;

/** Small independent chunk bitmaps keep distant regions from allocating a giant canvas. */
public final class SurfaceMapView extends View {
    interface Selection {void selected(String text);}
    private static final class Tile {ChunkSurface chunk;Tile(ChunkSurface c){chunk=c;}}
    private final android.util.LruCache<Tile,Bitmap> bitmaps=new android.util.LruCache<>(4096);
    private final List<Tile> tiles=new ArrayList<>();private final Paint paint=new Paint();
    private final WorldCatalog catalog;private final Selection selection;private final boolean german;
    private final ScaleGestureDetector gestures;
    private double centerX,centerZ,scale=1;private float lastX,lastY,downX,downY;private boolean moved;
    private int dimension;private boolean ready;private Runnable viewportChanged;
    void onViewportChanged(Runnable callback){viewportChanged=callback;}
    private void movedViewport(){if(viewportChanged!=null)viewportChanged.run();}
    boolean hasChunk(int x,int z){for(Tile t:tiles)if(t.chunk.x==x&&t.chunk.z==z&&t.chunk.dimension==dimension)return true;return false;}
    void replaceChunks(List<ChunkSurface> chunks){tiles.clear();bitmaps.evictAll();for(ChunkSurface c:chunks)tiles.add(new Tile(c));invalidate();}
    private Map<String,boolean[]> differences=Collections.emptyMap();
    void setDifferences(Map<String,boolean[]> values){differences=values;invalidate();}
    private boolean textures;private List<MapPoint> points=Collections.emptyList();private MapPoint selected;
    private List<MapNotebook.Marker> annotations=Collections.emptyList();private double[] reference;
    void setAnnotations(List<MapNotebook.Marker> values,double[] ref){annotations=values;reference=ref;invalidate();}
    interface PointSelection {void selected(MapPoint p);}private PointSelection pointSelection;
    public void setTextures(boolean enabled){textures=enabled;invalidate();}
    void setPoints(List<MapPoint> points,PointSelection callback){this.points=points;pointSelection=callback;invalidate();}
    public void focus(MapPoint point){ready=true;selected=point;centerX=point.x+0.5;centerZ=point.z+0.5;scale=Math.max(scale,4);invalidate();movedViewport();}
    SurfaceMapView(Context context,List<ChunkSurface> chunks,WorldCatalog catalog,boolean german,Selection selection) {
        super(context);this.catalog=catalog;this.german=german;this.selection=selection;
        setContentDescription(german?"Weltkarte. Ziehen zum Verschieben, Plus und Minus zum Zoomen.":"World map. Drag to pan; plus and minus to zoom.");
        for(ChunkSurface c:chunks)tiles.add(new Tile(c));
        gestures=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoomAt(d.getScaleFactor(),d.getFocusX(),d.getFocusY());moved=true;return true;}});
    }
    private Bitmap bitmap(Tile t){
        Bitmap cached=bitmaps.get(t);if(cached!=null)return cached;int[] pixels=new int[256];
        for(int i=0;i<256;i++){int base=catalog.color(t.chunk.ids[i]);float light=0.72f+Math.max(0,t.chunk.heights[i])/512f;
            pixels[i]=t.chunk.heights[i]<0?0xff172b30:Color.rgb(Math.min(255,(int)(Color.red(base)*light)),Math.min(255,(int)(Color.green(base)*light)),Math.min(255,(int)(Color.blue(base)*light)));}
        cached=Bitmap.createBitmap(pixels,16,16,Bitmap.Config.ARGB_8888);bitmaps.put(t,cached);return cached;
    }
    public void setDimension(int value){dimension=value;fit();}
    double[] viewport(){return ready?new double[]{centerX,centerZ,scale}:null;}
    void restoreViewport(double[] v){if(v!=null&&v.length==3&&Double.isFinite(v[0])&&Double.isFinite(v[1])&&Double.isFinite(v[2])&&v[2]>0){centerX=v[0];centerZ=v[1];scale=v[2];ready=true;invalidate();}}
    public void centerOn(int x,int z){centerX=x;centerZ=z;scale=Math.max(scale,4);ready=true;invalidate();movedViewport();}
    public void zoom(double factor){zoomAt(factor,getWidth()/2f,getHeight()/2f);}
    private void zoomAt(double factor,float x,float y){double before=scale;scale=Math.max(0.00000001,Math.min(48,scale*factor));centerX+=(x-getWidth()/2.0)*(1/before-1/scale);centerZ+=(y-getHeight()/2.0)*(1/before-1/scale);invalidate();movedViewport();}
    public void fit(){
        if(getWidth()<=32||getHeight()<=32){ready=false;return;}
        double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
        for(Tile t:tiles)if(t.chunk.dimension==dimension){minX=Math.min(minX,t.chunk.x);minZ=Math.min(minZ,t.chunk.z);maxX=Math.max(maxX,(double)t.chunk.x+16);maxZ=Math.max(maxZ,(double)t.chunk.z+16);}
        if(Double.isFinite(minX)){centerX=(minX+maxX)/2;centerZ=(minZ+maxZ)/2;scale=Math.max(0.00000001,Math.min((getWidth()-32)/Math.max(16,maxX-minX),(getHeight()-32)/Math.max(16,maxZ-minZ)));}
        ready=true;invalidate();movedViewport();
    }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){if(!ready)fit();movedViewport();}
    @Override protected void onDraw(Canvas canvas){
        canvas.drawColor(0xff0d191e);if(!ready)fit();int shown=0;paint.setFilterBitmap(false);
        for(Tile t:tiles){ChunkSurface c=t.chunk;if(c.dimension!=dimension)continue;shown++;
            float x=(float)((c.x-centerX)*scale+getWidth()/2.0),z=(float)((c.z-centerZ)*scale+getHeight()/2.0),size=(float)(16*scale);
            if(x>getWidth()||z>getHeight()||x+size<0||z+size<0)continue;
            canvas.drawBitmap(bitmap(t),null,new RectF(x,z,x+size,z+size),paint);
            if(textures&&scale>=8)for(int row=0;row<16;row++)for(int col=0;col<16;col++){
                float bx=x+(float)(col*scale),bz=z+(float)(row*scale),bs=(float)scale;
                if(bx>getWidth()||bz>getHeight()||bx+bs<0||bz+bs<0)continue;
                Bitmap texture=catalog.texture(c.ids[row*16+col]);if(texture!=null)canvas.drawBitmap(texture,null,new RectF(bx,bz,bx+bs,bz+bs),paint);
            }
        }
        paint.setColor(0x99ff9b35);for(Tile tile:tiles){ChunkSurface c=tile.chunk;if(c.dimension!=dimension)continue;boolean[] changed=differences.get(c.dimension+":"+c.x+":"+c.z);if(changed==null)continue;for(int i=0;i<256;i++)if(changed[i]){float px=(float)((c.x+i%16-centerX)*scale+getWidth()/2.0),pz=(float)((c.z+i/16-centerZ)*scale+getHeight()/2.0);canvas.drawRect(px,pz,px+(float)scale,pz+(float)scale,paint);}}
        Set<Long> occupied=new HashSet<>();
        for(MapPoint point:points){if(point.dimension!=dimension)continue;
            float px=(float)((point.x+0.5-centerX)*scale+getWidth()/2.0),pz=(float)((point.z+0.5-centerZ)*scale+getHeight()/2.0);
            if(px<0||pz<0||px>getWidth()||pz>getHeight())continue;
            long cell=((long)(px/22)<<32)|(int)(pz/22);if(point!=selected&&!occupied.add(cell))continue;
            paint.setColor(point==selected?0xffffffff:0xfff3bf55);canvas.drawCircle(px,pz,point==selected?11:8,paint);
            paint.setColor(0xff182720);paint.setTextSize(12);String mark=point.kind.equals("sign")?"S":point.kind.equals("chest")?(german?"T":"C"):point.kind.equals("bed")?"B":"W";
            canvas.drawText(mark,px-4,pz+4,paint);
        }
        paint.setTextSize(16*getResources().getDisplayMetrics().scaledDensity);
        for(MapNotebook.Marker m:annotations)if(m.dimension==dimension){float px=(float)((m.x-centerX)*scale+getWidth()/2.0),pz=(float)((m.z-centerZ)*scale+getHeight()/2.0);if(px<0||pz<0||px>getWidth()||pz>getHeight())continue;paint.setColor(0xff75d9ff);canvas.drawCircle(px,pz,7,paint);canvas.drawText((m.favorite?"★ ":"")+m.name,px+10,pz,paint);}
        if(reference!=null&&reference[3]==dimension){float px=(float)((reference[0]-centerX)*scale+getWidth()/2.0),pz=(float)((reference[2]-centerZ)*scale+getHeight()/2.0);paint.setColor(0xffff88dd);canvas.drawLine(px-10,pz,px+10,pz,paint);canvas.drawLine(px,pz-10,px,pz+10,paint);canvas.drawText(german?"Manuelle Referenz":"Manual reference",px+12,pz+18,paint);}
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
                if(!gestures.isInProgress()){centerX-=(e.getX()-lastX)/scale;centerZ-=(e.getY()-lastY)/scale;invalidate();movedViewport();}
                lastX=e.getX();lastY=e.getY();return true;
            case MotionEvent.ACTION_POINTER_UP:lastX=e.getX(e.getActionIndex()==0?1:0);lastY=e.getY(e.getActionIndex()==0?1:0);return true;
            case MotionEvent.ACTION_UP:if(!moved){performClick();select(e.getX(),e.getY());}getParent().requestDisallowInterceptTouchEvent(false);return true;
            case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private void select(float sx,float sy){
        MapPoint nearest=null;double distance=22*22;
        for(MapPoint point:points)if(point.dimension==dimension){double px=(point.x+0.5-centerX)*scale+getWidth()/2.0,pz=(point.z+0.5-centerZ)*scale+getHeight()/2.0,d=(px-sx)*(px-sx)+(pz-sy)*(pz-sy);if(d<distance){distance=d;nearest=point;}}
        if(nearest!=null&&pointSelection!=null){selected=nearest;pointSelection.selected(nearest);invalidate();return;}
        double wx=Math.floor(centerX+(sx-getWidth()/2.0)/scale),wz=Math.floor(centerZ+(sy-getHeight()/2.0)/scale);
        for(Tile t:tiles){ChunkSurface c=t.chunk;if(c.dimension!=dimension||wx<c.x||wx>=(double)c.x+16||wz<c.z||wz>=(double)c.z+16)continue;
            int i=((int)(wz-c.z))*16+(int)(wx-c.x);
            selection.selected(String.format(Locale.ROOT,"X %.0f · Z %.0f · Y %d\n%s",wx,wz,c.heights[i],catalog.name(c.ids[i],german)));return;}
        selection.selected(german?"Hier sind keine Kartendaten gespeichert.":"No saved map data here.");
    }
}
