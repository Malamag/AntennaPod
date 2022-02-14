package de.danoeh.antennapod.core.service.download.handler;

import android.content.Context;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.DownloadStatus;
import de.danoeh.antennapod.core.storage.DBTasks;

public class FeedSyncTask {
    private static final String TAG = "FeedParserTask";
    private final DownloadRequest request;
    private final Context context;
    private DownloadStatus downloadStatus;
    private Feed savedFeed;

    public FeedSyncTask(Context context, DownloadRequest request) {
        this.request = request;
        this.context = context;
    }

    public boolean run() {
        FeedParserTask task = new FeedParserTask(request);

        downloadStatus = task.getDownloadStatus();

        if (!task.isSuccessful()) {
            return false;
        }


        return true;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public Feed getSavedFeed() {
        return savedFeed;
    }
}
