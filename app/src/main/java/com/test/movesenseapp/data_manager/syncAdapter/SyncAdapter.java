package com.test.movesenseapp.data_manager.syncAdapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = "SyncDebug";
    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver contentResolver;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        contentResolver = context.getContentResolver();
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        contentResolver = context.getContentResolver();
    }

    /*
     * Specify the code you want to run in the sync adapter. The entire
     * sync adapter runs in a background thread, so you don't have to set
     * up your own background processing.
     */
    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {

        Log.i(LOG_TAG,"OnPerformSync");

        Log.i(LOG_TAG,"Calling uploadFilesFirebase()");
        uploadFiles();
    }

    public static void uploadFiles() {
        List<File> logsFileList = queryLogsFile();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        String currentISO8601Timestamp = String.format("%tFT%<tTZ.%<tL",
                Calendar.getInstance(TimeZone.getTimeZone("Z")));

        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference().child("SmartBandageFiles/");

        for(File f : logsFileList) {
            Uri file = Uri.fromFile(f);
            StorageReference smartBandageRef = storageRef.child(currentISO8601Timestamp+ "/" +  f.getName() + "/");

            UploadTask uploadTask =smartBandageRef.putFile(file);

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d(LOG_TAG, "FILE UPLOADED SUCCESSFULLY");
                }
            });
        }

    }

    private static List<File> queryLogsFile() {
        List<File> logsFileList = new ArrayList<File>();

        // Query logs from Movesense folder
        logsFileList.clear();

        File externalDirectory = Environment.getExternalStorageDirectory();
        File dirFile = new File(externalDirectory + "/Movesense");
        if (dirFile.exists()) {
            File[] logs = dirFile.listFiles();

            // Check if any file exists
            if (logs != null) {
                for (File file : logs) {
                    logsFileList.add(file);
                    Log.e(LOG_TAG, "Query File: " + file.getName());
                }

            } else {
                Log.e(LOG_TAG, "Query file failed. File[] = null");

            }
        } else {
            Log.e(LOG_TAG, "Movesense logs dir not exists.");

        }
        return logsFileList;
    }

    @Override
    public void onSyncCanceled() {
        super.onSyncCanceled();
        Log.i(LOG_TAG, "");
    }

    @Override
    public void onSecurityException(Account account, Bundle extras, String authority, SyncResult syncResult) {
        super.onSecurityException(account, extras, authority, syncResult);
        Log.i(LOG_TAG,"Extras: " + extras);
    }





    }







