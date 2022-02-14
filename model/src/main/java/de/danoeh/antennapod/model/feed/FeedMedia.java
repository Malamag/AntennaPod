package de.danoeh.antennapod.model.feed;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;


import java.util.Date;
import java.util.List;

public class FeedMedia extends FeedFile  {
    public static final int FEEDFILETYPE_FEEDMEDIA = 2;
    public static final int PLAYABLE_TYPE_FEEDMEDIA = 1;
    public static final String FILENAME_PREFIX_EMBEDDED_COVER = "metadata-retriever:";

    public static final String PREF_MEDIA_ID = "FeedMedia.PrefMediaId";
    private static final String PREF_FEED_ID = "FeedMedia.PrefFeedId";

    /**
     * Indicates we've checked on the size of the item via the network
     * and got an invalid response. Using Integer.MIN_VALUE because
     * 1) we'll still check on it in case it gets downloaded (it's <= 0)
     * 2) By default all FeedMedia have a size of 0 if we don't know it,
     *    so this won't conflict with existing practice.
     */
    private static final int CHECKED_ON_SIZE_BUT_UNKNOWN = Integer.MIN_VALUE;

    private int duration;
    private int position; // Current position in file
    private long lastPlayedTime; // Last time this media was played (in ms)
    private int played_duration; // How many ms of this file have been played
    private long size; // File size in Byte
    private String mime_type;
    @Nullable private volatile FeedItem item;
    private Date playbackCompletionDate;
    private int startPosition = -1;
    private int playedDurationWhenStarted;

    // if null: unknown, will be checked
    private Boolean hasEmbeddedPicture;

    /* Used for loading item when restoring from parcel. */
    private long itemID;

    public FeedMedia(FeedItem i, String download_url, long size,
                     String mime_type) {
        super(null, download_url, false);
        this.item = i;
        this.size = size;
        this.mime_type = mime_type;
    }

    public FeedMedia(long id, FeedItem item, int duration, int position,
                     long size, String mime_type, String file_url, String download_url,
                     boolean downloaded, Date playbackCompletionDate, int played_duration,
                     long lastPlayedTime) {
        super(file_url, download_url, downloaded);
        this.id = id;
        this.item = item;
        this.duration = duration;
        this.position = position;
        this.played_duration = played_duration;
        this.playedDurationWhenStarted = played_duration;
        this.size = size;
        this.mime_type = mime_type;
        this.playbackCompletionDate = playbackCompletionDate == null
                ? null : (Date) playbackCompletionDate.clone();
        this.lastPlayedTime = lastPlayedTime;
    }

    public FeedMedia(long id, FeedItem item, int duration, int position,
                      long size, String mime_type, String file_url, String download_url,
                      boolean downloaded, Date playbackCompletionDate, int played_duration,
                      Boolean hasEmbeddedPicture, long lastPlayedTime) {
        this(id, item, duration, position, size, mime_type, file_url, download_url, downloaded,
                playbackCompletionDate, played_duration, lastPlayedTime);
        this.hasEmbeddedPicture = hasEmbeddedPicture;
    }

    @Override
    public String getHumanReadableIdentifier() {
        if (item != null && item.getTitle() != null) {
            return item.getTitle();
        } else {
            return download_url;
        }
    }

    /**
     * Returns a MediaItem representing the FeedMedia object.
     * This is used by the MediaBrowserService
     */
    public MediaBrowserCompat.MediaItem getMediaItem() {

        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId(String.valueOf(id));


        return new MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
    }

    /**
     * Uses mimetype to determine the type of media.
     */


    public void updateFromOther(FeedMedia other) {
        super.updateFromOther(other);
        if (other.size > 0) {
            size = other.size;
        }
        if (other.mime_type != null) {
            mime_type = other.mime_type;
        }
    }

    public boolean compareWithOther(FeedMedia other) {
        if (super.compareWithOther(other)) {
            return true;
        }
        if (other.mime_type != null) {
            if (mime_type == null || !mime_type.equals(other.mime_type)) {
                return true;
            }
        }
        if (other.size > 0 && other.size != size) {
            return true;
        }
        return false;
    }

    @Override
    public int getTypeAsInt() {
        return FEEDFILETYPE_FEEDMEDIA;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }


    public int getPlayedDuration() {
        return played_duration;
    }

    public int getPlayedDurationWhenStarted() {
        return playedDurationWhenStarted;
    }

    public void setPlayedDuration(int played_duration) {
        this.played_duration = played_duration;
    }

    public int getPosition() {
        return position;
    }



    public void setPosition(int position) {
        this.position = position;
        if(position > 0 && item != null && item.isNew()) {
            this.item.setPlayed(false);
        }
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }



    /**
     * Indicates we asked the service what the size was, but didn't
     * get a valid answer and we shoudln't check using the network again.
     */
    public void setCheckedOnSizeButUnknown() {
        this.size = CHECKED_ON_SIZE_BUT_UNKNOWN;
    }

    public boolean checkedOnSizeButUnknown() {
        return (CHECKED_ON_SIZE_BUT_UNKNOWN == this.size);
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    @Nullable
    public FeedItem getItem() {
        return item;
    }

    /**
     * Sets the item object of this FeedMedia. If the given
     * FeedItem object is not null, it's 'media'-attribute value
     * will also be set to this media object.
     */
    public void setItem(FeedItem item) {
        this.item = item;
        if (item != null && item.getMedia() != this) {
            item.setMedia(this);
        }
    }

    public Date getPlaybackCompletionDate() {
        return playbackCompletionDate == null
                ? null : (Date) playbackCompletionDate.clone();
    }

    public void setPlaybackCompletionDate(Date playbackCompletionDate) {
        this.playbackCompletionDate = playbackCompletionDate == null
                ? null : (Date) playbackCompletionDate.clone();
    }

    public boolean isInProgress() {
        return (this.position > 0);
    }

    public boolean hasEmbeddedPicture() {
        if(hasEmbeddedPicture == null) {
            checkEmbeddedPicture();
        }
        return hasEmbeddedPicture;
    }








    public long getItemId() {
        return itemID;
    }



    public static final Parcelable.Creator<FeedMedia> CREATOR = new Parcelable.Creator<FeedMedia>() {
        public FeedMedia createFromParcel(Parcel in) {
            final long id = in.readLong();
            final long itemID = in.readLong();
            FeedMedia result = new FeedMedia(id, null, in.readInt(), in.readInt(), in.readLong(), in.readString(), in.readString(),
                    in.readString(), in.readByte() != 0, new Date(in.readLong()), in.readInt(), in.readLong());
            result.itemID = itemID;
            return result;
        }

        public FeedMedia[] newArray(int size) {
            return new FeedMedia[size];
        }
    };



    public void setHasEmbeddedPicture(Boolean hasEmbeddedPicture) {
        this.hasEmbeddedPicture = hasEmbeddedPicture;
    }



    public void checkEmbeddedPicture() {

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {

            byte[] image = mmr.getEmbeddedPicture();
            if(image != null) {
                hasEmbeddedPicture = Boolean.TRUE;
            } else {
                hasEmbeddedPicture = Boolean.FALSE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasEmbeddedPicture = Boolean.FALSE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        return super.equals(o);
    }
}
