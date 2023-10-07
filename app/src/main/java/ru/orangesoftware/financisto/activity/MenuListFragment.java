package ru.orangesoftware.financisto.activity;

import static android.app.Activity.RESULT_OK;
import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.ListFragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OnActivityResult;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.SummaryEntityListAdapter;
import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.export.BackupImportTask;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.drive.GoogleDriveBackupTask;
import ru.orangesoftware.financisto.export.drive.GoogleDriveFileInfo;
import ru.orangesoftware.financisto.export.drive.GoogleDriveFileList;
import ru.orangesoftware.financisto.export.drive.GoogleDriveListFilesTask;
import ru.orangesoftware.financisto.export.drive.GoogleDriveRestoreTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxBackupTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxFileList;
import ru.orangesoftware.financisto.export.dropbox.DropboxListFilesTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxRestoreTask;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.utils.PinProtection;

@EFragment(R.layout.activity_menu_list)
public class MenuListFragment extends ListFragment {
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;

    @Bean
    GreenRobotBus bus;

    @AfterViews
    protected void init() {
        setListAdapter(new SummaryEntityListAdapter(getContext(), MenuListItem.values()));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MenuListItem.values()[position].call(getActivity());
    }

    @OnActivityResult(MenuListItem.ACTIVITY_RESTORE_DATABASE)
    public void onRestoreDatabase(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            Uri backupFileUri = data.getData();
            Log.i("Financisto", "ACTIVITY_RESTORE_DATABASE uri: " + backupFileUri.toString());
            ProgressDialog d = ProgressDialog.show(getContext(), null, getString(R.string.restore_database_inprogress), true);
            new BackupImportTask(getActivity(), d).execute(backupFileUri);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CSV_EXPORT)
    public void onCsvExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvExportOptions options = CsvExportOptions.fromIntent(data);
            MenuListItem.doCsvExport(getActivity(), options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_QIF_EXPORT)
    public void onQifExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            QifExportOptions options = QifExportOptions.fromIntent(data);
            MenuListItem.doQifExport(getActivity(), options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CSV_IMPORT)
    public void onCsvImportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvImportOptions options = CsvImportOptions.fromIntent(data);
            MenuListItem.doCsvImport(getActivity(), options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_QIF_IMPORT)
    public void onQifImportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            QifImportOptions options = QifImportOptions.fromIntent(data);
            MenuListItem.doQifImport(getActivity(), options);
        }
    }

    @OnActivityResult(MenuListItem.ACTIVITY_CHANGE_PREFERENCES)
    public void onChangePreferences() {
        scheduleNextAutoBackup(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        PinProtection.lock(getContext());
        bus.unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        PinProtection.unlock(getContext());
        bus.register(this);
    }

    ProgressDialog progressDialog;

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    // google drive

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doGoogleDriveBackup(StartDriveBackup e) {
        ProgressDialog d = ProgressDialog.show(getContext(), null, getString(R.string.backup_database_gdocs_inprogress), true);
        new GoogleDriveBackupTask(getActivity(), d).execute();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doGoogleDriveRestore(StartDriveRestore e) {
        ProgressDialog d = ProgressDialog.show(getContext(), null, this.getString(R.string.google_drive_loading_files), true);
        new GoogleDriveListFilesTask(getActivity(), d).execute();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGoogleDriveFileList(GoogleDriveFileList event) {
        dismissProgressDialog();
        final GoogleDriveFileInfo[] files = event.files;
        final String[] fileNames = getFileNames(files);
        final Context context = getContext();
        final GoogleDriveFileInfo[] selectedDriveFile = new GoogleDriveFileInfo[1];
        new AlertDialog.Builder(context)
                .setTitle(R.string.restore_database_online_google_drive)
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    if (selectedDriveFile[0] != null) {
                        ProgressDialog d = ProgressDialog.show(context, null, getString(R.string.google_drive_restore_in_progress), true);
                        new GoogleDriveRestoreTask(getActivity(), d, selectedDriveFile[0]).execute();
                    }
                })
                .setSingleChoiceItems(fileNames, -1, (dialog, which) -> {
                    if (which >= 0 && which < fileNames.length) {
                        selectedDriveFile[0] = files[which];
                    }
                })
                .show();
    }

    private String[] getFileNames(GoogleDriveFileInfo[] files) {
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].name;
        }
        return names;
    }

    @OnActivityResult(RESOLVE_CONNECTION_REQUEST_CODE)
    public void onConnectionRequest(int resultCode) {
        if (resultCode == RESULT_OK) {
            Toast.makeText(getContext(), R.string.google_drive_connection_resolved, Toast.LENGTH_LONG).show();
        }
    }

    // dropbox
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doImportFromDropbox(DropboxFileList event) {
        final String[] backupFiles = event.files;
        if (backupFiles != null) {
            final String[] selectedDropboxFile = new String[1];
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.restore_database_online_dropbox)
                    .setPositiveButton(R.string.restore, (dialog, which) -> {
                        if (selectedDropboxFile[0] != null) {
                            ProgressDialog d = ProgressDialog.show(getContext(), null, getString(R.string.restore_database_inprogress_dropbox), true);
                            new DropboxRestoreTask(getActivity(), d, selectedDropboxFile[0]).execute();
                        }
                    })
                    .setSingleChoiceItems(backupFiles, -1, (dialog, which) -> {
                        if (which >= 0 && which < backupFiles.length) {
                            selectedDropboxFile[0] = backupFiles[which];
                        }
                    })
                    .show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doDropboxBackup(StartDropboxBackup e) {
        ProgressDialog d = ProgressDialog.show(getContext(), null, this.getString(R.string.backup_database_dropbox_inprogress), true);
        new DropboxBackupTask(getActivity(), d).execute();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doDropboxRestore(StartDropboxRestore e) {
        ProgressDialog d = ProgressDialog.show(getContext(), null, this.getString(R.string.dropbox_loading_files), true);
        new DropboxListFilesTask(getActivity(), d).execute();
    }

    public static class StartDropboxBackup {
    }

    public static class StartDropboxRestore {
    }

    public static class StartDriveBackup {
    }

    public static class StartDriveRestore {
    }
}
