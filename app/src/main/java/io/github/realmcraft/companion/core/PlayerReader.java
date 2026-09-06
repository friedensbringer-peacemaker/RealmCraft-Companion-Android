package io.github.realmcraft.companion.core;

import java.io.IOException;
import java.util.*;

/** Port of the Companion's strict observed v2 player / v1 item reader. */
public final class PlayerReader {
    public static final class Item {
        public final int slot, id, quantity;
        public final Integer durability;
        public final Map<Integer,Integer> enchantments;
        Item(int slot, int id, int quantity, Integer durability, Map<Integer,Integer> effects) {
            this.slot=slot; this.id=id; this.quantity=quantity; this.durability=durability;
            enchantments=Collections.unmodifiableMap(effects);
        }
    }
    public final List<Item> inventory, armor;
    public final Integer level;
    private PlayerReader(List<Item> inventory, List<Item> armor, Integer level) {
        this.inventory=Collections.unmodifiableList(inventory); this.armor=Collections.unmodifiableList(armor); this.level=level;
    }
    public static PlayerReader parse(byte[] bytes) throws IOException { return new Decoder(bytes).parse(); }
    private static final class Decoder {
        final byte[] b; int p;
        Decoder(byte[] b) { this.b=b; }
        void require(boolean ok) throws IOException { if(!ok) throw new IOException("Unsupported or incomplete player data / Spielerdaten nicht unterstützt oder unvollständig."); }
        boolean match(int at, int... values) {
            if(at<0 || at>b.length-values.length) return false;
            for(int i=0;i<values.length;i++) if((b[at+i]&255)!=values[i])return false;
            return true;
        }
        long number(int at) throws IOException {
            require(at>=0 && at<=b.length-4);
            return ((long)(b[at]&255)<<24)|((long)(b[at+1]&255)<<16)|((long)(b[at+2]&255)<<8)|(b[at+3]&255);
        }
        PlayerReader parse() throws IOException {
            require(b.length>=150 && b.length<=4_000_000 && match(0,2,0,0,0,1)); require(number(5)==b.length-9);
            int anchor=-1;
            for(int i=0;i<=b.length-12;i++) if(match(i,0,13,1)&&match(i+7,1,0,0,0,36)) {require(anchor<0);anchor=i;}
            require(anchor>=0); p=anchor+7;
            List<Item> inventory=container(36), armor=container(4);
            int xp=-1, occurrences=0;
            for(int i=p;i<=b.length-3;i++) if(match(i,0,41,1)){xp=i;occurrences++;}
            Integer level=null;
            if(occurrences==1 && match(xp+18,0,0,143,190,112)) {
                long n=Integer.toUnsignedLong(Integer.reverseBytes((int)number(xp+14)));
                if(n<=1_000_000) level=(int)n;
            }
            return new PlayerReader(inventory,armor,level);
        }
        List<Item> container(int capacity) throws IOException {
            require(match(p,1)&&number(p+1)==capacity);long count=number(p+5);require(count<=capacity);p+=9;
            List<Item> result=new ArrayList<>();Set<Integer> slots=new HashSet<>();
            for(int i=0;i<count;i++) {
                long id=number(p),qty=number(p+32);
                require(match(p+4,0,1,2,0,55)&&match(p+25,0,8,1)&&number(p+28)==id);
                require(id>0&&id<=65535&&qty>0&&qty<=Integer.MAX_VALUE);p+=36;
                require(match(p,0,0,0,0));p+=4;Integer durability=null;Map<Integer,Integer> effects=new LinkedHashMap<>();
                if(match(p,0,24,1)) {
                    long remaining=number(p+3),uses=number(p+7);require(remaining<=Integer.MAX_VALUE&&uses<=Integer.MAX_VALUE);
                    durability=(int)remaining;p+=11;require(match(p,0,59,0));long n=number(p+3);require(n<=256);p+=7;
                    for(int e=0;e<n;e++) {require(p<=b.length-6);int key=(b[p]&255)*256+(b[p+1]&255);long value=number(p+2);
                        require(value>0&&value<=Integer.MAX_VALUE&&!effects.containsKey(key));effects.put(key,(int)value);p+=6;}
                }
                require(match(p,0,12,0));long slot=number(p+3);require(slot<capacity&&slots.add((int)slot)&&match(p+7,255,255));p+=9;
                result.add(new Item((int)slot+1,(int)id,(int)qty,durability,effects));
            }
            result.sort(Comparator.comparingInt(i->i.slot));return result;
        }
    }
}
