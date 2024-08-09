/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tw.tib.financisto.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.files.FileMetadata;
import com.google.api.client.http.HttpResponse;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

import tw.tib.financisto.R;
import tw.tib.financisto.export.drive.GoogleDriveRESTClient;
import tw.tib.financisto.export.dropbox.Dropbox;

public class PicturesUtil {
    private static final String TAG = "PicturesUtil";
    private static final String PICTURES_DIRNAME = "pictures";
    private static final String PICTURES_MIME_TYPE = "image/jpeg";

    public static void showImage(Context context, ImageView imageView, TextView imageDescView, String pictureFileName) {
        if (pictureFileName == null || imageView == null) return;
        var executor = Executors.newSingleThreadExecutor();
        var handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                boolean haveFile = true;
                Uri pictureUri = getPictureFileUri(context, pictureFileName);
                DocumentFile pictureFile = DocumentFile.fromSingleUri(context, pictureUri);

                if (!pictureFile.exists()) {
                    haveFile = false;

                    if (MyPreferences.isGoogleDriveDownloadPictures(context)) {
                        handler.post(() -> imageDescView.setText(R.string.downloading_picture_from_google_drive));

                        try {
                            GoogleDriveRESTClient gdrive = new GoogleDriveRESTClient(context);
                            String fileId = gdrive.getPictureFileID(pictureFileName);
                            if (fileId != null) {
                                HttpResponse resp = gdrive.getFile(fileId);
                                Uri pictureFolderUri = getPictureFolderUri(context);
                                Uri targetFileUri = DocumentsContract.createDocument(context.getContentResolver(),
                                        pictureFolderUri, PICTURES_MIME_TYPE, pictureFileName);
                                OutputStream outputStream = context.getContentResolver().openOutputStream(targetFileUri);
                                resp.download(outputStream);
                                outputStream.close();
                                haveFile = true;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "downloading from Google Drive failed", e);
                        }
                    }

                    if (!haveFile && MyPreferences.isDropboxDownloadPictures(context)) {
                        handler.post(() -> imageDescView.setText(R.string.downloading_picture_from_dropbox));

                        try {
                            Dropbox dropbox = new Dropbox(context);
                            DbxDownloader<FileMetadata> resp = dropbox.getPictureFile(pictureFileName);
                            Uri pictureFolderUri = getPictureFolderUri(context);
                            Uri targetFileUri = DocumentsContract.createDocument(context.getContentResolver(),
                                    pictureFolderUri, PICTURES_MIME_TYPE, pictureFileName);
                            OutputStream outputStream = context.getContentResolver().openOutputStream(targetFileUri);
                            resp.download(outputStream);
                            outputStream.close();
                            haveFile = true;
                        } catch (Exception e) {
                            Log.e(TAG, "downloading from Dropbox failed", e);
                        }
                    }
                }
                if (haveFile) {
                    handler.post(() -> {
                        var metrics = new DisplayMetrics();
                        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
                        int shortSide = (int) (Math.min(metrics.widthPixels, metrics.heightPixels) * 0.8);

                        imageDescView.setText(pictureFileName);
                        Glide.with(context)
                                .load(pictureUri)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .override(shortSide)
                                .into(imageView);
                    });
                } else {
                    handler.post(() -> imageDescView.setText(String.format(
                            context.getString(R.string.missing_picture_file), pictureFileName)));
                }
            } catch (Exception e) {
                Log.e(TAG, "showImage error", e);
            }
        });
    }

    public static Uri getPictureFolderUri(Context context) {
        try {
            // backup folder
            Uri backupFolderUri = Uri.parse(MyPreferences.getDatabaseBackupFolder(context));
            Log.i(TAG, "backupFolderUri: " + backupFolderUri);
            String backupFolderId = DocumentsContract.getTreeDocumentId(backupFolderUri);
            Log.i(TAG, "backupFolderId: " + backupFolderId);

            Uri picturesFolderUriWithTree = DocumentsContract.buildDocumentUriUsingTree(backupFolderUri, backupFolderId + "/" + PICTURES_DIRNAME);
            Log.i(TAG, "picturesFolderUriWithTree: " + picturesFolderUriWithTree);

            // pictures folder
            DocumentFile picturesFolder = DocumentFile.fromTreeUri(context, picturesFolderUriWithTree);
            if (picturesFolder != null && picturesFolder.exists()) {
                Uri picturesFolderUri = picturesFolder.getUri();
                Log.i(TAG, "pictureFolder exists, uri: " + picturesFolderUri);
                Log.i(TAG, "pictureFolder document id: " + DocumentsContract.getDocumentId(picturesFolderUri));
                return picturesFolderUri;
            }

            Log.i(TAG, "creating pictures folder");
            Uri backupFolderUriWithTree = DocumentsContract.buildDocumentUriUsingTree(backupFolderUri, backupFolderId);
            Uri picturesFolderUri = DocumentsContract.createDocument(context.getContentResolver(),
                    backupFolderUriWithTree, DocumentsContract.Document.MIME_TYPE_DIR, PICTURES_DIRNAME);
            Log.i(TAG, "picturesFolderUri: " + picturesFolderUri);

            return picturesFolderUri;

        } catch (Exception e) {
            Log.e(TAG, "check backup folder writable fail", e);
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.fail)
                .setMessage(R.string.backup_folder_not_configured)
                .setPositiveButton(R.string.ok, null)
                .show();

        return null;
    }

    public static Uri getPictureFileUri(Context context, String fileName) {
        Uri backupFolderUri = Uri.parse(MyPreferences.getDatabaseBackupFolder(context));
        Log.i(TAG, "backupFolderUri: " + backupFolderUri);
        String backupFolderId = DocumentsContract.getTreeDocumentId(backupFolderUri);
        Log.i(TAG, "backupFolderId: " + backupFolderId);
        Uri pictureUri = DocumentsContract.buildDocumentUriUsingTree(backupFolderUri, backupFolderId + "/" + PICTURES_DIRNAME + "/" + fileName);
        Log.i(TAG, "pictureUri: " + pictureUri);

        return pictureUri;
    }

    public static String generateFileName() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");
        return df.format(new Date());
    }

    public static String saveSelectedPicture(Context context, Uri source) {
        Uri pictureFolderUri = getPictureFolderUri(context);

        DocumentFile sourceFile = DocumentFile.fromSingleUri(context, source);
        String sourceFileName = sourceFile.getName();
        Log.i(TAG, "source name: " + sourceFileName);
        String sourceMimeType = sourceFile.getType();
        Log.i(TAG, "source mime: " + sourceMimeType);

        try {
            Uri targetFileUri = DocumentsContract.createDocument(context.getContentResolver(),
                    pictureFolderUri, PICTURES_MIME_TYPE, generateFileName());
            Log.i(TAG, "targetFileUri: " + targetFileUri);

            InputStream inputStream = context.getContentResolver().openInputStream(source);
            OutputStream outputStream = context.getContentResolver().openOutputStream(targetFileUri);

            IOUtils.copy(inputStream, outputStream);

            DocumentFile targetFile = DocumentFile.fromSingleUri(context, targetFileUri);
            Log.i(TAG, "targetFile name: " + targetFile.getName());

            if (MyPreferences.isGoogleDriveUploadPictures(context)) {
                var executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        GoogleDriveRESTClient client = new GoogleDriveRESTClient(context);
                        String pictureFolderId = client.getPictureFolderID(true);
                        client.uploadFile(targetFileUri, sourceMimeType, pictureFolderId);
                    } catch (Exception e) {
                        Log.e(TAG, "upload picture to Google Drive failed", e);
                    }
                });
            }

            if (MyPreferences.isDropboxUploadPictures(context)) {
                var executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        Dropbox dropbox = new Dropbox(context);
                        dropbox.uploadPictureFile(targetFileUri);
                    } catch (Exception e) {
                        Log.e(TAG, "upload picture to Dropbox failed", e);
                    }
                });
            }

            return targetFile.getName();

        } catch (Exception e) {
            Log.i(TAG, "copy file failed", e);
            return null;
        }
    }
}