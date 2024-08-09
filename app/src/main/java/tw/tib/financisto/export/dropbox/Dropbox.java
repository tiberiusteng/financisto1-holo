/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package tw.tib.financisto.export.dropbox;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.util.IOUtil;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;

import tw.tib.financisto.BuildConfig;
import tw.tib.financisto.R;
import tw.tib.financisto.export.ImportExportException;
import tw.tib.financisto.utils.MyPreferences;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Dropbox {
    private static final String TAG = "Dropbox";

    private static final String APP_KEY = "aenijec51r68hsv";

    private static final String PICTURES_DIR = "/pictures";

    private final Context context;

    private boolean startedAuth = false;
    private DbxClientV2 dropboxClient;

    public Dropbox(Context context) {
        this.context = context;
    }

    public void startAuth() {
        Log.d(TAG, "startAuth");
        startedAuth = true;
        Auth.startOAuth2PKCE(context, APP_KEY, getDbxRequestConfig(),
                Arrays.asList("files.metadata.read", "files.content.read", "files.content.write"));
    }

    public void completeAuth() {
        Log.d(TAG, "enter completeAuth, startedAuth = " + startedAuth);
        try {
            DbxCredential dbxCredential = Auth.getDbxCredential();
            if (startedAuth && dbxCredential != null) {
                try {
                    Log.d(Dropbox.class.getSimpleName(), "dbxCredential = " + dbxCredential.toString());
                    Toast.makeText(context, R.string.dropbox_authorized, Toast.LENGTH_LONG).show();
                    MyPreferences.storeDropboxKeys(context, dbxCredential.toString());
                } catch (IllegalStateException e) {
                    Log.i(Dropbox.class.getSimpleName(), "Error authenticating Dropbox", e);
                }
            }
        } finally {
            startedAuth = false;
        }
    }

    public void deAuth() {
        MyPreferences.removeDropboxKeys(context);
        if (dropboxClient != null) {
            try {
                dropboxClient.auth().tokenRevoke();
            } catch (DbxException e) {
                Log.e("Financisto", "Unable to unlink Dropbox", e);
            }
        }
    }

    private DbxRequestConfig getDbxRequestConfig() {
        return DbxRequestConfig.newBuilder("financisto/" + BuildConfig.VERSION_CODE).build();
    }

    private boolean authSession() {
        String serializedCredential = MyPreferences.getDropboxAuthToken(context);
        if (serializedCredential != null) {
            try {
                if (dropboxClient == null) {
                    DbxCredential dbxCredential = DbxCredential.Reader.readFully(serializedCredential);
                    dropboxClient = new DbxClientV2(getDbxRequestConfig(), dbxCredential);
                }
                return true;
            } catch (JsonReadException e) {
                return false;
            }
        }
        return false;
    }

    public FileMetadata uploadFile(Uri uri, String path) throws Exception {
        if (authSession()) {
            try {
                InputStream is = context.getContentResolver().openInputStream(uri);
                try {
                    String fileName = uri.getLastPathSegment();
                    FileMetadata fileMetadata = dropboxClient.files()
                            .uploadBuilder(path + fileName.substring(fileName.lastIndexOf("/") + 1))
                            .withMode(WriteMode.ADD)
                            .uploadAndFinish(is);
                    Log.i(TAG, "Dropbox: The uploaded file's rev is: " + fileMetadata.getRev());
                    return fileMetadata;
                } finally {
                    IOUtil.closeInput(is);
                }
            } catch (Exception e) {
                Log.e(TAG, "Dropbox: Something wrong", e);
                throw new ImportExportException(R.string.dropbox_error, e);
            }
        } else {
            throw new ImportExportException(R.string.dropbox_auth_error);
        }
    }

    public FileMetadata uploadBackupFile(Uri uri) throws Exception {
        return uploadFile(uri, "/");
    }

    public FileMetadata uploadPictureFile(Uri uri) throws Exception {
        return uploadFile(uri, PICTURES_DIR + "/");
    }

    List<String> listFiles() throws Exception {
        if (authSession()) {
            try {
                List<String> files = new ArrayList<String>();
                ListFolderResult listFolderResult = dropboxClient.files().listFolder("");
                for (Metadata metadata : listFolderResult.getEntries()) {
                    String name = metadata.getName();
                    if (name.endsWith(".backup")) {
                        files.add(name);
                    }
                }
                Collections.sort(files, new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s2.compareTo(s1);
                    }
                });
                return files;
            } catch (Exception e) {
                Log.e("Financisto", "Dropbox: Something wrong", e);
                throw new ImportExportException(R.string.dropbox_error, e);
            }
        } else {
            throw new ImportExportException(R.string.dropbox_auth_error);
        }
    }

    public InputStream getFileAsStream(String fileName, String path) throws Exception {
        if (authSession()) {
            try {
                return dropboxClient.files().downloadBuilder(path + fileName).start().getInputStream();
            } catch (Exception e) {
                Log.e(TAG, "Dropbox: Something wrong", e);
                throw new ImportExportException(R.string.dropbox_error, e);
            }
        } else {
            throw new ImportExportException(R.string.dropbox_auth_error);
        }
    }

    public InputStream getBackupFileAsStream(String backupFileName) throws Exception {
        return getFileAsStream(backupFileName, "/");
    }

    public DbxDownloader<FileMetadata> getPictureFile(String fileName) {
        if (authSession()) {
            try {
                return dropboxClient.files().downloadBuilder(PICTURES_DIR + "/" + fileName).start();
            } catch (Exception e) {
                Log.e(TAG, "get picture file error", e);
                return null;
            }
        }
        return null;
    }
}