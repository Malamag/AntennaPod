package de.danoeh.antennapod.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import de.danoeh.antennapod.playback.base.PlayerStatus;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.ChaptersListAdapter;

import de.danoeh.antennapod.core.util.ChapterUtils;

import de.danoeh.antennapod.model.feed.Chapter;

import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ChaptersFragment extends AppCompatDialogFragment {
    public static final String TAG = "ChaptersFragment";
    private ChaptersListAdapter adapter;

    private Disposable disposable;
    private int focusedChapter = -1;

    private LinearLayoutManager layoutManager;
    private ProgressBar progressBar;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.chapters_label))
                .setView(onCreateView(getLayoutInflater()))
                .setNegativeButton(getString(R.string.cancel_label), null) //dismisses
                .create();
    }


    public View onCreateView(@NonNull LayoutInflater inflater) {
        View root = inflater.inflate(R.layout.simple_list_fragment, null, false);
        root.findViewById(R.id.toolbar).setVisibility(View.GONE);
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        progressBar = root.findViewById(R.id.progLoading);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation()));

        adapter = new ChaptersListAdapter(getActivity(), pos -> {
            /*if (controller.getStatus() != PlayerStatus.PLAYING) {
                controller.playPause();
            }*/

            updateChapterSelection(pos);
        });
        recyclerView.setAdapter(adapter);

        progressBar.setVisibility(View.VISIBLE);
        
        RelativeLayout.LayoutParams wrapHeight = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        recyclerView.setLayoutParams(wrapHeight);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
        loadMediaInfo();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (disposable != null) {
            disposable.dispose();
        }

        EventBus.getDefault().unregister(this);
    }



    private int getCurrentChapter() {

            return -1;


    }

    private void loadMediaInfo() {
        if (disposable != null) {
            disposable.dispose();
        }

    }

    private void onMediaChanged() {

    }

    private void updateChapterSelection(int position) {
        if (adapter == null) {
            return;
        }

        if (position != -1 && focusedChapter != position) {
            focusedChapter = position;
            adapter.notifyChapterChanged(focusedChapter);
            if (layoutManager.findFirstCompletelyVisibleItemPosition() >= position
                    || layoutManager.findLastCompletelyVisibleItemPosition() <= position) {
                layoutManager.scrollToPositionWithOffset(position, 100);
            }
        }
    }
}
