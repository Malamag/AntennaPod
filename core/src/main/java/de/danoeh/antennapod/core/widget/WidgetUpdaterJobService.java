package de.danoeh.antennapod.core.widget;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.SafeJobIntentService;


public class WidgetUpdaterJobService extends SafeJobIntentService {
    private static final int JOB_ID = -17001;

    /**
     * Loads the current media from the database and updates the widget in a background job.
     */
    public static void performBackgroundUpdate(Context context) {
        enqueueWork(context, WidgetUpdaterJobService.class,
                WidgetUpdaterJobService.JOB_ID, new Intent(context, WidgetUpdaterJobService.class));
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

    }
}