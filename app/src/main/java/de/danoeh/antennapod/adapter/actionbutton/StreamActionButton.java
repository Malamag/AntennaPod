package de.danoeh.antennapod.adapter.actionbutton;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import de.danoeh.antennapod.core.preferences.UsageStatistics;

import de.danoeh.antennapod.core.util.NetworkUtils;

import de.danoeh.antennapod.dialog.StreamingConfirmationDialog;

public class StreamActionButton extends ItemActionButton {

    public StreamActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    public int getLabel() {
        return R.string.stream_label;
    }

    @Override
    @DrawableRes
    public int getDrawable() {
        return R.drawable.ic_stream;
    }

    @Override
    public void onClick(Context context) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }
        UsageStatistics.logAction(UsageStatistics.ACTION_STREAM);

        if (!NetworkUtils.isStreamingAllowed()) {

            return;
        }



    }
}
