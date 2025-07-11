package tw.tib.financisto.activity;

import static android.app.Activity.RESULT_OK;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import tw.tib.financisto.R;
import tw.tib.financisto.export.Export;
import tw.tib.financisto.export.drive.GoogleDriveAuthorizeFolderTask;
import tw.tib.financisto.export.dropbox.Dropbox;
import tw.tib.financisto.preference.PinDialogFragment;
import tw.tib.financisto.preference.PinPreference;
import tw.tib.financisto.preference.TimeDialogFragment;
import tw.tib.financisto.preference.TimePreference;
import tw.tib.financisto.rates.ExchangeRateProviderFactory;
import tw.tib.financisto.utils.FingerprintUtils;
import tw.tib.financisto.utils.MyPreferences;
import tw.tib.financisto.utils.PinProtection;

public class PreferenceFragment extends PreferenceFragmentCompat {
    private static final int SELECT_DATABASE_FOLDER = 100;
    private static final int CHOOSE_ACCOUNT = 101;
    private static final int REQUEST_AUTHORIZATION = 102;

    private Dropbox dropbox;

    Preference pOpenExchangeRatesAppId;

    Preference pGoogleDriveSignIn;
    Preference pGoogleDriveSignOut;
    Preference pGoogleDriveBackupFolder;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference pLocale = preferenceScreen.findPreference("ui_language");
        Context context = getContext();

        ViewCompat.setOnApplyWindowInsetsListener(getListView(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, insets.bottom);
            ((ViewGroup) v).setClipToPadding(false);
            return WindowInsetsCompat.CONSUMED;
        });

        dropbox = new Dropbox(getContext());

        pLocale.setOnPreferenceChangeListener((preference, newValue) -> {
            String locale = (String) newValue;
            MyPreferences.switchLocale(context, locale);
            return true;
        });
        Preference pNewTransactionShortcut = preferenceScreen.findPreference("shortcut_new_transaction");
        pNewTransactionShortcut.setOnPreferenceClickListener(arg0 -> {
            addShortcut(".activity.TransactionActivity", R.string.transaction, R.drawable.icon_transaction);
            return true;
        });
        Preference pNewTransferShortcut = preferenceScreen.findPreference("shortcut_new_transfer");
        pNewTransferShortcut.setOnPreferenceClickListener(arg0 -> {
            addShortcut(".activity.TransferActivity", R.string.transfer, R.drawable.icon_transfer);
            return true;
        });
        Preference pDatabaseBackupFolder = preferenceScreen.findPreference("database_backup_folder");
        pDatabaseBackupFolder.setOnPreferenceClickListener(arg0 -> {
            selectDatabaseBackupFolder();
            return true;
        });
        Preference pAuthDropbox = preferenceScreen.findPreference("dropbox_authorize");
        pAuthDropbox.setOnPreferenceClickListener(arg0 -> {
            authDropbox();
            return true;
        });
        Preference pDeauthDropbox = preferenceScreen.findPreference("dropbox_unlink");
        pDeauthDropbox.setOnPreferenceClickListener(arg0 -> {
            deAuthDropbox();
            return true;
        });
        Preference pExchangeProvider = preferenceScreen.findPreference("exchange_rate_provider");
        pOpenExchangeRatesAppId = preferenceScreen.findPreference("openexchangerates_app_id");
        pExchangeProvider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                pOpenExchangeRatesAppId.setEnabled(isOpenExchangeRatesProvider((String) newValue));
                return true;
            }

            private boolean isOpenExchangeRatesProvider(String provider) {
                return ExchangeRateProviderFactory.openexchangerates.name().equals(provider);
            }
        });

        pGoogleDriveSignIn = preferenceScreen.findPreference("google_drive_backup_account");
        pGoogleDriveSignIn.setOnPreferenceClickListener(arg0 -> {
            chooseAccount();
            return true;
        });
        pGoogleDriveSignOut = preferenceScreen.findPreference("google_drive_sign_out");
        pGoogleDriveSignOut.setOnPreferenceClickListener(arg0 -> {
            signOutGoogleAccount();
            return true;
        });
        pGoogleDriveBackupFolder = preferenceScreen.findPreference("google_drive_backup_folder");
        pGoogleDriveBackupFolder.setOnPreferenceChangeListener((Preference preference, Object newValue) -> {
            new GoogleDriveAuthorizeFolderTask(getActivity(),
                    (String) newValue,
                    REQUEST_AUTHORIZATION).execute();
            return true;
        });

        GoogleSignInAccount googleDriveAccount = GoogleSignIn.getLastSignedInAccount(context);
        updateGoogleDriveSignIn(googleDriveAccount);

        Preference useFingerprint = preferenceScreen.findPreference("pin_protection_use_fingerprint");
        if (FingerprintUtils.fingerprintUnavailable(context)) {
            useFingerprint.setSummary(getString(R.string.fingerprint_unavailable, FingerprintUtils.reasonWhyFingerprintUnavailable(context)));
            useFingerprint.setEnabled(false);
        }
        linkToDropbox();
        setCurrentDatabaseBackupFolder();
        enableOpenExchangeApp();
        selectAccount();
    }
    private void updateGoogleDriveSignIn(GoogleSignInAccount account) {
        if (account == null) {
            pGoogleDriveSignIn.setEnabled(true);
            pGoogleDriveSignIn.setSummary(R.string.google_drive_backup_account_summary);
            pGoogleDriveSignOut.setEnabled(false);
        }
        else {
            pGoogleDriveSignIn.setEnabled(false);
            pGoogleDriveSignIn.setSummary(getString(R.string.google_drive_signed_in_as,
                    account.getEmail()));
            pGoogleDriveSignOut.setEnabled(true);
        }
    }

    private void chooseAccount() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getContext(), gso);

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, CHOOSE_ACCOUNT);
    }

    private void signOutGoogleAccount() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getContext(), gso);
        googleSignInClient.signOut().addOnCompleteListener(getActivity(), task -> {
            Toast.makeText(getContext(), R.string.google_drive_signed_out, Toast.LENGTH_LONG).show();
            pGoogleDriveSignOut.setEnabled(false);
            pGoogleDriveSignIn.setEnabled(true);
            pGoogleDriveSignIn.setSummary(R.string.google_drive_backup_account_summary);
        });

    }

    private Account getSelectedAccount() {
        String accountName = MyPreferences.getGoogleDriveAccount(getContext());
        if (accountName != null) {
            AccountManager accountManager = AccountManager.get(getContext());
            Account[] accounts = accountManager.getAccountsByType("com.google");
            for (Account account : accounts) {
                if (accountName.equals(account.name)) {
                    return account;
                }
            }
        }
        return null;
    }

    private void linkToDropbox() {
        boolean dropboxAuthorized = MyPreferences.isDropboxAuthorized(getContext());
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.findPreference("dropbox_authorize").setEnabled(!dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_unlink").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_backup").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_autobackup").setEnabled(dropboxAuthorized);
    }

    private void selectDatabaseBackupFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, getDatabaseBackupFolder());
        startActivityForResult(intent, SELECT_DATABASE_FOLDER);
    }

    private void enableOpenExchangeApp() {
        pOpenExchangeRatesAppId.setEnabled(MyPreferences.isOpenExchangeRatesProviderSelected(getContext()));
    }

    private String getDatabaseBackupFolder() {
        return Export.getBackupFolder(getContext());
    }

    private void setCurrentDatabaseBackupFolder() {
        Preference pDatabaseBackupFolder = getPreferenceScreen().findPreference("database_backup_folder");
        String summary = getString(R.string.database_backup_folder_summary, Uri.parse(getDatabaseBackupFolder()).getLastPathSegment());
        pDatabaseBackupFolder.setSummary(summary);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Context context = getContext();

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_DATABASE_FOLDER:
                    if (data != null) {
                        Uri backupFolderUri = data.getData();
                        Log.i("Financisto", "backup folder uri: " + backupFolderUri.toString());
                        getContext().getContentResolver().takePersistableUriPermission(backupFolderUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        MyPreferences.setDatabaseBackupFolder(context, backupFolderUri.toString());
                        setCurrentDatabaseBackupFolder();
                    }
                    else {
                        Log.e("Financisto", "select database folder data is null");
                    }
                    break;

                case CHOOSE_ACCOUNT:
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult();
                    new GoogleDriveAuthorizeFolderTask(getActivity(),
                            MyPreferences.getGoogleDriveBackupFolder(context),
                            REQUEST_AUTHORIZATION).execute();
                    String signedInAs = getString(R.string.google_drive_signed_in_as,
                            account.getEmail());
                    Toast.makeText(context, signedInAs, Toast.LENGTH_LONG).show();
                    updateGoogleDriveSignIn(account);
                    break;

                case REQUEST_AUTHORIZATION:
                    Toast.makeText(context, R.string.google_drive_authorized, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private void selectAccount() {
        Preference pDriveAccount = getPreferenceScreen().findPreference("google_drive_backup_account");
        Account account = getSelectedAccount();
        if (account != null) {
            pDriveAccount.setSummary(account.name);
        }
    }

    private void addShortcut(String activity, int nameId, int iconId) {
        Intent intent = createShortcutIntent(activity, getString(nameId), Intent.ShortcutIconResource.fromContext(getContext(), iconId),
                "com.android.launcher.action.INSTALL_SHORTCUT");
        getContext().sendBroadcast(intent);
    }

    private Intent createShortcutIntent(String activity, String shortcutName, Intent.ShortcutIconResource shortcutIcon, String action) {
        Intent shortcutIntent = new Intent();
        shortcutIntent.setComponent(new ComponentName(getContext().getPackageName(), activity));
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon);
        intent.setAction(action);
        return intent;
    }

    private void authDropbox() {
        dropbox.startAuth();
    }

    private void deAuthDropbox() {
        dropbox.deAuth();
        linkToDropbox();
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
        dropbox.completeAuth();
        linkToDropbox();

        var actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.preferences);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof TimePreference) {
            TimeDialogFragment f = TimeDialogFragment.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getParentFragmentManager(), null);
            return;
        }
        else if (preference instanceof PinPreference) {
            PinDialogFragment f = PinDialogFragment.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getParentFragmentManager(), null);
            return;
        }
        super.onDisplayPreferenceDialog(preference);
    }
}
