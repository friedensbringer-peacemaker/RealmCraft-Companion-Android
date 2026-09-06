package io.github.realmcraft.companion.core;

import java.io.IOException;
import java.util.*;

/** Process-local bounded cache. Call only after the source bytes pass checksum verification. */
final class SurfaceDecodeCache {
    private static final long LIMIT=8*1024*1024;
    private static final LinkedHashMap<String,ChunkSurface> entries=new LinkedHashMap<>(16,.75f,true);
    private static long used;static int hits;
    static synchronized ChunkSurface decode(byte[] bytes,String hash,String name,int ceiling)throws IOException{
        String key=hash+":"+name+":"+ceiling;ChunkSurface value=entries.get(key);
        if(value!=null){hits++;return copy(value);}
        value=ChunkSurface.decode(bytes,name,ceiling);long cost=cost(value);
        if(cost<=LIMIT){while(used+cost>LIMIT&&!entries.isEmpty()){String oldest=entries.keySet().iterator().next();used-=cost(entries.remove(oldest));}entries.put(key,value);used+=cost;}
        return copy(value);
    }
    private static long cost(ChunkSurface c){long size=4096;for(MapPoint p:c.points)size+=512L+p.text.length()*2L+p.items.size()*512L;return size;}
    private static ChunkSurface copy(ChunkSurface source){
        ChunkSurface c=new ChunkSurface(source.x,source.z,source.dimension);System.arraycopy(source.ids,0,c.ids,0,256);System.arraycopy(source.heights,0,c.heights,0,256);c.pointsLimited=source.pointsLimited;
        for(MapPoint p:source.points){MapPoint v=new MapPoint(p.x,p.y,p.z,p.dimension,p.blockId,p.kind);v.text=p.text;v.issue=p.issue;v.readable=p.readable;v.items=Collections.unmodifiableList(new ArrayList<>(p.items));c.points.add(v);}return c;
    }
    static synchronized void clear(){entries.clear();used=0;hits=0;}
}
