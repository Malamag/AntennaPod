package de.danoeh.antennapod.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.core.glide.ApGlideSettings;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.model.feed.EmbeddedChapterImage;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.ui.common.ThemeUtils;

import de.danoeh.antennapod.ui.common.CircularProgressBar;

public class ChaptersListAdapter extends RecyclerView.Adapter<ChaptersListAdapter.ChapterHolder> {

    private final Callback callback;
    private final Context context;
    private int currentChapterIndex = -1;
    private long currentChapterPosition = -1;
    private boolean hasImages = false;

    public ChaptersListAdapter(Context context, Callback callback) {
        this.callback = callback;
        this.context = context;
    }

    public void setMedia() {

    }

    @Override
    public void onBindViewHolder(@NonNull ChapterHolder holder, int position) {


    }

    @NonNull
    @Override
    public ChapterHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return new ChapterHolder(inflater.inflate(R.layout.simplechapter_item, parent, false));
    }

    @Override
    public int getItemCount() {
       return  0;
    }

    static class ChapterHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView start;
        final TextView link;
        final TextView duration;
        final ImageView image;
        final View secondaryActionButton;
        final ImageView secondaryActionIcon;
        final CircularProgressBar progressBar;

        public ChapterHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txtvTitle);
            start = itemView.findViewById(R.id.txtvStart);
            link = itemView.findViewById(R.id.txtvLink);
            image = itemView.findViewById(R.id.imgvCover);
            duration = itemView.findViewById(R.id.txtvDuration);
            secondaryActionButton = itemView.findViewById(R.id.secondaryActionButton);
            secondaryActionIcon = itemView.findViewById(R.id.secondaryActionIcon);
            progressBar = itemView.findViewById(R.id.secondaryActionProgress);
        }
    }

    public void notifyChapterChanged(int newChapterIndex) {
        currentChapterIndex = newChapterIndex;


    }

    public void notifyTimeChanged(long timeMs) {
        currentChapterPosition = timeMs;
        // Passing an argument prevents flickering.
        // See EpisodeItemListAdapter.notifyItemChangedCompat.
        notifyItemChanged(currentChapterIndex, "foo");
    }


    public interface Callback {
        void onPlayChapterButtonClicked(int position);
    }

}
