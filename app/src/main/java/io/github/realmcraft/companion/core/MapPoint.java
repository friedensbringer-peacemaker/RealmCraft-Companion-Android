package io.github.realmcraft.companion.core;
import java.util.*;
import java.nio.*;
import java.nio.charset.*;

public final class MapPoint {
    public final int x,y,z,dimension,blockId;public final String kind;
    public boolean readable;public String text="",issue="unavailable";
    public List<PlayerReader.Item> items=Collections.emptyList();
    MapPoint(int x,int y,int z,int d,int id,String kind){this.x=x;this.y=y;this.z=z;dimension=d;blockId=id;this.kind=kind;}
    static String kind(int id){if(id==153||id==342||id==283)return "chest";if(id>=81&&id<=96)return "bed";if(id==158)return "crafting";
        if(id>=162&&id<=167||id>=172&&id<=177||id>=736&&id<=739)return "sign";return null;}
    static int number(byte[] b,int p){return ByteBuffer.wrap(b,p,4).getInt();}
    static void readRecords(byte[] b,int start,List<MapPoint> points){
        Map<String,MapPoint> byLocation=new HashMap<>();for(MapPoint p:points)byLocation.put(p.x+","+p.y+","+p.z,p);
        Set<MapPoint> seen=new HashSet<>();
        for(int i=start;i<=b.length-28;i++){
            int marker=(b[i]&255)*256+(b[i+1]&255);if(marker!=162&&marker!=153&&marker!=342)continue;
            MapPoint p=byLocation.get(number(b,i+7)+","+number(b,i+11)+","+number(b,i+15));if(p==null)continue;
            if(marker==162?!(p.blockId==162||p.blockId==172):!(p.blockId==153||p.blockId==342))continue;
            if(!seen.add(p)){p.readable=false;p.text="";p.items=Collections.emptyList();p.issue="duplicate";continue;}
            try{int size=number(b,i+2);long bound=(long)i+6+size;if(size<22||bound>b.length||b[i+6]!=1)throw new IllegalArgumentException();int end=(int)bound;
                if(marker==162){int n=number(b,i+21);if(n<0||n>2048||(long)i+25+n+9!=end)throw new IllegalArgumentException();
                    byte[] trailer={0,15,0,0,0,0,0,15,0};for(int k=0;k<9;k++)if(b[i+25+n+k]!=trailer[k])throw new IllegalArgumentException();
                    p.text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b,i+25,n)).toString();
                }else p.items=ChestReader.parse(Arrays.copyOfRange(b,i+19,end));
                p.readable=true;p.issue="";
            }catch(Exception e){p.readable=false;p.text="";p.items=Collections.emptyList();p.issue="unsupported";}
        }
    }
}
