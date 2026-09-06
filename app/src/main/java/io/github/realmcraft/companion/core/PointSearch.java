package io.github.realmcraft.companion.core;

import java.text.Normalizer;
import java.util.*;

/** Searches only decoded evidence; unreadable containers never imply absence of items. */
public final class PointSearch {
    public interface Names {String name(int id);}
    public static final class Hit {
        public final MapPoint point;public final long quantity;public final String matches;
        Hit(MapPoint point,long quantity,String matches){this.point=point;this.quantity=quantity;this.matches=matches;}
    }
    private static String normalized(String text){return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).trim();}
    public static List<Hit> find(List<MapPoint> points,String query,boolean items,Names names){
        String q=normalized(query);List<Hit> hits=new ArrayList<>();Map<Integer,String> searchableNames=new HashMap<>();
        if(q.isEmpty())return hits;
        for(MapPoint point:points){
            if(items){
                if(!point.kind.equals("chest")||!point.readable)continue;
                long quantity=0;StringBuilder matches=new StringBuilder();
                for(PlayerReader.Item item:point.items)if(Integer.toString(item.id).equals(q)||searchableNames.computeIfAbsent(item.id,id->normalized(names.name(id))).contains(q)){
                    quantity+=item.quantity;if(matches.length()>0)matches.append(" · ");matches.append(names.name(item.id)).append(" × ").append(item.quantity);
                }
                if(quantity>0)hits.add(new Hit(point,quantity,matches.toString()));
            }else if(Integer.toString(point.blockId).equals(q)||normalized(names.name(point.blockId)+" "+point.kind+" "+(point.readable?point.text:"")).contains(q))hits.add(new Hit(point,0,""));
        }
        hits.sort(Comparator.comparingInt((Hit h)->h.point.dimension).thenComparingInt(h->h.point.x).thenComparingInt(h->h.point.z).thenComparingInt(h->h.point.y));
        return hits;
    }
}
