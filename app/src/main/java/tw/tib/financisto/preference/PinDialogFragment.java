package tw.tib.financisto.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDialogFragmentCompat;

import tw.tib.financisto.R;
import tw.tib.financisto.view.PinView;

public class PinDialogFragment extends PreferenceDialogFragmentCompat implements PinView.PinListener {
    public static PinDialogFragment newInstance(String key) {
        final PinDialogFragment f = new PinDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    protected View onCreateDialogView(@NonNull Context context) {
        return new PinView(context, this, R.layout.lock).getView();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {

    }

    @Override
    public void onConfirm(String pinBase64) {
        getDialog().setTitle(R.string.confirm_pin);
    }

    @Override
    public void onSuccess(String pinBase64) {
        var pinPreference = (PinPreference) getPreference();
        if (pinPreference.callChangeListener(pinBase64)) {
            pinPreference.setPin(pinBase64);
        }
        getDialog().dismiss();
    }
}
