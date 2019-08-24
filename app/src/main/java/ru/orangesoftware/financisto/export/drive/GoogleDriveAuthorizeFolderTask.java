package ru.orangesoftware.financisto.export.drive;

import android.app.Activity;
import android.os.AsyncTask;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

public class GoogleDriveAuthorizeFolderTask extends AsyncTask<String, String, Object> {
    protected final Activity context;
    private final int REQUEST_AUTHORIZATION = 1;

    public GoogleDriveAuthorizeFolderTask(Activity context) {
        this.context = context;
    }

    @Override
    protected Object doInBackground(String... params) {
        GoogleDriveRESTClient googleDriveRESTClient = new GoogleDriveRESTClient(this.context);
        try {
            googleDriveRESTClient.getBackupFolderID();
        } catch (Exception e) {
            if (e instanceof UserRecoverableAuthIOException) {
                context.startActivityForResult(((UserRecoverableAuthIOException) e).getIntent(), REQUEST_AUTHORIZATION);
            }
        }
        return true;
    }
}
