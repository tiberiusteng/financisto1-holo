/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.DriveScopes;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.dialog.FolderBrowser;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.drive.GoogleDriveAuthorizeFolderTask;
import ru.orangesoftware.financisto.export.drive.GoogleDriveRESTClient;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;
import ru.orangesoftware.financisto.rates.ExchangeRateProviderFactory;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

import static android.Manifest.permission.GET_ACCOUNTS;
import static ru.orangesoftware.financisto.activity.RequestPermission.isRequestingPermission;
import static ru.orangesoftware.financisto.activity.RequestPermission.isRequestingPermissions;
import static ru.orangesoftware.financisto.utils.FingerprintUtils.fingerprintUnavailable;
import static ru.orangesoftware.financisto.utils.FingerprintUtils.reasonWhyFingerprintUnavailable;

public class PreferencesActivity extends PreferenceActivity {

    private static final int SELECT_DATABASE_FOLDER = 100;
    private static final int CHOOSE_ACCOUNT = 101;
    private static final int REQUEST_AUTHORIZATION = 102;

    Preference pOpenExchangeRatesAppId;

    Preference pGoogleDriveSignIn;
    Preference pGoogleDriveSignOut;
    Preference pGoogleDriveBackupFolder;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference pLocale = preferenceScreen.findPreference("ui_language");
        pLocale.setOnPreferenceChangeListener((preference, newValue) -> {
            String locale = (String) newValue;
            MyPreferences.switchLocale(PreferencesActivity.this, locale);
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
            if (isRequestingPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return false;
            }
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
        pExchangeProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
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
            new GoogleDriveAuthorizeFolderTask(this,
                    (String) newValue,
                    REQUEST_AUTHORIZATION).execute();
            return true;
        });

        GoogleSignInAccount googleDriveAccount = GoogleSignIn.getLastSignedInAccount(this);
        updateGoogleDriveSignIn(googleDriveAccount);

        Preference useFingerprint = preferenceScreen.findPreference("pin_protection_use_fingerprint");
        if (fingerprintUnavailable(this)) {
            useFingerprint.setSummary(getString(R.string.fingerprint_unavailable, reasonWhyFingerprintUnavailable(this)));
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

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, CHOOSE_ACCOUNT);
    }

    private void signOutGoogleAccount() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(this, R.string.google_drive_signed_out, Toast.LENGTH_LONG).show();
            pGoogleDriveSignOut.setEnabled(false);
            pGoogleDriveSignIn.setEnabled(true);
            pGoogleDriveSignIn.setSummary(R.string.google_drive_backup_account_summary);
        });

    }

    private Account getSelectedAccount() {
        String accountName = MyPreferences.getGoogleDriveAccount(this);
        if (accountName != null) {
            AccountManager accountManager = AccountManager.get(this);
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
        boolean dropboxAuthorized = MyPreferences.isDropboxAuthorized(this);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.findPreference("dropbox_unlink").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_backup").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_autobackup").setEnabled(dropboxAuthorized);
    }

    private void selectDatabaseBackupFolder() {
        Intent intent = new Intent(this, FolderBrowser.class);
        intent.putExtra(FolderBrowser.PATH, getDatabaseBackupFolder());
        startActivityForResult(intent, SELECT_DATABASE_FOLDER);
    }

    private void enableOpenExchangeApp() {
        pOpenExchangeRatesAppId.setEnabled(MyPreferences.isOpenExchangeRatesProviderSelected(this));
    }

    private String getDatabaseBackupFolder() {
        return Export.getBackupFolder(this).getAbsolutePath();
    }

    private void setCurrentDatabaseBackupFolder() {
        Preference pDatabaseBackupFolder = getPreferenceScreen().findPreference("database_backup_folder");
        String summary = getString(R.string.database_backup_folder_summary, getDatabaseBackupFolder());
        pDatabaseBackupFolder.setSummary(summary);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_DATABASE_FOLDER:
                    String databaseBackupFolder = data.getStringExtra(FolderBrowser.PATH);
                    MyPreferences.setDatabaseBackupFolder(this, databaseBackupFolder);
                    setCurrentDatabaseBackupFolder();
                    break;

                case CHOOSE_ACCOUNT:
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult();
                    new GoogleDriveAuthorizeFolderTask(this,
                            MyPreferences.getGoogleDriveBackupFolder(this),
                            REQUEST_AUTHORIZATION).execute();
                    String signedInAs = getString(R.string.google_drive_signed_in_as,
                            account.getEmail());
                    Toast.makeText(this, signedInAs, Toast.LENGTH_LONG).show();
                    updateGoogleDriveSignIn(account);
                    break;

                case REQUEST_AUTHORIZATION:
                    Toast.makeText(this, R.string.google_drive_authorized, Toast.LENGTH_LONG).show();
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
        Intent intent = createShortcutIntent(activity, getString(nameId), Intent.ShortcutIconResource.fromContext(this, iconId),
                "com.android.launcher.action.INSTALL_SHORTCUT");
        sendBroadcast(intent);
    }

    private Intent createShortcutIntent(String activity, String shortcutName, ShortcutIconResource shortcutIcon, String action) {
        Intent shortcutIntent = new Intent();
        shortcutIntent.setComponent(new ComponentName(this.getPackageName(), activity));
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon);
        intent.setAction(action);
        return intent;
    }

    Dropbox dropbox = new Dropbox(this);

    private void authDropbox() {
        dropbox.startAuth();
    }

    private void deAuthDropbox() {
        dropbox.deAuth();
        linkToDropbox();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
        dropbox.completeAuth();
        linkToDropbox();
    }

}
