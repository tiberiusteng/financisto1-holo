package ru.orangesoftware.financisto.export.drive;

import android.app.Activity;
import android.os.AsyncTask;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

public class GoogleDriveAuthorizeFolderTask extends AsyncTask<String, String, Object> {
    protected final Activity context;
    protected final String folderName;
    protected final int requestCode;

    public GoogleDriveAuthorizeFolderTask(Activity context, String folderName, int requestCode) {
        this.context = context;
        this.folderName = folderName;
        this.requestCode = requestCode;
    }

    @Override
    protected Object doInBackground(String... params) {
        GoogleDriveRESTClient googleDriveRESTClient = new GoogleDriveRESTClient(this.context);
        try {
            googleDriveRESTClient.getBackupFolderID(true);
        } catch (Exception e) {
            if (e instanceof UserRecoverableAuthIOException) {
                context.startActivityForResult(
                        ((UserRecoverableAuthIOException) e).getIntent(),
                        requestCode);
            }
        }
        return true;
    }
}
