package com.example.ludditeinstaller;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

public class AppStore {
    private static final String DOWNLOADS_DIR = "apk_downloads";

    public void downloadAndInstallApk(Context context, String apkUrl, String fileName) {
        Uri contentUri = downloadApk(context, apkUrl, fileName);
        installApk(context, contentUri);
    }

    private Uri downloadApk(Context context, String apkUrl, String fileName) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle(fileName);
        request.setDestinationInExternalFilesDir(context, DOWNLOADS_DIR, fileName);

        long downloadId = dm.enqueue(request);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = dm.query(query);

        Uri fileUri = null;
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            if (columnIndex != -1) {
                String uriString = cursor.getString(columnIndex);
                if (uriString != null) {
                    fileUri = Uri.parse(uriString);
                }
            }
            cursor.close();
        }

        return fileUri;
    }

    private void installApk(Context context, Uri contentUri) {
        Intent intent = new Intent("com.luddite.app.store.INSTALL_PACKAGE");
        intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }
}