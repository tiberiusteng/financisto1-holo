package tw.tib.financisto.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import tw.tib.financisto.R;

public class TimePreference extends DialogPreference {
    private static final int DEFAULT_VALUE = 600;
    private int time;

    public TimePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public TimePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TimePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TimePreference(@NonNull Context context) {
        super(context);
    }

    public void setTime(int time) {
        final boolean wasBlocking = shouldDisableDependents();

        this.time = time;

        persistInt(time);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }

        showSummary();
        notifyChanged();
    }

    public int getTime() {
        return this.time;
    }

    private void showSummary() {
        setSummary(getContext().getString(R.string.auto_backup_time_summary, time/100, time%100));
    }

    @Nullable
    @Override
    protected Object onGetDefaultValue(@NonNull TypedArray a, int index) {
        return a.getInt(index, DEFAULT_VALUE);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        if (defaultValue == null) {
            defaultValue = DEFAULT_VALUE;
        }
        setTime(getPersistedInt((Integer) defaultValue));
    }
}
