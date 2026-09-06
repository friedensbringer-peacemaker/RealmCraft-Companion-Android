package io.github.realmcraft.companion;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import io.github.realmcraft.companion.core.SnapshotStore;
import io.github.realmcraft.companion.core.DemoDownload;
import io.github.realmcraft.companion.core.SnapshotStore.Snapshot;
import org.json.*;
import rikka.shizuku.Shizuku;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Small native 2D panel; all archive and device operations stay off the UI thread. */
public final class MainActivity extends Activity {
    public static final int DEMO_BUTTON = 1001, ZIP_BUTTON = 1002, LIBRARY_VIEW = 1003, STATUS_VIEW = 1004, GITHUB_DEMO_BUTTON = 1005, REPOSITORY_BUTTON = 1006;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Button> actions = new ArrayList<>();
    private SnapshotStore store;
    private LinearLayout page, library;
    private TextView status, bridgeStatus;
    private ProgressBar progress;
    private volatile InputStream activeInput;
    private IWorldAccess bridge;
    private boolean busy, binding;
    private volatile boolean destroyed;
    private final Shizuku.UserServiceArgs serviceArgs = new Shizuku.UserServiceArgs(
        new ComponentName("io.github.realmcraft.companion", WorldAccessService.class.getName()))
        .daemon(false).processNameSuffix("world-reader").version(1);
    private final Shizuku.OnBinderReceivedListener received = () -> main.post(this::updateBridgeStatus);
    private final Shizuku.OnBinderDeadListener died = () -> main.post(() -> {
        bridge = null; binding = false; updateBridgeStatus();
    });
    private final Shizuku.OnRequestPermissionResultListener permission = (request, grant) -> main.post(() -> {
        if (request != 9 || destroyed) return;
        if (grant == PackageManager.PERMISSION_GRANTED) bindBridge();
        else message(tr("Shizuku permission was denied.", "Shizuku-Zugriff wurde abgelehnt."));
    });
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            main.post(() -> {
                if (destroyed) {
                    try { Shizuku.unbindUserService(serviceArgs, connection, true); } catch (Exception ignored) { }
                    return;
                }
                binding = false; bridge = IWorldAccess.Stub.asInterface(binder); updateBridgeStatus();
            });
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            main.post(() -> { binding = false; bridge = null; updateBridgeStatus(); });
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        buildUi();
        Shizuku.addBinderReceivedListenerSticky(received);
        Shizuku.addBinderDeadListener(died);
        Shizuku.addRequestPermissionResultListener(permission);
        runWork(tr("Opening library…", "Bibliothek wird geöffnet…"), () -> {
            store = new SnapshotStore(new File(getFilesDir(), "snapshots"));
            return store.list();
        }, this::showLibrary);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(16, 26, 25));
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(20), dp(24), dp(32));
        scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
        addText(page, "REALMCRAFT / COMPANION", 13, Color.rgb(169, 232, 188), true);
        addText(page, tr("Your world, a window away.", "Deine Welt. Ein Fenster entfernt."), 29, Color.WHITE, true);
        addText(page, tr("Android + Quest · Prototype ", "Android + Quest · Prototyp ") + BuildConfig.VERSION_NAME, 15, 0xffaac1b8, false);
        LinearLayout intro = card(page);
        addText(intro, tr("Test the companion panel", "Companion-Fenster testen"), 21, Color.WHITE, true);
        addText(intro, tr("Import a copy, inspect its metadata and file list, then reopen it beside your game. Nothing is written back to RealmCraft.",
            "Kopie importieren, Metadaten und Dateiliste ansehen und neben dem Spiel wieder öffnen. RealmCraft-Dateien werden nicht verändert."), 16, 0xffd0ddd7, false);
        Button demo = button(intro, tr("Try synthetic sample", "Synthetische Testwelt öffnen"), () -> runWork(tr("Creating sample…", "Testwelt wird erstellt…"), () -> store.createDemo(), this::imported));
        demo.setId(DEMO_BUTTON);
        Button zip = button(intro, tr("Import world ZIP", "Welt-ZIP importieren"), this::chooseZip); zip.setId(ZIP_BUTTON);
        button(intro, tr("Download RealmCraft Companion Demo · 3.4 MB", "RealmCraft Companion Demo laden · 3,4 MB"), () ->
            runWork(tr("Downloading and verifying demo…", "Demo wird geladen und geprüft…"), () -> {
                byte[] archive = DemoDownload.download();
                if (destroyed) throw new InterruptedIOException("Download cancelled.");
                return store.importZip(new ByteArrayInputStream(archive));
            }, this::imported)).setId(GITHUB_DEMO_BUTTON);
        button(intro, tr("GitHub project & downloads", "GitHub-Projekt & Downloads"), () -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DemoDownload.REPOSITORY))); }
            catch (ActivityNotFoundException e) { message(tr("No browser is available.", "Kein Browser verfügbar.")); }
        }).setId(REPOSITORY_BUTTON);
        addText(intro, tr("The shared demo downloads from GitHub only when tapped. SHA-256 is checked before importing an independent copy. No Shizuku is needed. You can inspect metadata and files here; this does not add the world to RealmCraft or render a map.",
            "Die freigegebene Demo wird erst beim Antippen von GitHub geladen. Vor dem Import einer eigenständigen Kopie wird SHA-256 geprüft. Shizuku ist nicht nötig. Hier kannst du Metadaten und Dateien ansehen; die Welt wird weder in RealmCraft eingesetzt noch als Karte dargestellt."), 14, 0xffaac1b8, false);

        addText(intro, tr("One world per ZIP · Up to 1 GiB · Version 9 metadata", "Eine Welt pro ZIP · Bis 1 GiB · Metadaten-Version 9"), 13, 0xffaac1b8, false);
        LinearLayout device = card(page);
        addText(device, tr("Direct Quest import · Experimental", "Direktimport auf Quest · Experimentell"), 20, Color.WHITE, true);
        addText(device, tr("On the Quest itself, an authorized Shizuku helper can test access to the Tellurion folder. Shizuku must be installed and started separately. On a phone this checks the phone, not a connected Quest.",
            "Auf der Quest selbst kann ein freigegebener Shizuku-Helfer den Tellurion-Ordner lesen. Shizuku muss separat installiert und gestartet sein. Auf dem Handy wird das Handy geprüft, keine verbundene Quest."), 15, 0xffd0ddd7, false);
        bridgeStatus = addText(device, "", 14, 0xffaac1b8, false);
        button(device, tr("Connect Shizuku", "Shizuku verbinden"), this::connectBridge);
        button(device, tr("Find worlds on this device", "Welten auf diesem Gerät suchen"), this::confirmScan);
        button(device, tr("Shizuku setup guide", "Shizuku-Einrichtung öffnen"), () -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/guide/setup/"))); }
            catch (ActivityNotFoundException e) { message(tr("No browser is available.", "Kein Browser verfügbar.")); }
        });
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setVisibility(View.GONE); page.addView(progress);
        status = addText(page, tr("Ready.", "Bereit."), 15, 0xffa9e8bc, false); status.setId(STATUS_VIEW);
        addText(page, tr("Your snapshots", "Deine Spielstand-Kopien"), 23, Color.WHITE, true);
        library = new LinearLayout(this); library.setOrientation(LinearLayout.VERTICAL); library.setId(LIBRARY_VIEW); page.addView(library);
        addText(page, tr("Offline storage · No account · No analytics\nMaps, inventory decoding, restore and live sync are not included in this prototype.",
            "Lokale Ablage · Kein Konto · Keine Analyse-Dienste\nKarten, Inventar-Auswertung, Wiederherstellung und Live-Synchronisierung sind in diesem Prototyp noch nicht enthalten."), 13, 0xffaac1b8, false);
        updateBridgeStatus();
    }

    private void chooseZip() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivityForResult(pick, 7); }
        catch (ActivityNotFoundException e) { message(tr("This device has no document picker. Use the sample or Shizuku import.", "Kein Dateiauswahldialog verfügbar. Nutze die Testwelt oder den Shizuku-Import.")); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 7 && result == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            runWork(tr("Importing and verifying ZIP…", "ZIP wird importiert und geprüft…"), () -> {
                activeInput = getContentResolver().openInputStream(uri);
                if (activeInput == null) throw new IOException("Cannot open selected document.");
                try {
                    if (Thread.currentThread().isInterrupted() || destroyed) throw new InterruptedIOException("Import cancelled.");
                    return store.importZip(activeInput);
                } finally { closeActiveInput(); }
            }, this::imported);
        }
    }
    private void imported(Snapshot snapshot) {
        runWork(tr("Snapshot saved.", "Spielstand-Kopie gespeichert."), () -> store.list(), rows -> {
            showLibrary(rows);
            message(tr("Imported: ", "Importiert: ") + snapshot.name);
            showSnapshot(snapshot);
        });
    }
    private void showLibrary(List<Snapshot> rows) {
        library.removeAllViews();
        if (rows.isEmpty()) addText(library, tr("No snapshots yet. Start with the sample above.", "Noch keine Kopien. Starte oben mit der Testwelt."), 16, 0xffaac1b8, false);
        for (Snapshot snapshot : rows) {
            LinearLayout item = card(library);
            addText(item, snapshot.name, 20, Color.WHITE, true);
            addText(item, (snapshot.synthetic ? tr("SYNTHETIC / NOT PLAYABLE", "SYNTHETISCH / NICHT SPIELBAR") : tr("IMPORTED SNAPSHOT", "IMPORTIERTE KOPIE"))
                + "\n" + snapshot.fileCount + tr(" files · ", " Dateien · ") + size(snapshot.bytes)
                + "\n" + date(snapshot.importedAt), 14, 0xffaac1b8, false);
            Button open = button(item, tr("Inspect snapshot", "Kopie ansehen"), () -> showSnapshot(snapshot));
            // Library actions are lightweight and need not stay in the persistent action registry.
            actions.remove(open);
        }
    }
    private void showSnapshot(Snapshot snapshot) {
        StringBuilder text = new StringBuilder();
        if (snapshot.synthetic) text.append(tr("Generated test data. Not a playable world.\n\n", "Generierte Testdaten. Keine spielbare Welt.\n\n"));
        text.append("World ID: ").append(snapshot.worldId).append("\nSeed: ").append(snapshot.seed)
            .append("\n").append(tr("Imported: ", "Importiert: ")).append(date(snapshot.importedAt))
            .append("\n").append(snapshot.fileCount).append(tr(" files · ", " Dateien · ")).append(size(snapshot.bytes))
            .append("\n\nSHA-256 (manifest):\n").append(snapshot.sha256).append("\n\n");
        int count = 0;
        for (String file : snapshot.files) {
            if (count++ == 150) { text.append(tr("… list limited to 150 files", "… Anzeige auf 150 Dateien begrenzt")); break; }
            text.append(file).append('\n');
        }
        TextView body = new TextView(this); body.setText(text); body.setTextSize(15); body.setTextIsSelectable(true); body.setPadding(dp(20), dp(12), dp(20), dp(12));
        ScrollView scroll = new ScrollView(this); scroll.addView(body);
        new AlertDialog.Builder(this).setTitle(snapshot.name).setView(scroll).setPositiveButton(android.R.string.ok, null).show();
    }
    private void connectBridge() {
        try {
            if (!Shizuku.pingBinder()) {
                message(tr("Start Shizuku first, then return and connect again.", "Starte zuerst Shizuku und verbinde dich danach erneut.")); return;
            }
            if (Shizuku.isPreV11()) throw new IOException("Shizuku 11 or newer is required.");
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) bindBridge();
            else if (Shizuku.shouldShowRequestPermissionRationale()) message(tr("Allow Companion in Shizuku's authorized apps, then retry.", "Erlaube den Companion in Shizukus App-Freigaben und versuche es erneut."));
            else Shizuku.requestPermission(9);
        } catch (Exception e) { failure(e); }
    }
    private void bindBridge() {
        if (bridge != null || binding) return;
        try {
            binding = true; updateBridgeStatus();
            Shizuku.bindUserService(serviceArgs, connection);
            main.postDelayed(() -> {
                if (!destroyed && binding && bridge == null) {
                    binding = false; updateBridgeStatus();
                    message(tr("Helper did not connect. Check Shizuku and retry.", "Helfer nicht verbunden. Prüfe Shizuku und versuche es erneut."));
                }
            }, 15000);
        } catch (Exception e) { binding = false; failure(e); updateBridgeStatus(); }
    }
    private void updateBridgeStatus() {
        if (destroyed || bridgeStatus == null) return;
        String label = bridge != null ? tr("Helper connected", "Helfer verbunden")
            : binding ? tr("Connecting…", "Verbindung wird hergestellt…")
            : tr("Helper not connected", "Helfer nicht verbunden");
        bridgeStatus.setText(label);
    }
    private void confirmScan() {
        if (bridge == null) { message(tr("Connect Shizuku first.", "Verbinde zuerst Shizuku.")); return; }
        new AlertDialog.Builder(this).setTitle(tr("Save and close RealmCraft first", "RealmCraft zuerst speichern und schließen"))
            .setMessage(tr("After you save in the game, continue here. The helper will force-stop RealmCraft and list its worlds. Keep the game closed until import completes.",
                "Speichere zuerst im Spiel und fahre dann hier fort. Der Helfer beendet RealmCraft und listet seine Welten auf. Lass das Spiel bis zum Ende des Imports geschlossen."))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(tr("Stop and list worlds", "Beenden und Welten auflisten"), (dialog, which) -> scan()).show();
    }
    private void scan() {
        final IWorldAccess service = bridge;
        runWork(tr("Looking for worlds…", "Welten werden gesucht…"), () -> {
            if (service == null) throw new IOException("Shizuku disconnected.");
            return new JSONArray(service.stopAndListWorlds());
        }, this::chooseWorld);
    }
    private void chooseWorld(JSONArray worlds) {
        if (worlds.length() == 0) { message(tr("No worlds found on this device.", "Keine Welten auf diesem Gerät gefunden.")); return; }
        try {
            String[] labels = new String[worlds.length()];
            for (int i = 0; i < worlds.length(); i++) {
                JSONObject world = worlds.getJSONObject(i);
                labels[i] = world.getString("name") + "\nID " + world.getString("id") + " · Seed " + world.getString("seed")
                    + "\n" + size(world.getLong("bytes")) + " · " + date(world.getLong("modified"));
            }
            new AlertDialog.Builder(this).setTitle(tr("Choose one world to copy", "Eine Welt zum Kopieren auswählen"))
                .setItems(labels, (dialog, which) -> {
                    try {
                        JSONObject world = worlds.getJSONObject(which);
                        new AlertDialog.Builder(this).setTitle(world.getString("name"))
                            .setMessage(tr("Import this world as a new local snapshot? RealmCraft will be stopped again before copying.", "Diese Welt als neue lokale Kopie importieren? RealmCraft wird vor dem Kopieren erneut beendet."))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(tr("Import copy", "Kopie importieren"), (d, w) -> importWorld(world.optString("id"))).show();
                    } catch (JSONException e) { failure(e); }
                }).setNegativeButton(android.R.string.cancel, null).show();
        } catch (JSONException e) { failure(e); }
    }
    private void importWorld(String id) {
        final IWorldAccess service = bridge;
        runWork(tr("Copying and verifying world… Keep RealmCraft closed.", "Welt wird kopiert und geprüft… RealmCraft geschlossen lassen."), () -> {
            if (service == null) throw new IOException("Shizuku disconnected.");
            ParcelFileDescriptor pipe = service.openWorldZip(id);
            if (pipe == null) throw new IOException("Helper returned no data.");
            activeInput = new ParcelFileDescriptor.AutoCloseInputStream(pipe);
            try {
                    if (Thread.currentThread().isInterrupted() || destroyed) throw new InterruptedIOException("Import cancelled.");
                    return store.importZip(activeInput);
                } finally { closeActiveInput(); }
        }, this::imported);
    }
    private interface Job<T> { T call() throws Exception; }
    private interface Result<T> { void accept(T result); }
    private <T> void runWork(String label, Job<T> job, Result<T> success) {
        if (busy || destroyed) return;
        setBusy(true); message(label);
        worker.execute(() -> {
            try {
                T result = job.call();
                main.post(() -> { if (!destroyed) { setBusy(false); success.accept(result); } });
            } catch (Exception error) {
                main.post(() -> { if (!destroyed) { setBusy(false); failure(error); } });
            }
        });
    }
    private void setBusy(boolean value) {
        busy = value;
        for (Button button : actions) button.setEnabled(!value);
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
    }
    private void failure(Exception error) { message(tr("Could not complete: ", "Nicht abgeschlossen: ") + error.getMessage()); }
    private void message(String text) { if (!destroyed) status.setText(text); }
    private void closeActiveInput() { InputStream stream = activeInput; activeInput = null; if (stream != null) try { stream.close(); } catch (IOException ignored) { } }
    @Override protected void onDestroy() {
        destroyed = true; closeActiveInput(); worker.shutdownNow(); main.removeCallbacksAndMessages(null);
        Shizuku.removeBinderReceivedListener(received); Shizuku.removeBinderDeadListener(died); Shizuku.removeRequestPermissionResultListener(permission);
        try { if (Shizuku.pingBinder()) Shizuku.unbindUserService(serviceArgs, connection, true); } catch (Exception ignored) { }
        super.onDestroy();
    }
    private String tr(String en, String de) { return Locale.getDefault().getLanguage().equals("de") ? de : en; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static String date(long time) { return time == 0 ? "?" : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(time)); }
    private static String size(long bytes) { return String.format(Locale.getDefault(), "%.2f MiB", bytes / 1048576.0); }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(18), dp(14), dp(18), dp(16));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(0xff1d302b); bg.setCornerRadius(dp(16)); card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(16); lp.bottomMargin = dp(12); parent.addView(card, lp); return card;
    }
    private TextView addText(LinearLayout parent, String text, int size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); view.setPadding(0, dp(5), 0, dp(7));
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(view); return view;
    }
    private Button button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextSize(16); button.setMinHeight(dp(52));
        button.setOnClickListener(view -> action.run()); parent.addView(button, new LinearLayout.LayoutParams(-1, -2)); actions.add(button); return button;
    }
}
