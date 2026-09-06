package io.github.realmcraft.companion;

import android.app.*;
import android.os.*;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import io.github.realmcraft.companion.core.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only feature pages for one immutable imported snapshot. */
public final class SnapshotActivity extends Activity {
    public static final int MAP_TAB=2101,PLAYER_TAB=2102,INVENTORY_TAB=2103,DETAILS_TAB=2104,CONTENT=2105,MAP_VIEW=2106;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
    private boolean destroyed,german;private String selected="map";private int dimension;
    private LinearLayout root;private FrameLayout content;private TextView status;private SnapshotStore.Snapshot snapshot;
    private SnapshotAnalysis analysis;private WorldCatalog catalog;
    private String t(String en,String de){return german?de:en;}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);german=Locale.getDefault().getLanguage().equals("de");
        if(saved!=null){selected=saved.getString("tab","map");dimension=saved.getInt("dimension",0);}
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));root.setBackgroundColor(0xff101a19);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(16)+insets.getSystemWindowInsetLeft(),dp(12)+insets.getSystemWindowInsetTop(),dp(16)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom());return insets;});setContentView(root);
        button(root,t("‹ Library","‹ Bibliothek"),this::finish);
        status=text(root,t("Reading snapshot…","Spielstand-Kopie wird gelesen…"),15);status.setContentDescription("Analysis status");
        LinearLayout tabs=new LinearLayout(this);root.addView(tabs);
        tab(tabs,t("Map","Karte"),"map",MAP_TAB);tab(tabs,t("Player","Spieler"),"player",PLAYER_TAB);tab(tabs,t("Inventory","Inventar"),"inventory",INVENTORY_TAB);tab(tabs,t("Details","Details"),"details",DETAILS_TAB);
        content=new FrameLayout(this);content.setId(CONTENT);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        String id=getIntent().getStringExtra("snapshot");
        worker.execute(()->{try{
            if(id==null||!id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}"))throw new IOException("Invalid snapshot ID");
            SnapshotStore store=new SnapshotStore(new File(getFilesDir(),"snapshots"));SnapshotStore.Snapshot found=null;
            for(SnapshotStore.Snapshot s:store.list())if(s.id.equals(id)){found=s;break;}
            if(found==null)throw new IOException("Snapshot unavailable / Kopie nicht verfügbar.");
            SnapshotStore.Snapshot value=found;WorldCatalog names=new WorldCatalog(this);SnapshotAnalysis data=SnapshotAnalysis.load(value);
            main.post(()->{if(destroyed)return;snapshot=value;catalog=names;analysis=data;status.setText(value.name+" · "+t("Saved copy · read only","Gespeicherte Kopie · nur lesen"));render();});
        }catch(Exception e){main.post(()->{if(!destroyed)status.setText(t("Cannot read snapshot: ","Kopie nicht lesbar: ")+e.getMessage());});}});
    }
    private void tab(LinearLayout row,String title,String key,int id){Button b=button(row,title,()->{selected=key;render();});b.setId(id);b.setTextSize(13);b.setMinWidth(0);b.setPadding(0,0,0,0);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));}
    private void render(){content.removeAllViews();if(analysis==null)return;
        for(int id:new int[]{MAP_TAB,PLAYER_TAB,INVENTORY_TAB,DETAILS_TAB}) {
            boolean active=id==(selected.equals("map")?MAP_TAB:selected.equals("player")?PLAYER_TAB:selected.equals("inventory")?INVENTORY_TAB:DETAILS_TAB);
            Button tab=findViewById(id);tab.setSelected(active);
            tab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(active?0xffa9e8bc:0xff334740));
            tab.setTextColor(active?0xff10241b:0xffe1efe8);
        }
        if(selected.equals("map")){showMap();return;}
        ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(12),0,dp(20));scroll.addView(body);content.addView(scroll);
        if(selected.equals("details")){text(body,t("Snapshot details","Details der Kopie"),24);text(body,"World ID: "+snapshot.worldId+"\nSeed: "+snapshot.seed+"\n"+snapshot.fileCount+t(" files"," Dateien")+"\n"+android.text.format.DateFormat.format("yyyy-MM-dd HH:mm",snapshot.importedAt),17);text(body,"SHA-256\n"+snapshot.sha256,13);text(body,t("The map and player data show the time of import. Import again to see newer progress. No live game connection or edits.","Karte und Spielerdaten zeigen den importierten Stand. Für neuere Fortschritte erneut importieren. Keine Live-Verbindung und keine Änderungen am Spiel."),16);return;}
        if(analysis.player==null){text(body,t("Player data unavailable","Spielerdaten nicht verfügbar"),24);text(body,analysis.playerIssue,16);return;}
        PlayerReader player=analysis.player;
        if(selected.equals("player")){
            text(body,t("Player & equipment","Spieler & Ausrüstung"),24);text(body,t("Level ","Level ")+(player.level==null?t("unknown","unbekannt"):player.level),22);
            ArmorAvatarView avatar=new ArmorAvatarView(this,player,catalog);body.addView(avatar,new LinearLayout.LayoutParams(-1,dp(250)));
            text(body,t("Equipment preview · schematic\nPersonal skin and account name are not read.","Ausrüstungsvorschau · schematisch\nPersönlicher Skin und Kontoname werden nicht ausgelesen."),14);
            text(body,player.inventory.size()+t(" / 36 occupied inventory slots"," / 36 belegte Inventarplätze"),17);
            String[] labels=german?new String[]{"Stiefel","Beine","Brust","Kopf"}:new String[]{"Boots","Legs","Chest","Head"};
            for(int slot=4;slot>=1;slot--){PlayerReader.Item item=find(player.armor,slot);if(item==null)text(body,labels[slot-1]+" · —",17);else button(body,labels[slot-1]+" · "+catalog.name(item.id,german),()->itemDetails(item));}
        }else{
            text(body,t("Inventory","Inventar"),24);text(body,t("Slots 1–9: hotbar · Tap an item for details","Plätze 1–9: Schnellleiste · Gegenstand für Details antippen"),14);
            for(int start=1;start<=36;start+=3){LinearLayout row=new LinearLayout(this);body.addView(row);for(int slot=start;slot<start+3;slot++){
                PlayerReader.Item item=find(player.inventory,slot);String label="#"+slot+"\n"+(item==null?"—":catalog.name(item.id,german)+"\n× "+item.quantity);
                Button b=button(row,label,()->{if(item!=null)itemDetails(item);});b.setTextSize(13);b.setMinWidth(0);b.setEnabled(item!=null);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(112),1));
            }}
        }
    }
    private void showMap(){LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);content.addView(page);
        LinearLayout dimensions=new LinearLayout(this);page.addView(dimensions);
        Button overworld=button(dimensions,"Overworld",()->{dimension=0;render();});Button nether=button(dimensions,"Nether",()->{dimension=1;render();});
        overworld.setEnabled(dimension!=0);nether.setEnabled(dimension!=1);
        text(page,t("Surface · drag to pan · tap a block","Oberfläche · ziehen zum Verschieben · Block antippen"),14);
        TextView coords=text(page,t("Saved terrain, schematic colors. Nether view includes its roof.","Gespeichertes Gelände, schematische Farben. Nether-Ansicht einschließlich Dach."),13);
        SurfaceMapView map=new SurfaceMapView(this,analysis.chunks,catalog,german,coords::setText);map.setId(MAP_VIEW);page.addView(map,new LinearLayout.LayoutParams(-1,0,1));map.setDimension(dimension);
        LinearLayout zoom=new LinearLayout(this);page.addView(zoom);button(zoom,"−",()->map.zoom(1/1.6));button(zoom,"+",()->map.zoom(1.6));button(zoom,t("Fit world","Welt einpassen"),map::fit);
        text(page,analysis.chunks.size()+" / "+analysis.totalChunks+t(" chunks read"," Chunks gelesen")+(analysis.skippedChunks>0?" · "+analysis.skippedChunks+t(" unreadable"," nicht lesbar"):"")+(analysis.limited?t(" · limited to 4,096 chunks / 256 MiB"," · auf 4.096 Chunks / 256 MiB begrenzt"):""),13);
    }
    private static PlayerReader.Item find(List<PlayerReader.Item> items,int slot){for(PlayerReader.Item i:items)if(i.slot==slot)return i;return null;}
    private void itemDetails(PlayerReader.Item item){StringBuilder s=new StringBuilder("ID "+item.id+" · Slot "+item.slot+"\n"+t("Quantity: ","Menge: ")+item.quantity);
        if(item.durability!=null)s.append("\n").append(t("Saved durability: ","Gespeicherte Haltbarkeit: ")).append(item.durability);
        for(Map.Entry<Integer,Integer> e:item.enchantments.entrySet())s.append("\n").append(t("Enchantment ID ","Verzauberungs-ID ")).append(e.getKey()).append(" · Level ").append(e.getValue());
        new AlertDialog.Builder(this).setTitle(catalog.name(item.id,german)).setMessage(s).setPositiveButton(android.R.string.ok,null).show();
    }
    private TextView text(LinearLayout parent,String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextColor(Color.rgb(218,235,224));v.setTextSize(size);v.setPadding(dp(4),dp(8),dp(4),dp(8));parent.addView(v);return v;}
    private Button button(LinearLayout parent,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->action.run());parent.addView(b);return b;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onSaveInstanceState(Bundle b){b.putString("tab",selected);b.putInt("dimension",dimension);super.onSaveInstanceState(b);}
    @Override protected void onDestroy(){destroyed=true;worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
