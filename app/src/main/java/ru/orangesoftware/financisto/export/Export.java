/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.export;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import ru.orangesoftware.financisto.export.drive.GoogleDriveRESTClient;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;
import ru.orangesoftware.financisto.utils.MyPreferences;

public abstract class Export {

    public static final String BACKUP_DIRECTORY_NAME = "backups";
    public static final String BACKUP_MIME_TYPE = "application/x.financisto+gzip";

    private final Context context;
    private final boolean useGzip;

    protected Export(Context context, boolean useGzip) {
        this.context = context;
        this.useGzip = useGzip;
    }

    public Uri export() throws Exception {
        Uri backupFolderUri = Uri.parse(getBackupFolder(context));
        String backupFolderId = DocumentsContract.getTreeDocumentId(backupFolderUri);
        Log.i("Financisto", "backupPathId: " + backupFolderId);
        Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(backupFolderUri, backupFolderId);
        Log.i("Financisto", "dirUri: " + dirUri.toString());
        Uri backupFileUri = DocumentsContract.createDocument(context.getContentResolver(),
                dirUri, Export.BACKUP_MIME_TYPE, generateFilename());
        Log.i("Financisto", "backupFileUri: " + backupFileUri.toString());
        OutputStream outputStream = context.getContentResolver().openOutputStream(backupFileUri);
        try {
            if (useGzip) {
                export(new GZIPOutputStream(outputStream));
            } else {
                export(outputStream);
            }
        } finally {
            outputStream.flush();
            outputStream.close();
        }
        return backupFileUri;
    }

    protected void export(OutputStream outputStream) throws Exception {
        generateBackup(outputStream);
    }

    public String generateFilename() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");
        return df.format(new Date()) + getExtension();
    }

    public byte[] generateBackupBytes() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream out = new BufferedOutputStream(new GZIPOutputStream(outputStream));
        generateBackup(out);
        return outputStream.toByteArray();
    }

    private void generateBackup(OutputStream outputStream) throws Exception {
        OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
        try (BufferedWriter bw = new BufferedWriter(osw, 65536)) {
            writeHeader(bw);
            writeBody(bw);
            writeFooter(bw);
        }
    }

    protected abstract void writeHeader(BufferedWriter bw) throws IOException, NameNotFoundException;

    protected abstract void writeBody(BufferedWriter bw) throws IOException;

    protected abstract void writeFooter(BufferedWriter bw) throws IOException;

    protected abstract String getExtension();

    public static String getBackupFolder(Context context) {
        String backupFolderUri = MyPreferences.getDatabaseBackupFolder(context);
        Log.i("Financisto", "getBackupFolder: " + backupFolderUri);
        return backupFolderUri;
    }

    public static void uploadBackupFileToDropbox(Context context, Uri backupFileUri) throws Exception {
        Dropbox dropbox = new Dropbox(context);
        dropbox.uploadFile(backupFileUri);
    }

    public static void uploadBackupFileToGoogleDrive(Context context, Uri backupFileUri) throws Exception {
        GoogleDriveRESTClient googleDriveRESTClient = new GoogleDriveRESTClient(context);
        googleDriveRESTClient.uploadFile(backupFileUri);
    }

}
