package tw.tib.financisto.preference;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import tw.tib.financisto.R;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.dialog.AccountReorderDialog;

public class AccountPreferencesFragment extends PreferenceFragmentBase {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        title = R.string.accounts_list_screen;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_accounts, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findPreference("reorder_accounts").setOnPreferenceClickListener(p -> {
            DatabaseAdapter db = new DatabaseAdapter(getContext());
            db.open();
            new AccountReorderDialog(getContext(), db, null).show().setOnDismissListener(d -> db.close());
            return true;
        });
    }
}
