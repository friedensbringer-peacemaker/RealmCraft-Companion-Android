package io.github.realmcraft.companion.core;

import java.io.IOException;
import java.util.regex.*;

/** Bounded v9 four-channel RLE decoder. Only the top non-air block of each column is retained. */
public final class ChunkSurface {
    public final int x,z,dimension;
    public final int[] ids=new int[256], heights=new int[256]; // z * 16 + x
    public final java.util.List<MapPoint> points=new java.util.ArrayList<>();
    public boolean pointsLimited;
    ChunkSurface(int x,int z,int dimension){this.x=x;this.z=z;this.dimension=dimension;java.util.Arrays.fill(heights,-1);}
    private static IOException invalid(){return new IOException("Unsupported or damaged chunk / Chunk nicht unterstützt oder beschädigt.");}
    private static int integer(byte[] b,int p) throws IOException {
        if(p<0||p>b.length-4)throw invalid();return (b[p]&255)<<24|(b[p+1]&255)<<16|(b[p+2]&255)<<8|(b[p+3]&255);
    }
    public static ChunkSurface decode(byte[] b,String filename) throws IOException {
        return decode(b,filename,255);
    }
    public static ChunkSurface decode(byte[] b,String filename,int ceiling) throws IOException {
        if(ceiling<0||ceiling>255)throw invalid();
        if(b.length<15||b.length>4*1024*1024||integer(b,0)!=9)throw invalid();
        int x=integer(b,4),z=integer(b,8),dimension=b[12]&255;
        if(dimension>1||(b[14]&255)!=16||x%16!=0||z%16!=0)throw invalid();
        Matcher m=Pattern.compile("([on])\\.(-?\\d+),(-?\\d+)").matcher(filename);
        try {if(!m.matches()||Integer.parseInt(m.group(2))!=x||Integer.parseInt(m.group(3))!=z||"on".indexOf(m.group(1))!=dimension)throw invalid();}
        catch(NumberFormatException e){throw invalid();}
        ChunkSurface result=new ChunkSurface(x,z,dimension);int p=15;
        for(int section=0;section<16;section++) {
            if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException();
            int count=integer(b,p);p+=4;if(count==0)continue;
            if(count<0||count>4096)throw invalid();int size=integer(b,p);p+=4;
            if(size<4||size>b.length-p)throw invalid();int end=p+size;p+=4;
            int[] blocks=new int[4096];
            for(int channel=0;channel<4;channel++) {
                int out=0;
                while(out<4096){int run=0;while(p<end&&b[p]==0){run+=255;p++;if(run>=4096)throw invalid();}
                    if(p>end-2)throw invalid();run+=b[p++]&255;int value=b[p++]&255;
                    if(run==0||run>4096-out)throw invalid();
                    if(channel<2) for(int i=out;i<out+run;i++)blocks[i]|=value<<(8*channel);
                    out+=run;
                }
            }
            if(p!=end)throw invalid();
            for(int i=0;i<4096;i++) {int id=blocks[i]&4095;if(id==0||id==639)continue;
                int y=section*16+i/256,localX=(i/16)%16,localZ=i%16,index=localZ*16+localX;
                if(y<=ceiling){result.ids[index]=id;result.heights[index]=y;}
                String kind=MapPoint.kind(id);
                if(kind!=null){if(result.points.size()<4096)result.points.add(new MapPoint(x+localX,y,z+localZ,dimension,id,kind));else result.pointsLimited=true;}
            }
        }
        MapPoint.readRecords(b,p,result.points);
        return result;
    }
}
