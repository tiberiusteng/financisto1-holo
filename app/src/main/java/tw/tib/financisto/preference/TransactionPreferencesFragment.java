package tw.tib.financisto.preference;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import tw.tib.financisto.R;

public class TransactionPreferencesFragment extends PreferenceFragmentBase {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title = R.string.transaction_screen;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_transaction, rootKey);
        if (Build.VERSION.SDK_INT < 22) {
            var preferenceScreen = getPreferenceScreen();
            var preference = preferenceScreen.findPreference("ntsl_use_twin_date_picker");

            preference.setEnabled(false);
        }
    }
}
