package tw.tib.financisto.preference;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDialogFragmentCompat;

import tw.tib.financisto.datetime.DateUtils;

public class TimeDialogFragment extends PreferenceDialogFragmentCompat implements TimePicker.OnTimeChangedListener {
    public static final String TAG = "TimeDialogFragment";

    private int time;

    public static TimeDialogFragment newInstance(String key) {
        final TimeDialogFragment f = new TimeDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    protected View onCreateDialogView(@NonNull Context context) {
        context = new ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        time = ((TimePreference) getPreference()).getTime();
        Log.d(TAG, "time: " + time);
        TimePicker timePicker = new TimePicker(context);
        timePicker.setIs24HourView(DateUtils.is24HourFormat(context));
        timePicker.setCurrentHour(getHour());
        timePicker.setCurrentMinute(getMinute());
        timePicker.setOnTimeChangedListener(this);
        return timePicker;
    }

    private int getHour() {
        return time/100;
    }

    private int getMinute() {
        return time%100;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            var timePreference = (TimePreference) getPreference();
            if (timePreference.callChangeListener(time)) {
                timePreference.setTime(time);
            }
        }
    }

    @Override
    public void onTimeChanged(TimePicker timePicker, int hh, int mm) {
        time = hh*100+mm;
    }
}
