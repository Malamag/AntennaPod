package de.danoeh.antennapod.dialog;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;


public class StreamingConfirmationDialog {
    private final Context context;


    public StreamingConfirmationDialog(Context context) {
        this.context = context;

    }

    public void show() {
        new AlertDialog.Builder(context)
                .setTitle(R.string.stream_label)
                .setMessage(R.string.confirm_mobile_streaming_notification_message)
                .setPositiveButton(R.string.confirm_mobile_streaming_button_once, (dialog, which) -> stream())
                .setNegativeButton(R.string.confirm_mobile_streaming_button_always, (dialog, which) -> {
                    UserPreferences.setAllowMobileStreaming(true);
                    stream();
                })
                .setNeutralButton(R.string.cancel_label, null)
                .show();
    }

    private void stream() {

    }
}
