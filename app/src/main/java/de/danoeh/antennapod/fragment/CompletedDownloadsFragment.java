package de.danoeh.antennapod.fragment;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.SpeedDialView;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.adapter.actionbutton.DeleteActionButton;
import de.danoeh.antennapod.core.event.DownloadEvent;
import de.danoeh.antennapod.core.event.DownloadLogEvent;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.core.menuhandler.MenuItemUtils;
import de.danoeh.antennapod.fragment.actions.EpisodeMultiSelectActionHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.core.service.download.DownloadService;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.view.EmptyViewHandler;
import de.danoeh.antennapod.view.EpisodeItemListRecyclerView;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays all completed downloads and provides a button to delete them.
 */
public class CompletedDownloadsFragment extends Fragment implements
        EpisodeItemListAdapter.OnSelectModeListener {

    private static final String TAG = CompletedDownloadsFragment.class.getSimpleName();

    private List<FeedItem> items = new ArrayList<>();
    private CompletedDownloadsListAdapter adapter;
    private EpisodeItemListRecyclerView recyclerView;
    private ProgressBar progressBar;
    private Disposable disposable;
    private EmptyViewHandler emptyView;

    private boolean isUpdatingFeeds = false;

    private SpeedDialView speedDialView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.simple_list_fragment, container, false);
        Toolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setVisibility(View.GONE);

        recyclerView = root.findViewById(R.id.recyclerView);
        //recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        adapter = new CompletedDownloadsListAdapter(new MainActivity());//(MainActivity) getActivity());
        adapter.setOnSelectModeListener(this);
        recyclerView.setAdapter(adapter);
        progressBar = root.findViewById(R.id.progLoading);

        speedDialView = root.findViewById(R.id.fabSD);
        speedDialView.setOverlayLayout(root.findViewById(R.id.fabSDOverlay));
        speedDialView.inflate(R.menu.episodes_apply_action_speeddial);
        speedDialView.removeActionItemById(R.id.download_batch);
        speedDialView.removeActionItemById(R.id.mark_read_batch);
        speedDialView.removeActionItemById(R.id.mark_unread_batch);
        speedDialView.removeActionItemById(R.id.remove_from_queue_batch);
        speedDialView.setOnChangeListener(new SpeedDialView.OnChangeListener() {
            @Override
            public boolean onMainActionSelected() {
                return false;
            }

            @Override
            public void onToggleChanged(boolean open) {
                if (open && adapter.getSelectedCount() == 0) {
                    /*((MainActivity) getActivity()).showSnackbarAbovePlayer(R.string.no_items_selected,
                            Snackbar.LENGTH_SHORT);
                    speedDialView.close();*/
                }
            }
        });
        speedDialView.setOnActionSelectedListener(actionItem -> {
            /*new EpisodeMultiSelectActionHandler(((MainActivity) getActivity()), adapter.getSelectedItems())
                    .handleAction(actionItem.getId());
            adapter.endSelectMode();*/
            return true;
        });

        addEmptyView();
        EventBus.getDefault().register(this);
        return root;
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        adapter.endSelectMode();
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.clear_logs_item).setVisible(false);
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext());
            return true;
        }
        return false;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            ((PagedToolbarFragment) getParentFragment()).invalidateOptionsMenuIfActive(this);
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFeeds();

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        FeedItem selectedItem = adapter.getLongPressedItem();
        if (selectedItem == null) {
            Log.i(TAG, "Selected item at current position was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        if (adapter.onContextItemSelected(item)) {
            return true;
        }

        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    private void addEmptyView() {
        emptyView = new EmptyViewHandler(getActivity());
        emptyView.setIcon(R.drawable.ic_download);
        emptyView.setTitle(R.string.no_comp_downloads_head_label);
        emptyView.setMessage(R.string.no_comp_downloads_label);
        emptyView.attachToRecyclerView(recyclerView);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        if (items == null) {
            return;
        } else if (adapter == null) {
            loadItems();
            return;
        }
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemUtil.indexOfItemWithId(items, item.getId());
            if (pos >= 0) {
                items.remove(pos);
                if (item.getMedia().isDownloaded()) {
                    items.add(pos, item);
                    adapter.notifyItemChangedCompat(pos);
                } else {
                    adapter.notifyItemRemoved(pos);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadLogChanged(DownloadLogEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadItems();
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyView.hide();
        disposable = Observable.fromCallable(DBReader::getDownloadedItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    items = result;
                    adapter.updateItems(result);
                    ((PagedToolbarFragment) getParentFragment()).invalidateOptionsMenuIfActive(this);
                    progressBar.setVisibility(View.GONE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    public void onStartSelectMode() {
        speedDialView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onEndSelectMode() {
        speedDialView.close();
        speedDialView.setVisibility(View.GONE);
    }

    private static class CompletedDownloadsListAdapter extends EpisodeItemListAdapter {

        public CompletedDownloadsListAdapter(MainActivity mainActivity) {
            super(mainActivity);
        }

        @Override
        public void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
            if (!inActionMode()) {
                DeleteActionButton actionButton = new DeleteActionButton(getItem(pos));
                actionButton.configure(holder.secondaryActionButton, holder.secondaryActionIcon, new Context() {
                    @Override
                    public AssetManager getAssets() {
                        return null;
                    }

                    @Override
                    public Resources getResources() {
                        return null;
                    }

                    @Override
                    public PackageManager getPackageManager() {
                        return null;
                    }

                    @Override
                    public ContentResolver getContentResolver() {
                        return null;
                    }

                    @Override
                    public Looper getMainLooper() {
                        return null;
                    }

                    @Override
                    public Context getApplicationContext() {
                        return null;
                    }

                    @Override
                    public void setTheme(int resid) {

                    }

                    @Override
                    public Resources.Theme getTheme() {
                        return null;
                    }

                    @Override
                    public ClassLoader getClassLoader() {
                        return null;
                    }

                    @Override
                    public String getPackageName() {
                        return null;
                    }

                    @Override
                    public ApplicationInfo getApplicationInfo() {
                        return null;
                    }

                    @Override
                    public String getPackageResourcePath() {
                        return null;
                    }

                    @Override
                    public String getPackageCodePath() {
                        return null;
                    }

                    @Override
                    public SharedPreferences getSharedPreferences(String name, int mode) {
                        return null;
                    }

                    @Override
                    public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
                        return false;
                    }

                    @Override
                    public boolean deleteSharedPreferences(String name) {
                        return false;
                    }

                    @Override
                    public FileInputStream openFileInput(String name) throws FileNotFoundException {
                        return null;
                    }

                    @Override
                    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
                        return null;
                    }

                    @Override
                    public boolean deleteFile(String name) {
                        return false;
                    }

                    @Override
                    public File getFileStreamPath(String name) {
                        return null;
                    }

                    @Override
                    public File getDataDir() {
                        return null;
                    }

                    @Override
                    public File getFilesDir() {
                        return null;
                    }

                    @Override
                    public File getNoBackupFilesDir() {
                        return null;
                    }

                    @Nullable
                    @Override
                    public File getExternalFilesDir(@Nullable String type) {
                        return null;
                    }

                    @Override
                    public File[] getExternalFilesDirs(String type) {
                        return new File[0];
                    }

                    @Override
                    public File getObbDir() {
                        return null;
                    }

                    @Override
                    public File[] getObbDirs() {
                        return new File[0];
                    }

                    @Override
                    public File getCacheDir() {
                        return null;
                    }

                    @Override
                    public File getCodeCacheDir() {
                        return null;
                    }

                    @Nullable
                    @Override
                    public File getExternalCacheDir() {
                        return null;
                    }

                    @Override
                    public File[] getExternalCacheDirs() {
                        return new File[0];
                    }

                    @Override
                    public File[] getExternalMediaDirs() {
                        return new File[0];
                    }

                    @Override
                    public String[] fileList() {
                        return new String[0];
                    }

                    @Override
                    public File getDir(String name, int mode) {
                        return null;
                    }

                    @Override
                    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
                        return null;
                    }

                    @Override
                    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, @Nullable DatabaseErrorHandler errorHandler) {
                        return null;
                    }

                    @Override
                    public boolean moveDatabaseFrom(Context sourceContext, String name) {
                        return false;
                    }

                    @Override
                    public boolean deleteDatabase(String name) {
                        return false;
                    }

                    @Override
                    public File getDatabasePath(String name) {
                        return null;
                    }

                    @Override
                    public String[] databaseList() {
                        return new String[0];
                    }

                    @Override
                    public Drawable getWallpaper() {
                        return null;
                    }

                    @Override
                    public Drawable peekWallpaper() {
                        return null;
                    }

                    @Override
                    public int getWallpaperDesiredMinimumWidth() {
                        return 0;
                    }

                    @Override
                    public int getWallpaperDesiredMinimumHeight() {
                        return 0;
                    }

                    @Override
                    public void setWallpaper(Bitmap bitmap) throws IOException {

                    }

                    @Override
                    public void setWallpaper(InputStream data) throws IOException {

                    }

                    @Override
                    public void clearWallpaper() throws IOException {

                    }

                    @Override
                    public void startActivity(Intent intent) {

                    }

                    @Override
                    public void startActivity(Intent intent, @Nullable Bundle options) {

                    }

                    @Override
                    public void startActivities(Intent[] intents) {

                    }

                    @Override
                    public void startActivities(Intent[] intents, Bundle options) {

                    }

                    @Override
                    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {

                    }

                    @Override
                    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {

                    }

                    @Override
                    public void sendBroadcast(Intent intent) {

                    }

                    @Override
                    public void sendBroadcast(Intent intent, @Nullable String receiverPermission) {

                    }

                    @Override
                    public void sendOrderedBroadcast(Intent intent, @Nullable String receiverPermission) {

                    }

                    @Override
                    public void sendOrderedBroadcast(@NonNull Intent intent, @Nullable String receiverPermission, @Nullable BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

                    }

                    @Override
                    public void sendBroadcastAsUser(Intent intent, UserHandle user) {

                    }

                    @Override
                    public void sendBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission) {

                    }

                    @Override
                    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

                    }

                    @Override
                    public void sendStickyBroadcast(Intent intent) {

                    }

                    @Override
                    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

                    }

                    @Override
                    public void removeStickyBroadcast(Intent intent) {

                    }

                    @Override
                    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {

                    }

                    @Override
                    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

                    }

                    @Override
                    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {

                    }

                    @Nullable
                    @Override
                    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter, int flags) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler, int flags) {
                        return null;
                    }

                    @Override
                    public void unregisterReceiver(BroadcastReceiver receiver) {

                    }

                    @Nullable
                    @Override
                    public ComponentName startService(Intent service) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public ComponentName startForegroundService(Intent service) {
                        return null;
                    }

                    @Override
                    public boolean stopService(Intent service) {
                        return false;
                    }

                    @Override
                    public boolean bindService(Intent service, @NonNull ServiceConnection conn, int flags) {
                        return false;
                    }

                    @Override
                    public void unbindService(@NonNull ServiceConnection conn) {

                    }

                    @Override
                    public boolean startInstrumentation(@NonNull ComponentName className, @Nullable String profileFile, @Nullable Bundle arguments) {
                        return false;
                    }

                    @Override
                    public Object getSystemService(@NonNull String name) {
                        return null;
                    }

                    @Nullable
                    @Override
                    public String getSystemServiceName(@NonNull Class<?> serviceClass) {
                        return null;
                    }

                    @Override
                    public int checkPermission(@NonNull String permission, int pid, int uid) {
                        return PackageManager.PERMISSION_DENIED;
                    }

                    @Override
                    public int checkCallingPermission(@NonNull String permission) {
                        return PackageManager.PERMISSION_DENIED;
                    }

                    @Override
                    public int checkCallingOrSelfPermission(@NonNull String permission) {
                        return PackageManager.PERMISSION_DENIED;
                    }

                    @Override
                    public int checkSelfPermission(@NonNull String permission) {
                        return PackageManager.PERMISSION_DENIED;
                    }

                    @Override
                    public void enforcePermission(@NonNull String permission, int pid, int uid, @Nullable String message) {

                    }

                    @Override
                    public void enforceCallingPermission(@NonNull String permission, @Nullable String message) {

                    }

                    @Override
                    public void enforceCallingOrSelfPermission(@NonNull String permission, @Nullable String message) {

                    }

                    @Override
                    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {

                    }

                    @Override
                    public void revokeUriPermission(Uri uri, int modeFlags) {

                    }

                    @Override
                    public void revokeUriPermission(String toPackage, Uri uri, int modeFlags) {

                    }

                    @Override
                    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
                        return PackageManager.PERMISSION_DENIED;
                    }

                    @Override
                    public int checkCallingUriPermission(Uri uri, int modeFlags) {
                        return PackageManager.PERMISSION_DENIED;
                    }

                    @Override
                    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
                        return PackageManager.PERMISSION_DENIED;
                    }

                    @Override
                    public int checkUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags) {
                        return PackageManager.PERMISSION_DENIED;
                    }

                    @Override
                    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {

                    }

                    @Override
                    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {

                    }

                    @Override
                    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {

                    }

                    @Override
                    public void enforceUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags, @Nullable String message) {

                    }

                    @Override
                    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
                        return null;
                    }

                    @Override
                    public Context createContextForSplit(String splitName) throws PackageManager.NameNotFoundException {
                        return null;
                    }

                    @Override
                    public Context createConfigurationContext(@NonNull Configuration overrideConfiguration) {
                        return null;
                    }

                    @Override
                    public Context createDisplayContext(@NonNull Display display) {
                        return null;
                    }

                    @Override
                    public Context createDeviceProtectedStorageContext() {
                        return null;
                    }

                    @Override
                    public boolean isDeviceProtectedStorage() {
                        return false;
                    }
                });//getActivity());
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            if (!inActionMode()) {
                menu.findItem(R.id.multi_select).setVisible(true);
            }
        }
    }
}
