package io.github.realmcraft.companion.core;
/** Explicit user-selected work budget; independently bounded even for forged settings. */
public final class MapOptions {
    public final int chunks,ceiling,radius,centerX,centerZ;
    public final long byteLimit;public final boolean slice;
    public MapOptions(int chunks,int ceiling,int radius,int centerX,int centerZ){
        this(chunks,ceiling,radius,centerX,centerZ,false);
    }
    public MapOptions(int chunks,int ceiling,int radius,int centerX,int centerZ,boolean slice){
        this.slice=slice;this.chunks=Math.max(1,Math.min(16384,chunks));this.ceiling=Math.max(0,Math.min(255,ceiling));
        this.radius=Math.max(0,Math.min(4096,radius));this.centerX=centerX;this.centerZ=centerZ;
        byteLimit=this.chunks<=1024?64L*1024*1024:this.chunks<=4096?256L*1024*1024:512L*1024*1024;
    }
    public static MapOptions defaults(){return new MapOptions(4096,255,0,0,0);}
    public boolean includes(int x,int z){return radius==0 || ((long)x+15>=((long)centerX-radius)&&x<=(long)centerX+radius&&(long)z+15>=(long)centerZ-radius&&z<=(long)centerZ+radius);}
}
