package io.github.realmcraft.companion.core;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;

/** Same bounded item/header/footer reader as the Mac map. Unknown extras are not interpreted. */
final class ChestReader {
    static List<PlayerReader.Item> parse(byte[] b)throws IOException {
        if(b.length<9||b[0]!=1||ByteBuffer.wrap(b,1,4).getInt()!=27)throw new IOException("Unsupported chest");
        int count=ByteBuffer.wrap(b,5,4).getInt();if(count<0||count>27)throw new IOException("Invalid count");
        String binary=new String(b,java.nio.charset.StandardCharsets.ISO_8859_1);
        Pattern pattern=Pattern.compile("\\x00\\x00..\\x00\\x01\\x02\\x00\\x37.{16}\\x00\\x08\\x01",Pattern.DOTALL);
        Matcher matcher=pattern.matcher(binary);matcher.region(9,b.length);List<Integer> starts=new ArrayList<>();while(matcher.find())starts.add(matcher.start());
        if(starts.size()!=count||count==0&&b.length!=9||count>0&&starts.get(0)!=9)throw new IOException("Unsupported items");
        Set<Integer> slots=new HashSet<>();List<PlayerReader.Item> result=new ArrayList<>();
        for(int i=0;i<count;i++){int start=starts.get(i),end=i+1<count?starts.get(i+1):b.length;
            if(end-start<49||b[end-9]!=0||b[end-8]!=12||b[end-7]!=0||(b[end-2]&255)!=255||(b[end-1]&255)!=255)throw new IOException("Invalid footer");
            int id=(b[start+2]&255)*256+(b[start+3]&255),other=(b[start+30]&255)*256+(b[start+31]&255);
            int quantity=ByteBuffer.wrap(b,start+32,4).getInt(),slot=ByteBuffer.wrap(b,end-6,4).getInt();
            if(b[start+28]!=0||b[start+29]!=0||id!=other||quantity<=0||slot<0||slot>=27||!slots.add(slot))throw new IOException("Invalid item");
            result.add(new PlayerReader.Item(slot+1,id,quantity,null,Collections.emptyMap()));
        }
        result.sort(Comparator.comparingInt(p->p.slot));return result;
    }
}
