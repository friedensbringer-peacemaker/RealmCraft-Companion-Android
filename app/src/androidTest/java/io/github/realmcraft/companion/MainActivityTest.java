package io.github.realmcraft.companion;

import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import io.github.realmcraft.companion.core.SnapshotStore;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/** Run on a disposable Android emulator; creates only the built-in synthetic sample. */
@SuppressWarnings("deprecation")
public final class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public MainActivityTest() { super(MainActivity.class); }

    public void testSampleImportSurvivesActivityRelaunch() throws Exception {
        MainActivity first = getActivity();
        awaitReady(first);
        SnapshotStore library = new SnapshotStore(new File(first.getFilesDir(), "snapshots"));
        int before = library.list().size();
        getInstrumentation().runOnMainSync(() -> {
            Button demo = first.findViewById(MainActivity.DEMO_BUTTON);
            assertTrue("Sample button should accept a click", demo.performClick());
        });
        long deadline = System.currentTimeMillis() + 15000;
        while (library.list().size() == before && System.currentTimeMillis() < deadline) Thread.sleep(50);
        assertEquals("Sample should create exactly one independent snapshot", before + 1, library.list().size());
        awaitReady(first);
        awaitLibraryText(first, "Synthetic test world");
        getInstrumentation().waitForIdleSync();
        // Successful import opens a details dialog. Back dismisses it without leaving the activity.
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        getInstrumentation().waitForIdleSync();
        assertFalse("Back should close the detail dialog", first.isFinishing());
        getInstrumentation().runOnMainSync(first::finish);
        getInstrumentation().waitForIdleSync();
        setActivity(null);
        MainActivity reopened = getActivity();
        assertNotSame(first, reopened);
        awaitReady(reopened);
        awaitLibraryText(reopened, "Synthetic test world");
        assertEquals("Relaunch should preserve the imported snapshot", before + 1,
            new SnapshotStore(new File(reopened.getFilesDir(), "snapshots")).list().size());
    }

    private void awaitReady(MainActivity activity) throws Exception {
        awaitUi(() -> {
            Button demo = activity.findViewById(MainActivity.DEMO_BUTTON);
            return demo != null && demo.isEnabled();
        }, "Activity did not finish loading its local library");
    }
    private void awaitLibraryText(MainActivity activity, String text) throws Exception {
        awaitUi(() -> containsText(activity.findViewById(MainActivity.LIBRARY_VIEW), text),
            "Imported synthetic world was not displayed in the library");
    }
    private interface UiCondition { boolean isTrue(); }
    private void awaitUi(UiCondition condition, String failure) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        AtomicBoolean success = new AtomicBoolean();
        do {
            getInstrumentation().runOnMainSync(() -> success.set(condition.isTrue()));
            if (success.get()) return;
            Thread.sleep(50);
        } while (System.currentTimeMillis() < deadline);
        fail(failure);
    }
    private static boolean containsText(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (containsText(group.getChildAt(i), expected)) return true;
        }
        return false;
    }
}
