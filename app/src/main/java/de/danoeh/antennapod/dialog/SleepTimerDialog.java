package de.danoeh.antennapod.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;

import de.danoeh.antennapod.core.preferences.SleepTimerPreferences;

import de.danoeh.antennapod.core.util.Converter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SleepTimerDialog extends DialogFragment {

    private EditText etxtTime;
    private Spinner spTimeUnit;
    private LinearLayout timeSetup;
    private LinearLayout timeDisplay;
    private TextView time;

    public SleepTimerDialog() {

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View content = View.inflate(getContext(), R.layout.time_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.sleep_timer_label);
        builder.setView(content);
        builder.setPositiveButton(R.string.close_label, null);

        etxtTime = content.findViewById(R.id.etxtTime);
        spTimeUnit = content.findViewById(R.id.spTimeUnit);
        timeSetup = content.findViewById(R.id.timeSetup);
        timeDisplay = content.findViewById(R.id.timeDisplay);
        timeDisplay.setVisibility(View.GONE);
        time = content.findViewById(R.id.time);
        Button extendSleepFiveMinutesButton = content.findViewById(R.id.extendSleepFiveMinutesButton);
        extendSleepFiveMinutesButton.setText(getString(R.string.extend_sleep_timer_label, 5));
        Button extendSleepTenMinutesButton = content.findViewById(R.id.extendSleepTenMinutesButton);
        extendSleepTenMinutesButton.setText(getString(R.string.extend_sleep_timer_label, 10));
        Button extendSleepTwentyMinutesButton = content.findViewById(R.id.extendSleepTwentyMinutesButton);
        extendSleepTwentyMinutesButton.setText(getString(R.string.extend_sleep_timer_label, 20));


        etxtTime.setText(SleepTimerPreferences.lastTimerValue());
        etxtTime.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etxtTime, InputMethodManager.SHOW_IMPLICIT);
        }, 100);

        String[] spinnerContent = new String[] {
                getString(R.string.time_seconds),
                getString(R.string.time_minutes),
                getString(R.string.time_hours) };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, spinnerContent);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTimeUnit.setAdapter(spinnerAdapter);
        spTimeUnit.setSelection(SleepTimerPreferences.lastTimerTimeUnit());

        CheckBox cbShakeToReset = content.findViewById(R.id.cbShakeToReset);
        CheckBox cbVibrate = content.findViewById(R.id.cbVibrate);
        CheckBox chAutoEnable = content.findViewById(R.id.chAutoEnable);

        cbShakeToReset.setChecked(SleepTimerPreferences.shakeToReset());
        cbVibrate.setChecked(SleepTimerPreferences.vibrate());
        chAutoEnable.setChecked(SleepTimerPreferences.autoEnable());

        cbShakeToReset.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setShakeToReset(isChecked));
        cbVibrate.setOnCheckedChangeListener((buttonView, isChecked)
                -> SleepTimerPreferences.setVibrate(isChecked));
        chAutoEnable.setOnCheckedChangeListener((compoundButton, isChecked)
                -> SleepTimerPreferences.setAutoEnable(isChecked));

        Button disableButton = content.findViewById(R.id.disableSleeptimerButton);

        Button setButton = content.findViewById(R.id.setSleeptimerButton);

        return builder.create();
    }



    private void closeKeyboard(View content) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(content.getWindowToken(), 0);
    }
}
