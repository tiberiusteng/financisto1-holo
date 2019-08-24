package ru.orangesoftware.financisto.export.drive;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.DriveFolder;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.ImportExportException;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class GoogleDriveRESTClient {
    private final Context context;
    private Drive googleDriveService;

    private static final String TAG = "GoogleDriveRESTClient";

    public GoogleDriveRESTClient(Context context) {
        this.context = context.getApplicationContext();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));

        credential.setSelectedAccount(account.getAccount());

        this.googleDriveService = new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName("Financisto 1 Holo Test")
                        .build();
    }

    private String getBackupFolderName() throws ImportExportException {
        String folder = MyPreferences.getGoogleDriveBackupFolder(context);
        // check the backup folder registered on preferences
        if (folder == null || folder.equals("")) {
            throw new ImportExportException(R.string.gdocs_folder_not_configured);
        }
        return folder;
    }

    public String getFolderID(String folderName, boolean createIfNotExist) throws Exception {
        String folderID = null;

        FileList result = googleDriveService.files().list()
                .setQ("mimeType = '" + DriveFolder.MIME_TYPE + "' and name = '" + folderName + "' ")
                .setSpaces("drive")
                .execute();
        if (result.getFiles().size() > 0) {
            folderID = result.getFiles().get(0).getId();
            Log.i(TAG, String.format("Got folder '%s' ID: '%s'", folderName, folderID));
        } else if (createIfNotExist) {
            // Backup folder not exist yet, create it
            File metadata = new File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setName(folderName);

            File googleFile = googleDriveService.files().create(metadata).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }
            folderID = googleFile.getId();
            Log.i(TAG, String.format("Created new folder '%s', ID: '%s'", folderName, folderID));
        }

        return folderID;

    }

    public String getBackupFolderID(boolean createIfNotExist) throws Exception {
        return getFolderID(getBackupFolderName(), createIfNotExist);
    }

    public List<GoogleDriveFileInfo> listFiles() throws Exception {
        String folderID = getBackupFolderID(false);
        if (folderID == null) {
            throw new ImportExportException(R.string.gdocs_folder_not_configured);
        }

        ArrayList<GoogleDriveFileInfo> result = new ArrayList<>();

        List<File> fileList = googleDriveService.files().list()
                .setQ("'" + folderID + "' in parents and mimeType ='" + Export.BACKUP_MIME_TYPE + "'")
                .setSpaces("drive")
                .setFields("files(id,name,createdTime)")
                .execute().getFiles();

        for (File f : fileList) {
            result.add(new GoogleDriveFileInfo(f.getId(), f.getName(), f.getCreatedTime()));
        }

        return result;
    }

    public void uploadFile(java.io.File file) throws Exception {
        String folderID = getBackupFolderID(false);
        if (folderID == null) {
            throw new ImportExportException(R.string.gdocs_folder_not_configured);
        }

        File fileMetadata = new File()
                .setParents(Collections.singletonList(folderID))
                .setMimeType(Export.BACKUP_MIME_TYPE)
                .setName(file.getName());

        FileContent mediaContent = new FileContent(Export.BACKUP_MIME_TYPE, file);

        File uploadedFile = googleDriveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        String backupFileId = uploadedFile.getId();
        Log.i(TAG, "Created backup file ID = " + backupFileId);
    }

    public InputStream getFileAsStream(String fileID) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        googleDriveService.files().get(fileID).executeMediaAndDownloadTo(outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
