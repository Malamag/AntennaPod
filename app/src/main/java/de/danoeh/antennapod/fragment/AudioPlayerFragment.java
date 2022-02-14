package de.danoeh.antennapod.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import de.danoeh.antennapod.event.PlayerErrorEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.FavoritesEvent;

import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.event.UnreadItemsUpdateEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.ChapterUtils;
import de.danoeh.antennapod.core.util.Converter;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.TimeSpeedConverter;

import de.danoeh.antennapod.dialog.PlaybackControlsDialog;
import de.danoeh.antennapod.dialog.SkipPreferenceDialog;
import de.danoeh.antennapod.dialog.SleepTimerDialog;
import de.danoeh.antennapod.dialog.VariableSpeedDialog;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.ui.common.PlaybackSpeedIndicatorView;
import de.danoeh.antennapod.view.ChapterSeekBar;
import de.danoeh.antennapod.view.PlayButton;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Shows the audio player.
 */
public class AudioPlayerFragment extends Fragment implements
        ChapterSeekBar.OnSeekBarChangeListener, Toolbar.OnMenuItemClickListener {
    public static final String TAG = "AudioPlayerFragment";
    public static final int POS_COVER = 0;
    public static final int POS_DESCRIPTION = 1;
    private static final int NUM_CONTENT_FRAGMENTS = 2;

    PlaybackSpeedIndicatorView butPlaybackSpeed;
    TextView txtvPlaybackSpeed;
    private ViewPager2 pager;
    private TextView txtvPosition;
    private TextView txtvLength;
    private ChapterSeekBar sbPosition;
    private ImageButton butRev;
    private TextView txtvRev;
    private PlayButton butPlay;
    private ImageButton butFF;
    private TextView txtvFF;
    private ImageButton butSkip;
    private Toolbar toolbar;
    private ProgressBar progressIndicator;
    private CardView cardViewSeek;
    private TextView txtvSeek;


    private Disposable disposable;
    private boolean showTimeLeft;
    private boolean seekedToChapterStart = false;
    private int currentChapterIndex = -1;
    private int duration;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View root = inflater.inflate(R.layout.audioplayer_fragment, container, false);
        root.setOnTouchListener((v, event) -> true); // Avoid clicks going through player to fragments below
        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        //toolbar.setNavigationOnClickListener(v ->
          //      ((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED));
        toolbar.setOnMenuItemClickListener(this);

        ExternalPlayerFragment externalPlayerFragment = new ExternalPlayerFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.playerFragment, externalPlayerFragment, ExternalPlayerFragment.TAG)
                .commit();

        butPlaybackSpeed = root.findViewById(R.id.butPlaybackSpeed);
        txtvPlaybackSpeed = root.findViewById(R.id.txtvPlaybackSpeed);
        sbPosition = root.findViewById(R.id.sbPosition);
        txtvPosition = root.findViewById(R.id.txtvPosition);
        txtvLength = root.findViewById(R.id.txtvLength);
        butRev = root.findViewById(R.id.butRev);
        txtvRev = root.findViewById(R.id.txtvRev);
        butPlay = root.findViewById(R.id.butPlay);
        butFF = root.findViewById(R.id.butFF);
        txtvFF = root.findViewById(R.id.txtvFF);
        butSkip = root.findViewById(R.id.butSkip);
        progressIndicator = root.findViewById(R.id.progLoading);
        cardViewSeek = root.findViewById(R.id.cardViewSeek);
        txtvSeek = root.findViewById(R.id.txtvSeek);

        setupLengthTextView();
        setupControlButtons();
        butPlaybackSpeed.setOnClickListener(v -> new VariableSpeedDialog().show(getChildFragmentManager(), null));
        sbPosition.setOnSeekBarChangeListener(this);

        pager = root.findViewById(R.id.pager);
        pager.setAdapter(new AudioPlayerPagerAdapter(this));
        // Required for getChildAt(int) in ViewPagerBottomSheetBehavior to return the correct page
        pager.setOffscreenPageLimit((int) NUM_CONTENT_FRAGMENTS);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pager.post(() -> {
                    if (getActivity() != null) {
                        // By the time this is posted, the activity might be closed again.
                        //((MainActivity) getActivity()).getBottomSheet().updateScrollingChild();
                    }
                });
            }
        });

        return root;
    }

    private void setChapterDividers() {


    }

    public View getExternalPlayerHolder() {
        return getView().findViewById(R.id.playerFragment);
    }

    private void setupControlButtons() {
        butRev.setOnClickListener(v -> {

        });
        butRev.setOnLongClickListener(v -> {
            SkipPreferenceDialog.showSkipPreference(getContext(),
                    SkipPreferenceDialog.SkipDirection.SKIP_REWIND, txtvRev);
            return true;
        });
        butPlay.setOnClickListener(v -> {

        });
        butFF.setOnClickListener(v -> {

        });
        butFF.setOnLongClickListener(v -> {
            SkipPreferenceDialog.showSkipPreference(getContext(),
                    SkipPreferenceDialog.SkipDirection.SKIP_FORWARD, txtvFF);
            return false;
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsUpdate(UnreadItemsUpdateEvent event) {

    }



    private void setupLengthTextView() {
        showTimeLeft = UserPreferences.shouldShowRemainingTime();
        txtvLength.setOnClickListener(v -> {

            showTimeLeft = !showTimeLeft;
            UserPreferences.setShowRemainTimeSetting(showTimeLeft);

        });
    }



    private void loadMediaInfo(boolean includingChapters) {
        if (disposable != null) {
            disposable.dispose();
        }

    }



    private void updateUi() {

    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        loadMediaInfo(false);
        EventBus.getDefault().register(this);
        txtvRev.setText(NumberFormat.getInstance().format(UserPreferences.getRewindSecs()));
        txtvFF.setText(NumberFormat.getInstance().format(UserPreferences.getFastForwardSecs()));
    }

    @Override
    public void onStop() {
        super.onStop();

        progressIndicator.setVisibility(View.GONE); // Controller released; we will not receive buffering updates
        EventBus.getDefault().unregister(this);
        if (disposable != null) {
            disposable.dispose();
        }
    }




    @Subscribe(threadMode = ThreadMode.MAIN)
    public void favoritesChanged(FavoritesEvent event) {
        AudioPlayerFragment.this.loadMediaInfo(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void mediaPlayerError(PlayerErrorEvent event) {
        final AlertDialog.Builder errorDialog = new AlertDialog.Builder(getContext());
        errorDialog.setTitle(R.string.error_label);
        errorDialog.setMessage(event.getMessage());
       //errorDialog.setPositiveButton(android.R.string.ok, (dialog, which) ->
                //((MainActivity) getActivity()).getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED));
        if (!UserPreferences.useExoplayer()) {
            errorDialog.setNeutralButton(R.string.media_player_switch_to_exoplayer, (dialog, which) -> {
                UserPreferences.enableExoplayer();
                //((MainActivity) getActivity()).showSnackbarAbovePlayer(
                  //      R.string.media_player_switched_to_exoplayer, Snackbar.LENGTH_LONG);
            });
        }
        errorDialog.create().show();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {



    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // interrupt position Observer, restart later
        cardViewSeek.setScaleX(.8f);
        cardViewSeek.setScaleY(.8f);
        cardViewSeek.animate()
                .setInterpolator(new FastOutSlowInInterpolator())
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(200)
                .start();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public void setupOptionsMenu() {

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }

    private static class AudioPlayerPagerAdapter extends FragmentStateAdapter {
        private static final String TAG = "AudioPlayerPagerAdapter";

        public AudioPlayerPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "getItem(" + position + ")");

            switch (position) {
                case POS_COVER:
                    return new CoverFragment();
                default:
                case POS_DESCRIPTION:
                    return new ItemDescriptionFragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_CONTENT_FRAGMENTS;
        }
    }

    public void scrollToPage(int page, boolean smoothScroll) {
        if (pager == null) {
            return;
        }

        pager.setCurrentItem(page, smoothScroll);

        Fragment visibleChild = getChildFragmentManager().findFragmentByTag("f" + POS_DESCRIPTION);
        if (visibleChild instanceof ItemDescriptionFragment) {
            ((ItemDescriptionFragment) visibleChild).scrollToTop();
        }
    }

    public void scrollToPage(int page) {
        scrollToPage(page, false);
    }
}
