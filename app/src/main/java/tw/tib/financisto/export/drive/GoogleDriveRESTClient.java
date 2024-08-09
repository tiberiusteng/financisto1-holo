package tw.tib.financisto.export.drive;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.DriveFolder;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
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

import tw.tib.financisto.R;
import tw.tib.financisto.export.Export;
import tw.tib.financisto.export.ImportExportException;
import tw.tib.financisto.utils.MyPreferences;

public class GoogleDriveRESTClient {
    private final Context context;
    private Drive googleDriveService;

    private static final String TAG = "GoogleDriveRESTClient";

    private static final String PICTURES_DIRNAME = "pictures";

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

    public String getFolderID(String folderName, String parentFolderId, boolean createIfNotExist) throws Exception {
        String folderID = null;

        String query = "mimeType = '" + DriveFolder.MIME_TYPE + "' and name = '" + folderName + "'";
        if (parentFolderId != null) {
            query += " and '" + parentFolderId + "' in parents";
        }

        FileList result = googleDriveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .execute();

        if (!result.getFiles().isEmpty()) {
            folderID = result.getFiles().get(0).getId();
            Log.i(TAG, String.format("Got folder '%s' ID: '%s'", folderName, folderID));
        } else if (createIfNotExist) {
            // Backup folder not exist yet, create it
            File metadata = new File()
                    .setMimeType(DriveFolder.MIME_TYPE)
                    .setName(folderName);

            if (parentFolderId != null) {
                metadata.setParents(Collections.singletonList(parentFolderId));
            } else {
                metadata.setParents(Collections.singletonList("root"));
            }

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
        return getFolderID(getBackupFolderName(), null, createIfNotExist);
    }

    public String getPictureFolderID(boolean createIfNotExist) throws Exception {
        String backupFolderId = getFolderID(getBackupFolderName(), null, createIfNotExist);
        return getFolderID(PICTURES_DIRNAME, backupFolderId, createIfNotExist);
    }

    public String getPictureFileID(String fileName) throws Exception {
        Log.i(TAG, "getting id for picture: " + fileName);
        String pictureFolderId = getPictureFolderID(false);
        if (pictureFolderId == null) {
            Log.i(TAG, "picture folder not exist");
            return null;
        }
        List<File> fileList = googleDriveService.files().list()
                .setQ("'" + pictureFolderId + "' in parents and trashed = false and name = '" + fileName + "'")
                .setSpaces("drive")
                .setFields("files(id)")
                .execute().getFiles();
        if (fileList.isEmpty()) {
            Log.i(TAG, "file not found");
            return null;
        }
        String fileId = fileList.get(0).getId();
        Log.i(TAG, "file ID: " + fileId);
        return fileId;
    }

    public List<GoogleDriveFileInfo> listFiles() throws Exception {
        String folderID = getBackupFolderID(false);
        if (folderID == null) {
            throw new ImportExportException(R.string.gdocs_folder_not_configured);
        }

        ArrayList<GoogleDriveFileInfo> result = new ArrayList<>();

        List<File> fileList = googleDriveService.files().list()
                .setQ("'" + folderID + "' in parents and trashed = false and mimeType ='" + Export.BACKUP_MIME_TYPE + "'")
                .setSpaces("drive")
                .setFields("files(id,name,createdTime)")
                .execute().getFiles();

        for (File f : fileList) {
            result.add(new GoogleDriveFileInfo(f.getId(), f.getName(), f.getCreatedTime()));
        }

        return result;
    }

    public void uploadFile(Uri uri, String mimeType, String folderID) throws Exception {
        String fileName = uri.getLastPathSegment();

        File fileMetadata = new File()
                .setParents(Collections.singletonList(folderID))
                .setMimeType(mimeType)
                .setName(fileName.substring(fileName.lastIndexOf("/")+1));

        InputStreamContent mediaContent = new InputStreamContent(mimeType,
                context.getContentResolver().openInputStream(uri));

        File uploadedFile = googleDriveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        String fileId = uploadedFile.getId();
        Log.i(TAG, "Created file mime: " + mimeType + ", id: " + fileId);
    }

    public void uploadBackup(Uri uri) throws Exception {
        String folderID = getBackupFolderID(false);
        if (folderID == null) {
            throw new ImportExportException(R.string.gdocs_folder_not_configured);
        }
        uploadFile(uri, Export.BACKUP_MIME_TYPE, folderID);
    }

    public InputStream getFileAsStream(String fileID) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        googleDriveService.files().get(fileID).executeMediaAndDownloadTo(outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public HttpResponse getFile(String fileID) throws Exception {
        return googleDriveService.files().get(fileID).executeMedia();
    }
}
