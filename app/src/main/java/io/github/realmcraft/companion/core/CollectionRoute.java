package io.github.realmcraft.companion.core;
import java.util.*;
/** Greedy horizontal nearest-chest order, not walkable pathfinding. */
public final class CollectionRoute {
    public static final class Stop {public final MapPoint point;public final Map<Integer,Long> take=new TreeMap<>();Stop(MapPoint p){point=p;}}
    public final List<Stop> stops=new ArrayList<>();public final Map<Integer,Long> remaining=new TreeMap<>();
    public static CollectionRoute plan(List<MapPoint> points,Map<Integer,Long> targets,int dimension,double x,double z){CollectionRoute route=new CollectionRoute();route.remaining.putAll(targets);List<MapPoint> candidates=new ArrayList<>();for(MapPoint p:points)if(p.dimension==dimension&&p.kind.equals("chest")&&p.readable)candidates.add(p);
        while(!candidates.isEmpty()&&route.stops.size()<100){MapPoint nearest=null;double distance=Double.POSITIVE_INFINITY;for(MapPoint p:candidates){boolean useful=false;for(PlayerReader.Item item:p.items)if(route.remaining.getOrDefault(item.id,0L)>0){useful=true;break;}double d=Math.hypot(p.x-x,p.z-z);if(useful&&d<distance){nearest=p;distance=d;}}if(nearest==null)break;candidates.remove(nearest);Stop stop=new Stop(nearest);for(PlayerReader.Item i:nearest.items){long n=Math.min((long)i.quantity,Math.max(0,route.remaining.getOrDefault(i.id,0L)));if(n>0){stop.take.merge(i.id,n,Long::sum);route.remaining.merge(i.id,-n,Long::sum);}}route.stops.add(stop);x=nearest.x;z=nearest.z;}
        route.remaining.values().removeIf(v->v<=0);return route;
    }
}
