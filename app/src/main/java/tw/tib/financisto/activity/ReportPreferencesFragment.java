package tw.tib.financisto.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.Collections;

import tw.tib.financisto.R;
import tw.tib.financisto.model.Currency;
import tw.tib.financisto.utils.CurrencyCache;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;

public class ReportPreferencesFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.report_preferences, rootKey);

        var currencies = new ArrayList<>(CurrencyCache.getAllCurrencies());
        var currencyTitles = new ArrayList<String>();
        var currencyDisplay = new ArrayList<String>();
        Collections.sort(currencies, (a, b) -> {
            if (a.isDefault) return -1;
            if (b.isDefault) return 1;
            return a.name.compareTo(b.name);
        });
        for (Currency c : currencies) {
            currencyTitles.add(c.title);
            currencyDisplay.add(c.name + " " + c.symbol + " " + c.title);
        }

        ListPreference referenceCurrencyPref = getPreferenceScreen().findPreference("report_reference_currency");

        referenceCurrencyPref.setEntries(currencyDisplay.toArray(new String[0]));
        referenceCurrencyPref.setEntryValues(currencyTitles.toArray(new String[0]));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = getContext();

        MyPreferences.switchLocale(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        PinProtection.lock(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        PinProtection.unlock(getContext());
    }

}
