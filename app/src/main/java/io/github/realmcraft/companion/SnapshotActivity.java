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
    private MapOptions options;private boolean textures;private String pointKind="all";
    private java.util.List<MapPoint> filtered=new ArrayList<>();private int pointIndex=-1;
    private android.content.SharedPreferences preferences;
    private String t(String en,String de){return german?de:en;}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);german=Locale.getDefault().getLanguage().equals("de");
        preferences=getSharedPreferences("map-settings",0);
        options=new MapOptions(preferences.getInt("chunks",4096),preferences.getInt("ceiling",255),preferences.getInt("radius",0),preferences.getInt("centerX",0),preferences.getInt("centerZ",0));textures=preferences.getBoolean("textures",false);
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
            SnapshotStore.Snapshot value=found;WorldCatalog names=new WorldCatalog(this);SnapshotAnalysis data=SnapshotAnalysis.load(value,options);
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
    private void showMap(){
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);content.addView(page);
        LinearLayout dimensions=new LinearLayout(this);page.addView(dimensions);
        Button overworld=button(dimensions,"Overworld",()->{dimension=0;pointIndex=-1;render();});
        Button nether=button(dimensions,"Nether",()->{dimension=1;pointIndex=-1;render();});
        button(dimensions,t("Settings…","Einstellungen …"),this::mapSettings);
        overworld.setEnabled(dimension!=0);nether.setEnabled(dimension!=1);
        TextView coords=text(page,t("Top block up to Y ","Oberster Block bis Y ")+options.ceiling+t(" · drag / zoom / tap"," · ziehen / zoomen / antippen"),13);
        SurfaceMapView map=new SurfaceMapView(this,analysis.chunks,catalog,german,coords::setText);map.setId(MAP_VIEW);map.setTextures(textures);
        page.addView(map,new LinearLayout.LayoutParams(-1,0,1));map.setDimension(dimension);
        LinearLayout controls=new LinearLayout(this);page.addView(controls);
        button(controls,"−",()->map.zoom(1/1.6));button(controls,"+",()->map.zoom(1.6));button(controls,t("Fit","Einpassen"),map::fit);
        Spinner filter=new Spinner(this);String[] kinds={"all","sign","chest","bed","crafting"};String[] labels=german?new String[]{"Alle Punkte","Schilder","Truhen","Betten","Werkbänke"}:new String[]{"All points","Signs","Chests","Beds","Crafting tables"};
        filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));controls.addView(filter,new LinearLayout.LayoutParams(0,-2,1));filter.setSelection(Arrays.asList(kinds).indexOf(pointKind));
        TextView info=text(page,"",14);info.setMaxLines(2);
        LinearLayout navigation=new LinearLayout(this);page.addView(navigation);
        Button prev=button(navigation,t("‹ Previous","‹ Zurück"),()->{}),next=button(navigation,t("Next ›","Weiter ›"),()->{}),details=button(navigation,t("Point details","Punktdetails"),()->{if(pointIndex>=0&&pointIndex<filtered.size())showPoint(filtered.get(pointIndex));});
        Runnable refresh=()->{
            filtered=new ArrayList<>();for(MapPoint point:analysis.points)if(point.dimension==dimension&&(pointKind.equals("all")||point.kind.equals(pointKind)))filtered.add(point);
            filtered.sort(Comparator.comparingInt((MapPoint v)->v.x).thenComparingInt(v->v.z).thenComparingInt(v->v.y));pointIndex=-1;
            map.setPoints(filtered,p->{pointIndex=filtered.indexOf(p);info.setText(pointDescription(p));details.setEnabled(true);});
            info.setText(filtered.size()+t(" points · includes underground blocks"," Punkte · einschließlich unterirdischer Blöcke"));prev.setEnabled(!filtered.isEmpty());next.setEnabled(!filtered.isEmpty());details.setEnabled(false);
        };
        filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){pointKind=kinds[position];refresh.run();}});
        prev.setOnClickListener(v->cyclePoint(-1,map,info,details));next.setOnClickListener(v->cyclePoint(1,map,info,details));refresh.run();
        text(page,analysis.chunks.size()+" / "+analysis.totalChunks+t(" chunks · "," Chunks · ")+analysis.skippedChunks+t(" unreadable"," nicht lesbar")+(analysis.limited?t(" · render limit reached"," · Renderlimit erreicht"):"")+(analysis.pointsLimited?t(" · point limit reached"," · Punktlimit erreicht"):""),12);
    }
    private void cyclePoint(int step,SurfaceMapView map,TextView info,Button details){if(filtered.isEmpty())return;pointIndex=pointIndex<0?(step>0?0:filtered.size()-1):Math.floorMod(pointIndex+step,filtered.size());MapPoint p=filtered.get(pointIndex);map.focus(p);info.setText((pointIndex+1)+" / "+filtered.size()+" · "+pointDescription(p));details.setEnabled(true);}
    private String pointDescription(MapPoint p){return catalog.name(p.blockId,german)+" · X "+p.x+" · Y "+p.y+" · Z "+p.z+(p.kind.equals("sign")&&p.readable?"\n"+p.text:"");}
    private void showPoint(MapPoint p){StringBuilder body=new StringBuilder("X "+p.x+" · Y "+p.y+" · Z "+p.z+"\n"+t("Saved location; may be underground or above the selected map height.","Gespeicherter Standort; kann unterirdisch oder über der gewählten Kartenhöhe liegen."));
        if(p.kind.equals("sign"))body.append("\n\n").append(p.readable?(p.text.isEmpty()?t("Empty inscription","Leere Beschriftung"):p.text):t("Inscription unavailable: missing, duplicate or unsupported record.","Beschriftung nicht lesbar: fehlender, doppelter oder unbekannter Datensatz."));
        if(p.kind.equals("chest")){body.append("\n\n");if(!p.readable)body.append(t("Contents unavailable. This does not mean the chest is empty.","Inhalt nicht lesbar. Das bedeutet nicht, dass die Truhe leer ist."));else if(p.items.isEmpty())body.append(t("Empty chest","Leere Truhe"));else for(PlayerReader.Item item:p.items)body.append("#").append(item.slot).append(" · ").append(catalog.name(item.id,german)).append(" × ").append(item.quantity).append("\n");}
        if(p.kind.equals("bed"))body.append("\n\n").append(t("Bed block. Both halves can appear separately; this does not establish a player spawn point.","Bettblock. Beide Hälften können separat erscheinen; daraus wird kein Spieler-Spawnpunkt abgeleitet."));
        if(p.kind.equals("crafting"))body.append("\n\n").append(t("Crafting table location. No stored item inventory is assigned to this block.","Standort einer Werkbank. Diesem Block wird kein gespeichertes Inventar zugeordnet."));
        TextView text=new TextView(this);text.setText(body);text.setTextIsSelectable(true);text.setPadding(dp(18),dp(12),dp(18),dp(12));ScrollView scroll=new ScrollView(this);scroll.addView(text);
        new AlertDialog.Builder(this).setTitle(catalog.name(p.blockId,german)).setView(scroll).setPositiveButton(android.R.string.ok,null).show();
    }
    private void mapSettings(){
        ScrollView scroll=new ScrollView(this);LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),dp(8),dp(18),dp(16));scroll.addView(form);
        text(form,t("Display style","Darstellung"),18);Spinner style=spinner(form,new String[]{t("Companion colors","Companion-Farben"),"Kenney Voxel Pack"});style.setSelection(textures?1:0);
        text(form,t("Kenney CC0 textures appear when zoomed in. Unmapped blocks keep their colors. Source: kenney.nl/assets/voxel-pack","Kenney-CC0-Texturen erscheinen beim Hineinzoomen. Nicht zugeordnete Blöcke behalten ihre Farben. Quelle: kenney.nl/assets/voxel-pack"),13);
        text(form,t("Maximum chunks","Maximale Chunks"),18);int[] counts={256,1024,4096,16384};Spinner count=spinner(form,new String[]{"256 · 64 MiB","1,024 · 64 MiB","4,096 · 256 MiB","16,384 · 512 MiB"});for(int i=0;i<counts.length;i++)if(options.chunks==counts[i])count.setSelection(i);
        text(form,t("Area around X/Z (square half-width)","Bereich um X/Z (halbe Quadratbreite)"),18);int[] radii={0,64,128,256,512};Spinner radius=spinner(form,new String[]{t("Whole saved area","Gesamter gespeicherter Bereich"),"±64", "±128","±256","±512"});for(int i=0;i<radii.length;i++)if(options.radius==radii[i])radius.setSelection(i);
        EditText x=numberField(form,"X",options.centerX),z=numberField(form,"Z",options.centerZ);
        TextView yLabel=text(form,t("Highest rendered block: Y ","Höchster dargestellter Block: Y ")+options.ceiling,16);SeekBar y=new SeekBar(this);y.setMax(255);y.setProgress(options.ceiling);form.addView(y);y.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int v,boolean u){yLabel.setText("Y ≤ "+v);}});
        text(form,t("Applies to this device. More chunks need more memory and time. Only saved chunks are available; reaching a limit is reported. Points include all heights within the rendered area.","Gilt auf diesem Gerät. Mehr Chunks benötigen mehr Speicher und Zeit. Es sind nur gespeicherte Chunks verfügbar; erreichte Limits werden angezeigt. Punkte umfassen alle Höhen im gewählten Bereich."),13);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(t("Map settings","Karteneinstellungen")).setView(scroll).setNegativeButton(android.R.string.cancel,null).setPositiveButton(t("Render","Rendern"),null).create();dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            MapOptions next=new MapOptions(counts[count.getSelectedItemPosition()],y.getProgress(),radii[radius.getSelectedItemPosition()],Integer.parseInt(x.getText().toString()),Integer.parseInt(z.getText().toString()));
            options=next;textures=style.getSelectedItemPosition()==1;preferences.edit().putInt("chunks",next.chunks).putInt("ceiling",next.ceiling).putInt("radius",next.radius).putInt("centerX",next.centerX).putInt("centerZ",next.centerZ).putBoolean("textures",textures).apply();dialog.dismiss();reloadMap();
        }catch(NumberFormatException e){x.setError(t("Enter whole-number X/Z coordinates","Ganzzahlige X/Z-Koordinaten eingeben"));}});
    }
    private Spinner spinner(LinearLayout p,String[] labels){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));p.addView(s);return s;}
    private EditText numberField(LinearLayout p,String label,int value){text(p,label,14);EditText e=new EditText(this);e.setHint(label);e.setContentDescription(label);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);e.setText(Integer.toString(value));p.addView(e);return e;}
    private void reloadMap(){analysis=null;pointIndex=-1;content.removeAllViews();status.setText(t("Rendering selected area…","Gewählter Bereich wird gerendert…"));final MapOptions requested=options;
        worker.execute(()->{try{SnapshotAnalysis result=SnapshotAnalysis.load(snapshot,requested);main.post(()->{if(!destroyed){analysis=result;status.setText(snapshot.name+t(" · saved copy"," · gespeicherte Kopie"));render();}});}catch(Exception e){main.post(()->{if(!destroyed)status.setText(e.getMessage());});}});
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
