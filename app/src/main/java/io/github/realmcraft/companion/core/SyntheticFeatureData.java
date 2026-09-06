package io.github.realmcraft.companion.core;
import java.io.*;

/** Deliberately incomplete, non-playable fixtures for the offline feature preview. */
public final class SyntheticFeatureData {
    private SyntheticFeatureData() { }
    public static byte[] player() throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.write(new byte[]{2,0,0,0,1});out.writeInt(0);out.write(new byte[129]);out.write(new byte[]{0,13,1,0,0,0,7});
        out.writeByte(1);out.writeInt(36);out.writeInt(3);
        item(out,3157,12,0,false);item(out,8,64,8,false);item(out,3028,1,35,true);
        out.writeByte(1);out.writeInt(4);out.writeInt(1);item(out,3047,1,3,true);
        out.write(new byte[]{0,41,1});out.write(new byte[11]);out.write(new byte[]{7,0,0,0,0,0,(byte)143,(byte)190,112});
        byte[] data=bytes.toByteArray();java.nio.ByteBuffer.wrap(data).putInt(5,data.length-9);return data;
    }
    private static void item(DataOutputStream out,int id,int quantity,int slot,boolean extra) throws IOException {
        out.writeInt(id);out.write(new byte[]{0,1,2,0,55});out.write(new byte[16]);out.write(new byte[]{0,8,1});out.writeInt(id);out.writeInt(quantity);out.writeInt(0);
        if(extra){out.write(new byte[]{0,24,1});out.writeInt(100);out.writeInt(0);out.write(new byte[]{0,59,0});out.writeInt(1);out.writeShort(7);out.writeInt(2);}
        out.write(new byte[]{0,12,0});out.writeInt(slot);out.writeShort(65535);
    }
    public static byte[] chunk(int x,int z,int dimension) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(9);out.writeInt(x);out.writeInt(z);out.writeByte(dimension);out.writeByte(0);out.writeByte(16);
        for(int section=0;section<16;section++) {
            int[] values=new int[4096];int count=0;
            for(int i=0;i<4096;i++){int y=section*16+i/256,lx=i/16%16,lz=i%16;int height=40+lx/3+lz/4;
                if(y<=height){values[i]=dimension==1?87:(y==height?8:1);count++;}}
            out.writeInt(count);if(count==0)continue;
            ByteArrayOutputStream payload=new ByteArrayOutputStream();DataOutputStream p=new DataOutputStream(payload);p.writeInt(0);
            for(int channel=0;channel<4;channel++)for(int i=0;i<4096;){int value=(values[i]>>>(8*channel))&255,end=i+1;while(end<4096&&((values[end]>>>(8*channel))&255)==value)end++;
                int run=end-i;while(run>255){p.writeByte(0);run-=255;}p.writeByte(run);p.writeByte(value);i=end;}
            out.writeInt(payload.size());out.write(payload.toByteArray());
        }
        return bytes.toByteArray();
    }
}
