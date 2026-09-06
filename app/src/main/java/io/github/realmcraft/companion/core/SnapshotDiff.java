package io.github.realmcraft.companion.core;
import java.util.*;
/** Compares decoded evidence only; missing coverage must never imply removed terrain. */
public final class SnapshotDiff {
    public static final class ChestChange {public final MapPoint point;public final Map<Integer,Long> quantities=new TreeMap<>();public final boolean propertiesChanged;
        ChestChange(MapPoint old,MapPoint current){point=current;for(PlayerReader.Item i:old.items)quantities.merge(i.id,-(long)i.quantity,Long::sum);for(PlayerReader.Item i:current.items)quantities.merge(i.id,(long)i.quantity,Long::sum);quantities.values().removeIf(v->v==0);propertiesChanged=quantities.isEmpty();}}
    public final List<ChestChange> chestChanges=new ArrayList<>();
    public final Map<Integer,Long> inventory=new TreeMap<>();
    public int changedChests,unknownChests,addedChunks,missingChunks,changedColumns;
    public static SnapshotDiff between(SnapshotAnalysis before,SnapshotAnalysis after){
        SnapshotDiff d=new SnapshotDiff();
        if(before.player!=null&&after.player!=null){for(PlayerReader.Item i:before.player.inventory)d.inventory.merge(i.id,-(long)i.quantity,Long::sum);for(PlayerReader.Item i:after.player.inventory)d.inventory.merge(i.id,(long)i.quantity,Long::sum);d.inventory.values().removeIf(v->v==0);}
        Map<String,ChunkSurface> chunks=new HashMap<>();for(ChunkSurface c:before.chunks)chunks.put(key(c.dimension,c.x,0,c.z),c);
        for(ChunkSurface c:after.chunks){ChunkSurface old=chunks.remove(key(c.dimension,c.x,0,c.z));if(old==null)d.addedChunks++;else for(int i=0;i<256;i++)if(c.ids[i]!=old.ids[i]||c.heights[i]!=old.heights[i])d.changedColumns++;}d.missingChunks=chunks.size();
        Map<String,MapPoint> chests=new HashMap<>();for(MapPoint p:before.points)if(p.kind.equals("chest"))chests.put(key(p.dimension,p.x,p.y,p.z),p);
        for(MapPoint p:after.points)if(p.kind.equals("chest")){MapPoint old=chests.remove(key(p.dimension,p.x,p.y,p.z));if(old==null||!old.readable||!p.readable)d.unknownChests++;else if(!items(old).equals(items(p))){d.changedChests++;d.chestChanges.add(new ChestChange(old,p));}}d.unknownChests+=chests.size();return d;
    }
    private static Map<String,Long> items(MapPoint p){Map<String,Long> m=new TreeMap<>();for(PlayerReader.Item i:p.items)m.merge(i.id+":"+i.durability+":"+new TreeMap<>(i.enchantments),(long)i.quantity,Long::sum);return m;}
    private static String key(int d,int x,int y,int z){return d+":"+x+":"+y+":"+z;}
}
