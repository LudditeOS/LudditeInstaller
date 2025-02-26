package com.example.ludditeinstaller;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

public class AppStore {
    private LogDisplay logDisplay;
    private static final String TAG = "AppStore";
    private static final String DOWNLOADS_DIR = "apk_downloads";
    private AppStoreCallback callback;

    public interface AppStoreCallback {
        void onDownloadComplete();
        void onDownloadFailed(String error);
    }

    public AppStore(LogDisplay logDisplay) {
        this.logDisplay = logDisplay;
    }

    public void setCallback(AppStoreCallback callback) {
        this.callback = callback;
    }

    public void downloadAndInstallApk(Context context, String apkUrl, String fileName) {
        logDisplay.log(TAG, "Starting download and install process for: " + apkUrl);
        Handler mainHandler = new Handler(context.getMainLooper());

        new Thread(() -> {
            try {
                logDisplay.log(TAG, "Starting download...");
                Uri contentUri = downloadApk(context, apkUrl, fileName);
                logDisplay.log(TAG, "Download completed, contentUri: " + contentUri);

                if (contentUri != null) {
                    mainHandler.post(() -> {
                        logDisplay.log(TAG, "Initiating install process");
                        installApk(context, contentUri);
                        // Notify download is complete
                        if (callback != null) {
                            callback.onDownloadComplete();
                        }
                    });
                } else {
                    logDisplay.log(TAG, "Download failed - contentUri is null");
                    if (callback != null) {
                        mainHandler.post(() -> callback.onDownloadFailed("Download failed - contentUri is null"));
                    }
                }
            } catch (Exception e) {
                final String errorMessage = e.getMessage();
                logDisplay.log(TAG, "Error in download/install process: " + errorMessage);
                if (callback != null) {
                    mainHandler.post(() -> callback.onDownloadFailed(errorMessage));
                }
            }
        }).start();
    }

    private Uri downloadApk(Context context, String apkUrl, String fileName) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle(fileName);
            request.setDestinationInExternalFilesDir(context, DOWNLOADS_DIR, fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

            final long downloadId = dm.enqueue(request);
            logDisplay.log(TAG, "Download started with ID: " + downloadId);

            int maxAttempts = 30;
            int attempts = 0;

            while (attempts < maxAttempts) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = dm.query(query);

                if (cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (statusIndex != -1) {
                        int status = cursor.getInt(statusIndex);
                        int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                        int reason = reasonIndex != -1 ? cursor.getInt(reasonIndex) : -1;

                        logDisplay.log(TAG, "Download status: " + getStatusString(status) +
                                ", Reason: " + getReasonString(reason));

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                            if (uriIndex != -1) {
                                String uriString = cursor.getString(uriIndex);
                                cursor.close();
                                return Uri.parse(uriString);
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            cursor.close();
                            throw new RuntimeException("Download failed with reason: " + getReasonString(reason));
                        }
                    }
                }
                cursor.close();

                attempts++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Download interrupted", e);
                }
            }

            // If we get here, download timed out
            dm.remove(downloadId);
            throw new RuntimeException("Download timed out after " + maxAttempts + " seconds");

        } catch (Exception e) {
            logDisplay.log(TAG, "Error in downloadApk: " + e.getMessage());
            throw e;
        }
    }
    private String getStatusString(int status) {
        switch (status) {
            case DownloadManager.STATUS_FAILED:
                return "STATUS_FAILED";
            case DownloadManager.STATUS_PAUSED:
                return "STATUS_PAUSED";
            case DownloadManager.STATUS_PENDING:
                return "STATUS_PENDING";
            case DownloadManager.STATUS_RUNNING:
                return "STATUS_RUNNING";
            case DownloadManager.STATUS_SUCCESSFUL:
                return "STATUS_SUCCESSFUL";
            default:
                return "STATUS_UNKNOWN_" + status;
        }
    }

    private String getReasonString(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME: return "ERROR_CANNOT_RESUME";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND: return "ERROR_DEVICE_NOT_FOUND";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS: return "ERROR_FILE_ALREADY_EXISTS";
            case DownloadManager.ERROR_FILE_ERROR: return "ERROR_FILE_ERROR";
            case DownloadManager.ERROR_HTTP_DATA_ERROR: return "ERROR_HTTP_DATA_ERROR";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE: return "ERROR_INSUFFICIENT_SPACE";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS: return "ERROR_TOO_MANY_REDIRECTS";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE: return "ERROR_UNHANDLED_HTTP_CODE";
            case DownloadManager.ERROR_UNKNOWN: return "ERROR_UNKNOWN";
            case DownloadManager.PAUSED_QUEUED_FOR_WIFI: return "PAUSED_QUEUED_FOR_WIFI";
            case DownloadManager.PAUSED_UNKNOWN: return "PAUSED_UNKNOWN";
            case DownloadManager.PAUSED_WAITING_FOR_NETWORK: return "PAUSED_WAITING_FOR_NETWORK";
            case DownloadManager.PAUSED_WAITING_TO_RETRY: return "PAUSED_WAITING_TO_RETRY";
            default: return "UNKNOWN_REASON_" + reason;
        }
    }

    private void installApk(Context context, Uri fileUri) {
        try {
            File file = new File(fileUri.getPath());
            Uri contentUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider",
                    file);

            Intent intent = new Intent("com.luddite.app.store.INSTALL_PACKAGE");
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            logDisplay.log(TAG, "Error in installApk: " + e.getMessage());
            if (callback != null) {
                callback.onDownloadFailed("Error during installation: " + e.getMessage());
            }
        }
    }
}