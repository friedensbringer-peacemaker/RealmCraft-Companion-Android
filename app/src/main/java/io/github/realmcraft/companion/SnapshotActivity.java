package io.github.realmcraft.companion;

import android.app.*;
import android.os.*;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import io.github.realmcraft.companion.core.*;
import java.io.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only feature pages for one immutable imported snapshot. */
public final class SnapshotActivity extends Activity {
    public static final int MAP_TAB=2101,PLAYER_TAB=2102,INVENTORY_TAB=2103,DETAILS_TAB=2104,CONTENT=2105,MAP_VIEW=2106,LARGE_MAP=2107,MAP_TOOLS=2108,SEARCH_POINTS=2109,SEARCH_ITEMS=2110,SEARCH_QUERY=2111,SEARCH_RESULTS=2112,NOTEBOOK=2113,REFERENCE=2114,COMPARE=2115;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
    private boolean destroyed,german;private String selected="map";private int dimension;
    AlertDialog searchDialog,featureDialog;private MapNotebook notebook;private boolean compact;
    private boolean largeMap,showTools;private View libraryButton,tabsView;private SurfaceMapView activeMap;
    private final double[][] viewports=new double[2][];private final List<View> mapTools=new ArrayList<>();
    private LinearLayout root;private FrameLayout content;private TextView status;private SnapshotStore.Snapshot snapshot;
    private SnapshotAnalysis analysis;private WorldCatalog catalog;
    private MapOptions options;private boolean textures;private String pointKind="all";
    private java.util.List<MapPoint> filtered=new ArrayList<>();private int pointIndex=-1;
    private android.content.SharedPreferences preferences;
    private String t(String en,String de){return german?de:en;}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);german=Locale.getDefault().getLanguage().equals("de");
        preferences=getSharedPreferences("map-settings",0);
        options=new MapOptions(preferences.getInt("chunks",4096),preferences.getInt("ceiling",255),preferences.getInt("radius",0),preferences.getInt("centerX",0),preferences.getInt("centerZ",0));textures=preferences.getBoolean("textures",false);
        if(saved!=null){selected=saved.getString("tab","map");dimension=saved.getInt("dimension",0);largeMap=saved.getBoolean("largeMap");showTools=saved.getBoolean("showTools");for(int i=0;i<2;i++)viewports[i]=saved.getDoubleArray("viewport"+i);}
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));root.setBackgroundColor(0xff101a19);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(16)+insets.getSystemWindowInsetLeft(),dp(12)+insets.getSystemWindowInsetTop(),dp(16)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom());return insets;});setContentView(root);
        libraryButton=button(root,t("‹ Library","‹ Bibliothek"),this::finish);
        status=text(root,t("Reading snapshot…","Spielstand-Kopie wird gelesen…"),15);status.setContentDescription("Analysis status");
        LinearLayout tabs=new LinearLayout(this);root.addView(tabs);tabsView=tabs;
        tab(tabs,t("Map","Karte"),"map",MAP_TAB);tab(tabs,t("Player","Spieler"),"player",PLAYER_TAB);tab(tabs,t("Inventory","Inventar"),"inventory",INVENTORY_TAB);tab(tabs,t("Details","Details"),"details",DETAILS_TAB);
        content=new FrameLayout(this);content.setId(CONTENT);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        String id=getIntent().getStringExtra("snapshot");
        worker.execute(()->{try{
            if(id==null||!id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}"))throw new IOException("Invalid snapshot ID");
            SnapshotStore store=new SnapshotStore(new File(getFilesDir(),"snapshots"));SnapshotStore.Snapshot found=null;
            for(SnapshotStore.Snapshot s:store.list())if(s.id.equals(id)){found=s;break;}
            if(found==null)throw new IOException("Snapshot unavailable / Kopie nicht verfügbar.");
            SnapshotStore.Snapshot value=found;
            preferences=getSharedPreferences("map-world-"+value.worldId+"-"+value.seed,0);notebook=new MapNotebook(this,value.worldId+"-"+value.seed);
            options=new MapOptions(preferences.getInt("chunks",4096),preferences.getInt("ceiling",255),preferences.getInt("radius",0),preferences.getInt("centerX",0),preferences.getInt("centerZ",0),preferences.getBoolean("slice",false));textures=preferences.getBoolean("textures",false);compact=preferences.getBoolean("compact",false);
            if(saved==null){dimension=Math.max(0,Math.min(1,preferences.getInt("dimension",0)));largeMap=preferences.getBoolean("largeMap",false);showTools=preferences.getBoolean("showTools",false);pointKind=preferences.getString("pointKind","all");for(int i=0;i<2;i++)if(preferences.contains("zoom"+i))viewports[i]=new double[]{Double.longBitsToDouble(preferences.getLong("x"+i,0)),Double.longBitsToDouble(preferences.getLong("z"+i,0)),Double.longBitsToDouble(preferences.getLong("zoom"+i,0))};}
            WorldCatalog names=new WorldCatalog(this);SnapshotAnalysis data=SnapshotAnalysis.load(value,options);
            main.post(()->{if(destroyed)return;snapshot=value;catalog=names;analysis=data;status.setText(value.name+" · "+t("Saved copy · read only","Gespeicherte Kopie · nur lesen"));render();});
        }catch(Exception e){main.post(()->{if(!destroyed)status.setText(t("Cannot read snapshot: ","Kopie nicht lesbar: ")+e.getMessage());});}});
    }
    private void tab(LinearLayout row,String title,String key,int id){Button b=button(row,title,()->{selected=key;render();});b.setId(id);b.setTextSize(13);b.setMinWidth(0);b.setPadding(0,0,0,0);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));}
    private void render(){rememberMap();activeMap=null;mapTools.clear();content.removeAllViews();if(analysis==null)return;
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
        LinearLayout toolbar=new LinearLayout(this);page.addView(toolbar);
        Button expand=button(toolbar,"",()->{largeMap=!largeMap;applyMapMode();});expand.setId(LARGE_MAP);
        Button tools=button(toolbar,"",()->{showTools=!showTools;applyMapMode();});tools.setId(MAP_TOOLS);
        LinearLayout dimensions=new LinearLayout(this);HorizontalScrollView dimensionScroll=new HorizontalScrollView(this);dimensionScroll.addView(dimensions);page.addView(dimensionScroll);mapTools.add(dimensionScroll);
        Button overworld=button(dimensions,"Overworld",()->{rememberMap();activeMap=null;dimension=0;pointIndex=-1;render();});
        Button nether=button(dimensions,"Nether",()->{rememberMap();activeMap=null;dimension=1;pointIndex=-1;render();});
        button(dimensions,t("Settings…","Einstellungen …"),this::mapSettings);
        button(dimensions,t("More…","Mehr …"),this::moreTools);
        LinearLayout searches=new LinearLayout(this);page.addView(searches);mapTools.add(searches);
        Button searchPoints=button(searches,t("Search points…","Punkte suchen …"),()->search(false));searchPoints.setId(SEARCH_POINTS);
        Button searchItems=button(searches,t("Find chest items…","Truheninhalt suchen …"),()->search(true));searchItems.setId(SEARCH_ITEMS);
        for(Button b:new Button[]{searchPoints,searchItems}){b.setMinWidth(0);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),1));}
        overworld.setEnabled(dimension!=0);nether.setEnabled(dimension!=1);
        TextView coords=text(page,(options.slice?t("Exact layer Y ","Exakte Ebene Y "):t("Top block up to Y ","Oberster Block bis Y "))+options.ceiling+t(" · drag / zoom / tap"," · ziehen / zoomen / antippen"),13);
        SurfaceMapView map=new SurfaceMapView(this,analysis.chunks,catalog,german,coords::setText);map.setId(MAP_VIEW);map.setTextures(textures);map.setAnnotations(notebook.list(),reference());
        activeMap=map;page.addView(map,new LinearLayout.LayoutParams(-1,0,1));map.setDimension(dimension);map.restoreViewport(viewports[dimension]);
        button(toolbar,"−",()->map.zoom(1/1.6));button(toolbar,"+",()->map.zoom(1.6));button(toolbar,t("Fit","Einpassen"),map::fit);
        for(int i=0;i<toolbar.getChildCount();i++){Button b=(Button)toolbar.getChildAt(i);b.setMinWidth(0);b.setTextSize(13);b.setPadding(dp(4),0,dp(4),0);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(48),i<2?2:1));}
        mapTools.add(coords);
        LinearLayout controls=new LinearLayout(this);page.addView(controls);mapTools.add(controls);
        button(controls,t("Go to X/Z…","Zu X/Z …"),()->goTo(map));
        Spinner filter=new Spinner(this);String[] kinds={"all","sign","chest","bed","crafting"};String[] labels=german?new String[]{"Alle Punkte","Schilder","Truhen","Betten","Werkbänke"}:new String[]{"All points","Signs","Chests","Beds","Crafting tables"};
        filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));controls.addView(filter,new LinearLayout.LayoutParams(0,-2,1));filter.setSelection(Arrays.asList(kinds).indexOf(pointKind));
        TextView info=text(page,"",14);info.setMaxLines(2);mapTools.add(info);
        LinearLayout navigation=new LinearLayout(this);page.addView(navigation);mapTools.add(navigation);
        Button prev=button(navigation,t("‹ Previous","‹ Zurück"),()->{}),next=button(navigation,t("Next ›","Weiter ›"),()->{}),details=button(navigation,t("Point details","Punktdetails"),()->{if(pointIndex>=0&&pointIndex<filtered.size())showPoint(filtered.get(pointIndex));});
        Runnable refresh=()->{
            filtered=new ArrayList<>();for(MapPoint point:analysis.points)if(point.dimension==dimension&&(pointKind.equals("all")||point.kind.equals(pointKind)))filtered.add(point);
            filtered.sort(Comparator.comparingInt((MapPoint v)->v.x).thenComparingInt(v->v.z).thenComparingInt(v->v.y));pointIndex=-1;
            map.setPoints(filtered,p->{pointIndex=filtered.indexOf(p);info.setText(pointDescription(p));details.setEnabled(true);});
            info.setText(filtered.size()+t(" points · includes underground blocks"," Punkte · einschließlich unterirdischer Blöcke"));prev.setEnabled(!filtered.isEmpty());next.setEnabled(!filtered.isEmpty());details.setEnabled(false);
        };
        filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){pointKind=kinds[position];refresh.run();}});
        prev.setOnClickListener(v->cyclePoint(-1,map,info,details));next.setOnClickListener(v->cyclePoint(1,map,info,details));refresh.run();
        mapTools.add(text(page,analysis.chunks.size()+" / "+analysis.totalChunks+t(" chunks · "," Chunks · ")+analysis.skippedChunks+t(" unreadable"," nicht lesbar")+(analysis.limited?t(" · render limit reached"," · Renderlimit erreicht"):"")+(analysis.pointsLimited?t(" · point limit reached"," · Punktlimit erreicht"):""),12));applyMapMode();
    }
    private void search(boolean items){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),0);
        EditText query=new EditText(this);query.setSingleLine(true);query.setId(SEARCH_QUERY);query.setHint(items?t("Item name or ID","Gegenstandsname oder ID"):t("Block name, sign text or ID","Blockname, Schildertext oder ID"));form.addView(query);
        text(form,t("Searches both dimensions in the loaded area. Render and point limits apply; unreadable signs or chests cannot be searched completely.","Durchsucht beide Dimensionen im geladenen Bereich. Render- und Punktlimits gelten; unlesbare Schilder oder Truhen sind nicht vollständig durchsuchbar."),13);
        TextView summary=text(form,"",14);ListView results=new ListView(this);results.setId(SEARCH_RESULTS);form.addView(results,new LinearLayout.LayoutParams(-1,dp(240)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(items?t("Find chest items","Truheninhalt suchen"):t("Search map points","Kartenpunkte suchen")).setView(form).setNegativeButton(android.R.string.cancel,null).create();
        final List<PointSearch.Hit> displayed=new ArrayList<>();
        Runnable update=()->{
            List<PointSearch.Hit> hits=PointSearch.find(analysis.points,query.getText().toString(),items,id->catalog.name(id,german));displayed.clear();displayed.addAll(hits.subList(0,Math.min(100,hits.size())));
            long amount=0;for(PointSearch.Hit h:hits)amount+=h.quantity;
            int unreadable=0;for(MapPoint p:analysis.points)if((items?p.kind.equals("chest"):p.kind.equals("chest")||p.kind.equals("sign"))&&!p.readable)unreadable++;
            summary.setText(hits.size()+t(" matches"," Treffer")+(items?" · "+amount+t(" items"," Gegenstände"):"")+" · "+unreadable+t(" unreadable points"," unlesbare Punkte")+(hits.size()>100?t(" · first 100 shown; refine search"," · erste 100 angezeigt; Suche eingrenzen"):""));
            List<String> labels=new ArrayList<>();for(PointSearch.Hit h:displayed)labels.add(pointDescription(h.point)+" · "+(h.point.dimension==0?"Overworld":"Nether")+(items?"\n"+h.matches:""));
            results.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels));
        };
        query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){update.run();}public void afterTextChanged(android.text.Editable s){}});
        results.setOnItemClickListener((parent,view,position,id)->{MapPoint point=displayed.get(position).point;dialog.dismiss();rememberMap();activeMap=null;dimension=point.dimension;pointKind="all";render();activeMap.focus(point);showPoint(point);});
        searchDialog=dialog;dialog.show();update.run();
    }
    private void rememberMap(){if(activeMap!=null)viewports[dimension]=activeMap.viewport();}
    private void saveMapState(){if(snapshot==null)return;rememberMap();android.content.SharedPreferences.Editor e=preferences.edit().putInt("dimension",dimension).putBoolean("largeMap",largeMap).putBoolean("showTools",showTools).putString("pointKind",pointKind).putBoolean("compact",compact);
        for(int i=0;i<2;i++)if(viewports[i]!=null)e.putLong("x"+i,Double.doubleToLongBits(viewports[i][0])).putLong("z"+i,Double.doubleToLongBits(viewports[i][1])).putLong("zoom"+i,Double.doubleToLongBits(viewports[i][2]));e.apply();}
    @Override public void finish(){saveMapState();super.finish();}
    @Override protected void onPause(){saveMapState();super.onPause();}
    private void applyMapMode(){
        libraryButton.setVisibility(largeMap?View.GONE:View.VISIBLE);status.setVisibility(largeMap?View.GONE:View.VISIBLE);tabsView.setVisibility(largeMap?View.GONE:View.VISIBLE);
        for(View v:mapTools)v.setVisibility(!largeMap||showTools?View.VISIBLE:View.GONE);
        Button expand=findViewById(LARGE_MAP),tools=findViewById(MAP_TOOLS);
        if(expand!=null)expand.setText(largeMap?t("‹ Small map","‹ Kleine Karte"):t("⛶ Large map","⛶ Große Karte"));
        if(tools!=null){tools.setVisibility(largeMap?View.VISIBLE:View.GONE);tools.setText(showTools?t("Hide tools","Werkzeuge aus"):t("Show tools","Werkzeuge ein"));}
    }
    @Override public void onBackPressed(){if(largeMap){largeMap=false;applyMapMode();}else super.onBackPressed();}
    private void goTo(SurfaceMapView map){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),0);
        double[] viewport=map.viewport();EditText x=numberField(form,"X",viewport==null?0:(int)viewport[0]),z=numberField(form,"Z",viewport==null?0:(int)viewport[1]);
        text(form,t("Centers the map. Unsaved or excluded areas stay blank.","Zentriert die Karte. Nicht gespeicherte oder ausgeblendete Bereiche bleiben leer."),14);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(t("Go to coordinates","Zu Koordinaten springen")).setView(form).setNegativeButton(android.R.string.cancel,null).setPositiveButton(t("Go","Anzeigen"),null).create();dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{map.centerOn(Integer.parseInt(x.getText().toString()),Integer.parseInt(z.getText().toString()));dialog.dismiss();}catch(NumberFormatException e){x.setError(t("Enter whole-number X/Z coordinates","Ganzzahlige X/Z-Koordinaten eingeben"));}});
    }
    private void cyclePoint(int step,SurfaceMapView map,TextView info,Button details){if(filtered.isEmpty())return;pointIndex=pointIndex<0?(step>0?0:filtered.size()-1):Math.floorMod(pointIndex+step,filtered.size());MapPoint p=filtered.get(pointIndex);map.focus(p);info.setText((pointIndex+1)+" / "+filtered.size()+" · "+pointDescription(p));details.setEnabled(true);}
    private String pointDescription(MapPoint p){return catalog.name(p.blockId,german)+" · X "+p.x+" · Y "+p.y+" · Z "+p.z+distance(p.x,p.z,p.dimension)+(p.kind.equals("sign")&&p.readable?"\n"+p.text:"");}
    private void showPoint(MapPoint p){StringBuilder body=new StringBuilder("X "+p.x+" · Y "+p.y+" · Z "+p.z+"\n"+t("Saved location; may be underground or above the selected map height.","Gespeicherter Standort; kann unterirdisch oder über der gewählten Kartenhöhe liegen."));
        if(p.kind.equals("sign"))body.append("\n\n").append(p.readable?(p.text.isEmpty()?t("Empty inscription","Leere Beschriftung"):p.text):t("Inscription unavailable: missing, duplicate or unsupported record.","Beschriftung nicht lesbar: fehlender, doppelter oder unbekannter Datensatz."));
        if(p.kind.equals("chest")){body.append("\n\n");if(!p.readable)body.append(t("Contents unavailable. This does not mean the chest is empty.","Inhalt nicht lesbar. Das bedeutet nicht, dass die Truhe leer ist."));else if(p.items.isEmpty())body.append(t("Empty chest","Leere Truhe"));else for(PlayerReader.Item item:p.items)body.append("#").append(item.slot).append(" · ").append(catalog.name(item.id,german)).append(" × ").append(item.quantity).append("\n");}
        if(p.kind.equals("bed"))body.append("\n\n").append(t("Bed block. Both halves can appear separately; this does not establish a player spawn point.","Bettblock. Beide Hälften können separat erscheinen; daraus wird kein Spieler-Spawnpunkt abgeleitet."));
        if(p.kind.equals("crafting"))body.append("\n\n").append(t("Crafting table location. No stored item inventory is assigned to this block.","Standort einer Werkbank. Diesem Block wird kein gespeichertes Inventar zugeordnet."));
        TextView text=new TextView(this);text.setText(body);text.setTextIsSelectable(true);text.setPadding(dp(18),dp(12),dp(18),dp(12));ScrollView scroll=new ScrollView(this);scroll.addView(text);
        new AlertDialog.Builder(this).setTitle(catalog.name(p.blockId,german)).setView(scroll).setPositiveButton(android.R.string.ok,null).show();
    }
    private void moreTools(){
        String[] labels={t("Markers & favorites","Markierungen & Favoriten"),t("Manual reference position","Manuelle Referenzposition"),t("Compare snapshots","Spielstände vergleichen"),t("Layer down","Ebene tiefer"),t("Layer up","Ebene höher"),t("Compact Quest layout","Kompakte Quest-Ansicht"),t("Packing list","Packliste"),t("Map bookmarks","Karten-Lesezeichen")};
        featureDialog=new AlertDialog.Builder(this).setTitle(t("Map tools","Kartenwerkzeuge")).setItems(labels,(d,i)->{if(i==0)markers();else if(i==1)editReference();else if(i==2)chooseComparison();else if(i==6)packingList();else if(i==7)bookmarks();else if(i==5){compact=!compact;largeMap=true;showTools=!compact;applyMapMode();saveMapState();}else{options=new MapOptions(options.chunks,options.ceiling+(i==3?-1:1),options.radius,options.centerX,options.centerZ,true);preferences.edit().putInt("ceiling",options.ceiling).putBoolean("slice",true).apply();reloadMap();}}).setNegativeButton(android.R.string.cancel,null).show();
    }
    private double[] reference(){if(!preferences.contains("refX"))return null;return new double[]{preferences.getInt("refX",0),preferences.getInt("refY",0),preferences.getInt("refZ",0),preferences.getInt("refD",0)};}
    private String distance(int x,int z,int d){double[] r=reference();return r==null||r[3]!=d?"":String.format(Locale.ROOT,t(" · %.1f blocks from manual reference"," · %.1f Blöcke zur manuellen Referenz"),Math.hypot(x-r[0],z-r[2]));}
    void editReference(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);double[] r=reference();double[] v=activeMap.viewport();
        text(form,t("Automatic saved player position is unavailable. Enter a reference manually; distances are horizontal, not routes.","Automatische gespeicherte Spielerposition nicht verfügbar. Referenz manuell eingeben; Entfernungen sind horizontal, keine Wegstrecken."),15);
        EditText x=numberField(form,"X",r==null?(int)v[0]:(int)r[0]),y=numberField(form,"Y",r==null?64:(int)r[1]),z=numberField(form,"Z",r==null?(int)v[1]:(int)r[2]);Spinner dim=spinner(form,new String[]{"Overworld","Nether"});dim.setSelection(r==null?dimension:(int)r[3]);ScrollView referenceScroll=new ScrollView(this);referenceScroll.addView(form);
        featureDialog=new AlertDialog.Builder(this).setTitle(t("Manual reference","Manuelle Referenz")).setView(referenceScroll).setNegativeButton(android.R.string.cancel,null).setNeutralButton(t("Clear","Entfernen"),(d,w)->{preferences.edit().remove("refX").remove("refY").remove("refZ").remove("refD").apply();render();}).setPositiveButton(t("Save & center","Speichern & zentrieren"),null).create();featureDialog.show();
        featureDialog.getButton(-1).setOnClickListener(vw->{try{int rx=Integer.parseInt(x.getText().toString()),ry=Integer.parseInt(y.getText().toString()),rz=Integer.parseInt(z.getText().toString());preferences.edit().putInt("refX",rx).putInt("refY",ry).putInt("refZ",rz).putInt("refD",dim.getSelectedItemPosition()).apply();rememberMap();activeMap=null;dimension=dim.getSelectedItemPosition();featureDialog.dismiss();render();activeMap.centerOn(rx,rz);}catch(NumberFormatException e){x.setError(t("Enter whole numbers","Ganzzahlen eingeben"));}});
    }
    void markers(){List<MapNotebook.Marker> rows=notebook.list();String[] labels=new String[rows.size()];for(int i=0;i<rows.size();i++){MapNotebook.Marker m=rows.get(i);labels[i]=(m.favorite?"★ ":"")+m.name+" · "+(m.dimension==0?"Overworld":"Nether")+" · "+m.x+", "+m.y+", "+m.z+distance(m.x,m.z,m.dimension);}
        featureDialog=new AlertDialog.Builder(this).setTitle(t("Markers & favorites","Markierungen & Favoriten")).setItems(labels,(d,i)->{
            MapNotebook.Marker m=rows.get(i);featureDialog=new AlertDialog.Builder(this).setTitle(m.name).setItems(new String[]{t("Show on map","Auf Karte zeigen"),t("Edit / favorite","Bearbeiten / Favorit"),t("Delete marker","Markierung löschen")},(a,n)->{if(n==0){rememberMap();activeMap=null;dimension=m.dimension;render();activeMap.centerOn(m.x,m.z);}else if(n==1)editMarker(m);else new AlertDialog.Builder(this).setTitle(t("Delete marker?","Markierung löschen?")).setMessage(m.name).setNegativeButton(android.R.string.cancel,null).setPositiveButton(t("Delete","Löschen"),(b,c)->{notebook.delete(m.id);render();}).show();}).show();
        }).setPositiveButton(t("Add marker","Markierung hinzufügen"),(d,w)->editMarker(null)).setNegativeButton(android.R.string.cancel,null).show();
    }
    void editMarker(MapNotebook.Marker existing){
        MapNotebook.Marker m=existing==null?new MapNotebook.Marker():existing;double[] v=activeMap.viewport();if(existing==null){m.id=UUID.randomUUID().toString();m.x=(int)v[0];m.y=options.ceiling;m.z=(int)v[1];m.dimension=dimension;m.name="";}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);EditText name=new EditText(this);name.setId(R.id.marker_name);name.setHint(t("Name","Name"));name.setText(m.name);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});form.addView(name);
        EditText x=numberField(form,"X",m.x),y=numberField(form,"Y",m.y),z=numberField(form,"Z",m.z);Spinner dim=spinner(form,new String[]{"Overworld","Nether"});dim.setSelection(m.dimension);CheckBox favorite=new CheckBox(this);favorite.setText(t("Favorite","Favorit"));favorite.setChecked(m.favorite);form.addView(favorite);ScrollView scroll=new ScrollView(this);scroll.addView(form);
        featureDialog=new AlertDialog.Builder(this).setTitle(t("Local marker","Lokale Markierung")).setView(scroll).setNegativeButton(android.R.string.cancel,null).setPositiveButton(t("Save","Speichern"),null).create();featureDialog.show();featureDialog.getButton(-1).setOnClickListener(w->{try{String label=name.getText().toString().trim();if(label.isEmpty()){name.setError(t("Enter a name","Namen eingeben"));return;}m.x=Integer.parseInt(x.getText().toString());m.y=Integer.parseInt(y.getText().toString());m.z=Integer.parseInt(z.getText().toString());m.name=label;m.dimension=dim.getSelectedItemPosition();m.favorite=favorite.isChecked();notebook.save(m);featureDialog.dismiss();render();}catch(RuntimeException e){name.setError(e.getMessage());}});
    }
    void chooseComparison(){status.setText(t("Reading library…","Bibliothek wird gelesen…"));worker.execute(()->{try{List<SnapshotStore.Snapshot> rows=new ArrayList<>();for(SnapshotStore.Snapshot s:new SnapshotStore(new File(getFilesDir(),"snapshots")).list())if(!s.id.equals(snapshot.id)&&s.worldId.equals(snapshot.worldId)&&s.seed.equals(snapshot.seed))rows.add(s);
        main.post(()->{if(destroyed)return;if(rows.isEmpty()){new AlertDialog.Builder(this).setMessage(t("Import another copy of the same world first.","Zuerst eine weitere Kopie derselben Welt importieren.")).setPositiveButton(android.R.string.ok,null).show();return;}String[] labels=new String[rows.size()];for(int i=0;i<rows.size();i++)labels[i]=rows.get(i).name+" · "+android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss",rows.get(i).importedAt);featureDialog=new AlertDialog.Builder(this).setTitle(t("Compare from selected copy → current copy","Vergleich: gewählte Kopie → aktuelle Kopie")).setItems(labels,(d,i)->compare(rows.get(i))).setNegativeButton(android.R.string.cancel,null).show();});
        }catch(Exception e){main.post(()->{if(!destroyed)status.setText(e.getMessage());});}});}
    private void compare(SnapshotStore.Snapshot before){status.setText(t("Comparing…","Wird verglichen…"));final SnapshotAnalysis current=analysis;final MapOptions requested=options;worker.execute(()->{try{SnapshotAnalysis old=SnapshotAnalysis.load(before,requested);SnapshotDiff diff=SnapshotDiff.between(old,current);StringBuilder body=new StringBuilder(t("Selected copy → current copy. Both use the same render settings; only decoded coverage is compared. Missing chunks do not prove removed terrain.\n\n","Gewählte Kopie → aktuelle Kopie. Beide verwenden dieselben Rendereinstellungen; nur gelesene Bereiche werden verglichen. Fehlende Chunks beweisen kein entferntes Gelände.\n\n"));
        body.append(t("Changed surface columns: ","Geänderte Kartenspalten: ")).append(diff.changedColumns).append(t("\nChunks only in current / selected: ","\nChunks nur aktuell / gewählt: ")).append(diff.addedChunks).append(" / ").append(diff.missingChunks).append(t("\nChanged readable chests: ","\nGeänderte lesbare Truhen: ")).append(diff.changedChests).append(t("\nUnmatched / unreadable chests: ","\nNicht zugeordnete / unlesbare Truhen: ")).append(diff.unknownChests);
        body.append(t("\n\nInventory quantity changes\n","\n\nInventar-Mengenänderungen\n"));if(old.player==null||current.player==null)body.append(t("Unavailable","Nicht verfügbar"));else if(diff.inventory.isEmpty())body.append(t("No quantity changes","Keine Mengenänderungen"));else for(Map.Entry<Integer,Long> e:diff.inventory.entrySet())body.append(catalog.name(e.getKey(),german)).append(" ").append(e.getValue()>0?"+":"").append(e.getValue()).append("\n");
        int shown=0;for(SnapshotDiff.ChestChange change:diff.chestChanges){if(shown++>=100){body.append(t("\nFirst 100 changed chests shown.","\nErste 100 geänderte Truhen angezeigt."));break;}MapPoint p=change.point;body.append("\n\n").append(p.dimension==0?"Overworld":"Nether").append(" · X ").append(p.x).append(" · Y ").append(p.y).append(" · Z ").append(p.z);if(change.propertiesChanged)body.append(t("\nItem properties changed; quantities unchanged.","\nGegenstandseigenschaften geändert; Mengen unverändert."));for(Map.Entry<Integer,Long> e:change.quantities.entrySet())body.append("\n").append(catalog.name(e.getKey(),german)).append(" ").append(e.getValue()>0?"+":"").append(e.getValue());}
        body.append("\n").append(t("Unreadable chunks (selected/current): ","Unlesbare Chunks (gewählt/aktuell): ")).append(old.skippedChunks).append(" / ").append(current.skippedChunks);if(old.limited||current.limited||old.pointsLimited||current.pointsLimited)body.append(t("\nLimited coverage: render or point limit reached.","\nBegrenzte Abdeckung: Render- oder Punktlimit erreicht."));String report=body.toString();main.post(()->{if(destroyed)return;LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);text(form,report,16);ScrollView scroll=new ScrollView(this);scroll.addView(form);featureDialog=new AlertDialog.Builder(this).setTitle(t("Snapshot comparison","Spielstandsvergleich")).setView(scroll).setPositiveButton(android.R.string.ok,null).show();status.setText(snapshot.name);});
        }catch(Exception e){main.post(()->{if(!destroyed)status.setText(e.getMessage());});}});}
    void packingList(){
        ExplorationPlans plans=new ExplorationPlans(preferences);JSONArray list=plans.read("packing");ChestStock stock=ChestStock.from(analysis.points);LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        text(form,t("Target amounts versus readable chests in the loaded area. Inventory is excluded. Unknown chests: ","Zielmengen gegenüber lesbaren Truhen im geladenen Bereich. Inventar nicht eingerechnet. Unlesbare Truhen: ")+stock.unknown+(analysis.limited||analysis.pointsLimited?t(" · coverage limited"," · Abdeckung begrenzt"):""),14);
        if(list.length()==0)text(form,t("No items yet. Add an item and its target quantity.","Noch keine Gegenstände. Gegenstand und Zielmenge hinzufügen."),16);
        for(int i=0;i<list.length();i++)try{JSONObject row=list.getJSONObject(i);int id=row.getInt("item");long needed=row.getLong("needed");
            button(form,catalog.name(id,german)+" · "+stock.available(id)+" / "+needed+t(" available / target; missing "," vorhanden / Ziel; fehlend ")+stock.missing(id,needed),()->{
                featureDialog.dismiss();featureDialog=new AlertDialog.Builder(this).setTitle(catalog.name(id,german)).setItems(new String[]{t("Find in chests","In Truhen finden"),t("Change target","Ziel ändern"),t("Remove from list","Aus Liste entfernen")},(d,n)->{try{if(n==0){search(true);((EditText)searchDialog.findViewById(SEARCH_QUERY)).setText(Integer.toString(id));}else if(n==1)packingItem(id,needed);else{plans.remove("packing",Integer.toString(id));packingList();}}catch(JSONException e){status.setText(e.getMessage());}}).show();
            });
        }catch(JSONException ignored){}
        ScrollView scroll=new ScrollView(this);scroll.addView(form);featureDialog=new AlertDialog.Builder(this).setTitle(t("Packing list","Packliste")).setView(scroll).setPositiveButton(t("Add item","Gegenstand hinzufügen"),(d,w)->choosePackingItem()).setNegativeButton(android.R.string.cancel,null).show();
    }
    private void choosePackingItem(){
        ChestStock stock=ChestStock.from(analysis.points);List<Integer> ids=new ArrayList<>(stock.quantities.keySet());ids.sort(Comparator.comparing(id->catalog.name(id,german)));String[] labels=new String[ids.size()];for(int i=0;i<ids.size();i++)labels[i]=catalog.name(ids.get(i),german)+" · "+stock.available(ids.get(i));
        featureDialog=new AlertDialog.Builder(this).setTitle(t("Choose from saved chest stock","Aus gespeichertem Truhenbestand wählen")).setItems(labels,(d,i)->packingItem(ids.get(i),64)).setNeutralButton(t("Enter item ID","Gegenstands-ID eingeben"),(d,w)->packingItem(0,64)).setNegativeButton(android.R.string.cancel,null).show();
    }
    void packingItem(int item,long needed){LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);EditText id=numberField(form,t("Item ID","Gegenstands-ID"),item);id.setId(R.id.plan_item);EditText count=numberField(form,t("Target quantity","Zielmenge"),(int)needed);count.setId(R.id.plan_quantity);
        featureDialog=new AlertDialog.Builder(this).setTitle(t("Packing target","Packziel")).setView(form).setNegativeButton(android.R.string.cancel,null).setPositiveButton(t("Save","Speichern"),null).create();featureDialog.show();featureDialog.getButton(-1).setOnClickListener(v->{try{int itemId=Integer.parseInt(id.getText().toString()),quantity=Integer.parseInt(count.getText().toString());if(itemId<=0||itemId>65535||quantity<1||quantity>1000000)throw new IllegalArgumentException(t("ID 1–65535; quantity 1–1000000","ID 1–65535; Menge 1–1000000"));new ExplorationPlans(preferences).put("packing",new JSONObject().put("id",Integer.toString(itemId)).put("item",itemId).put("needed",quantity),100);featureDialog.dismiss();packingList();}catch(Exception e){count.setError(e.getMessage());}});
    }
    void bookmarks(){ExplorationPlans plans=new ExplorationPlans(preferences);JSONArray rows=plans.read("bookmarks");String[] labels=new String[rows.length()];for(int i=0;i<rows.length();i++)labels[i]=rows.optJSONObject(i).optString("name");
        featureDialog=new AlertDialog.Builder(this).setTitle(t("Map bookmarks","Karten-Lesezeichen")).setItems(labels,(d,i)->{JSONObject row=rows.optJSONObject(i);featureDialog=new AlertDialog.Builder(this).setTitle(row.optString("name")).setItems(new String[]{t("Open saved view","Gespeicherte Ansicht öffnen"),t("Delete bookmark","Lesezeichen löschen")},(a,n)->{try{if(n==1){plans.remove("bookmarks",row.getString("id"));bookmarks();return;}rememberMap();activeMap=null;dimension=row.getInt("dimension");viewports[dimension]=new double[]{row.getDouble("x"),row.getDouble("z"),row.getDouble("zoom")};options=new MapOptions(row.getInt("chunks"),row.getInt("ceiling"),row.getInt("radius"),row.getInt("centerX"),row.getInt("centerZ"),row.getBoolean("slice"));textures=row.getBoolean("textures");pointKind=row.getString("filter");preferences.edit().putInt("chunks",options.chunks).putInt("ceiling",options.ceiling).putInt("radius",options.radius).putInt("centerX",options.centerX).putInt("centerZ",options.centerZ).putBoolean("slice",options.slice).putBoolean("textures",textures).apply();reloadMap();}catch(JSONException e){status.setText(e.getMessage());}}).show();}).setPositiveButton(t("Save current view","Aktuelle Ansicht speichern"),(d,w)->saveBookmark()).setNegativeButton(android.R.string.cancel,null).show();
    }
    void saveBookmark(){EditText name=new EditText(this);name.setId(R.id.bookmark_name);name.setHint(t("Name","Name"));name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});featureDialog=new AlertDialog.Builder(this).setTitle(t("Save map bookmark","Karten-Lesezeichen speichern")).setView(name).setNegativeButton(android.R.string.cancel,null).setPositiveButton(t("Save","Speichern"),null).create();featureDialog.show();featureDialog.getButton(-1).setOnClickListener(v->{try{String label=name.getText().toString().trim();if(label.isEmpty()){name.setError(t("Enter a name","Namen eingeben"));return;}double[] view=activeMap.viewport();if(view==null)throw new IllegalStateException(t("Map is not ready","Karte noch nicht bereit"));new ExplorationPlans(preferences).put("bookmarks",new JSONObject().put("id",UUID.randomUUID().toString()).put("name",label).put("x",view[0]).put("z",view[1]).put("zoom",view[2]).put("dimension",dimension).put("chunks",options.chunks).put("ceiling",options.ceiling).put("radius",options.radius).put("centerX",options.centerX).put("centerZ",options.centerZ).put("slice",options.slice).put("textures",textures).put("filter",pointKind),30);featureDialog.dismiss();bookmarks();}catch(Exception e){name.setError(e.getMessage());}});}
    void mapSettings(){
        ScrollView scroll=new ScrollView(this);LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),dp(8),dp(18),dp(16));scroll.addView(form);
        text(form,t("Display style","Darstellung"),18);Spinner style=spinner(form,new String[]{t("Companion colors","Companion-Farben"),"Kenney Voxel Pack"});style.setSelection(textures?1:0);
        text(form,t("Kenney CC0 textures appear when zoomed in. Unmapped blocks keep their colors. Source: kenney.nl/assets/voxel-pack","Kenney-CC0-Texturen erscheinen beim Hineinzoomen. Nicht zugeordnete Blöcke behalten ihre Farben. Quelle: kenney.nl/assets/voxel-pack"),13);
        Switch layer=new Switch(this);layer.setText(t("Exact layer / caves","Exakte Ebene / Höhlen"));layer.setChecked(options.slice);form.addView(layer);
        text(form,t("Air on the selected layer stays blank; the surface below it is not substituted.","Luft auf der gewählten Ebene bleibt leer; die Oberfläche darunter wird nicht eingesetzt."),13);
        text(form,t("Maximum chunks","Maximale Chunks"),18);int[] counts={256,1024,4096,16384};Spinner count=spinner(form,new String[]{"256 · 64 MiB","1,024 · 64 MiB","4,096 · 256 MiB","16,384 · 512 MiB"});for(int i=0;i<counts.length;i++)if(options.chunks==counts[i])count.setSelection(i);
        text(form,t("Area around X/Z (square half-width)","Bereich um X/Z (halbe Quadratbreite)"),18);int[] radii={0,64,128,256,512};Spinner radius=spinner(form,new String[]{t("Whole saved area","Gesamter gespeicherter Bereich"),"±64", "±128","±256","±512"});for(int i=0;i<radii.length;i++)if(options.radius==radii[i])radius.setSelection(i);
        EditText x=numberField(form,"X",options.centerX),z=numberField(form,"Z",options.centerZ);
        TextView yLabel=text(form,t("Highest rendered block: Y ","Höchster dargestellter Block: Y ")+options.ceiling,16);SeekBar y=new SeekBar(this);y.setMax(255);y.setProgress(options.ceiling);form.addView(y);y.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int v,boolean u){yLabel.setText("Y ≤ "+v);}});
        text(form,t("Applies to this device. More chunks need more memory and time. Only saved chunks are available; reaching a limit is reported. Points include all heights within the rendered area.","Gilt auf diesem Gerät. Mehr Chunks benötigen mehr Speicher und Zeit. Es sind nur gespeicherte Chunks verfügbar; erreichte Limits werden angezeigt. Punkte umfassen alle Höhen im gewählten Bereich."),13);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(t("Map settings","Karteneinstellungen")).setView(scroll).setNegativeButton(android.R.string.cancel,null).setPositiveButton(t("Render","Rendern"),null).create();dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            MapOptions next=new MapOptions(counts[count.getSelectedItemPosition()],y.getProgress(),radii[radius.getSelectedItemPosition()],Integer.parseInt(x.getText().toString()),Integer.parseInt(z.getText().toString()),layer.isChecked());
            options=next;textures=style.getSelectedItemPosition()==1;preferences.edit().putInt("chunks",next.chunks).putInt("ceiling",next.ceiling).putInt("radius",next.radius).putInt("centerX",next.centerX).putInt("centerZ",next.centerZ).putBoolean("textures",textures).putBoolean("slice",next.slice).apply();dialog.dismiss();reloadMap();
        }catch(NumberFormatException e){x.setError(t("Enter whole-number X/Z coordinates","Ganzzahlige X/Z-Koordinaten eingeben"));}});
    }
    private Spinner spinner(LinearLayout p,String[] labels){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));p.addView(s);return s;}
    private EditText numberField(LinearLayout p,String label,int value){text(p,label,14);EditText e=new EditText(this);e.setId(label.equals("X")?R.id.coordinate_x:label.equals("Y")?R.id.coordinate_y:R.id.coordinate_z);e.setHint(label);e.setContentDescription(label);e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);e.setText(Integer.toString(value));p.addView(e);return e;}
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
    private Button button(LinearLayout parent,String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setMinHeight(dp(52));b.setFocusable(true);b.setOnClickListener(v->action.run());parent.addView(b);return b;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onSaveInstanceState(Bundle b){rememberMap();b.putBoolean("largeMap",largeMap);b.putBoolean("showTools",showTools);for(int i=0;i<2;i++)b.putDoubleArray("viewport"+i,viewports[i]);b.putString("tab",selected);b.putInt("dimension",dimension);super.onSaveInstanceState(b);}
    @Override protected void onDestroy(){destroyed=true;worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
