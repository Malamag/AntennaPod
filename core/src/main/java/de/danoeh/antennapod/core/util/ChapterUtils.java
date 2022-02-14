package de.danoeh.antennapod.core.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.core.feed.ChapterMerger;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.comparator.ChapterStartTimeComparator;
import de.danoeh.antennapod.parser.media.id3.ChapterReader;
import de.danoeh.antennapod.parser.media.id3.ID3ReaderException;

import de.danoeh.antennapod.parser.media.vorbis.VorbisCommentChapterReader;
import de.danoeh.antennapod.parser.media.vorbis.VorbisCommentReaderException;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.input.CountingInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for getting chapter data from media files.
 */
public class ChapterUtils {

    private static final String TAG = "ChapterUtils";

    private ChapterUtils() {
    }

    public static int getCurrentChapterIndex(int position) {

        return  1;
    }

    public static void loadChapters( Context context) {


    }

    public static List<Chapter> loadChaptersFromMediaFile( Context context) {

        return null;
    }

    private static CountingInputStream openStream(Context context) throws IOException {


        throw new IOException("Local file does not exist");
    }

    @NonNull
    private static List<Chapter> readId3ChaptersFrom(CountingInputStream in) throws IOException, ID3ReaderException {
        ChapterReader reader = new ChapterReader(in);
        reader.readInputStream();
        List<Chapter> chapters = reader.getChapters();
        Collections.sort(chapters, new ChapterStartTimeComparator());
        enumerateEmptyChapterTitles(chapters);
        if (!chaptersValid(chapters)) {
            Log.e(TAG, "Chapter data was invalid");
            return Collections.emptyList();
        }
        return chapters;
    }

    @NonNull
    private static List<Chapter> readOggChaptersFromInputStream(InputStream input) throws VorbisCommentReaderException {
        VorbisCommentChapterReader reader = new VorbisCommentChapterReader();
        reader.readInputStream(input);
        List<Chapter> chapters = reader.getChapters();
        if (chapters == null) {
            return Collections.emptyList();
        }
        Collections.sort(chapters, new ChapterStartTimeComparator());
        enumerateEmptyChapterTitles(chapters);
        if (chaptersValid(chapters)) {
            return chapters;
        }
        return Collections.emptyList();
    }

    /**
     * Makes sure that chapter does a title and an item attribute.
     */
    private static void enumerateEmptyChapterTitles(List<Chapter> chapters) {
        for (int i = 0; i < chapters.size(); i++) {
            Chapter c = chapters.get(i);
            if (c.getTitle() == null) {
                c.setTitle(Integer.toString(i));
            }
        }
    }

    private static boolean chaptersValid(List<Chapter> chapters) {
        if (chapters.isEmpty()) {
            return false;
        }
        for (Chapter c : chapters) {
            if (c.getStart() < 0) {
                return false;
            }
        }
        return true;
    }
}
