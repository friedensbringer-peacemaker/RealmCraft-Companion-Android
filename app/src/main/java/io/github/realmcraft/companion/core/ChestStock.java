package io.github.realmcraft.companion.core;
import java.util.*;
/** Counts only readable saved chest contents in loaded coverage; never counts unknown as empty. */
public final class ChestStock {
    public final Map<Integer,Long> quantities=new TreeMap<>();public int unknown;
    public static ChestStock from(List<MapPoint> points){ChestStock stock=new ChestStock();for(MapPoint p:points)if(p.kind.equals("chest")){if(!p.readable){stock.unknown++;continue;}for(PlayerReader.Item i:p.items)stock.quantities.merge(i.id,(long)i.quantity,Long::sum);}return stock;}
    public long available(int id){return quantities.getOrDefault(id,0L);}
    public long missing(int id,long needed){return Math.max(0,needed-available(id));}
}
