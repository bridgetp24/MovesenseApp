package com.test.movesenseapp.data_manager;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class StorageUploader extends AppCompatActivity {

    public String LOG_TAG = "StorageUploader";
    FirebaseAuth mAuth;

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new  OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Log.d(LOG_TAG, "Signed in anonymously");

            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e(LOG_TAG, "signInAnonymously:FAILURE", exception);
                    }
                });
    }

    public void uploadFilesFirebase() {

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null) {
            uploadFiles();
        }else {
            signInAnonymously();
            uploadFiles();
            Log.d(LOG_TAG, "FILE UPLOAD COMPLETE");

        }

    }

    public void uploadFileFirebase(File f) {
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null) {
            uploadFile(f);
        }else {
            signInAnonymously();
            uploadFile(f);
            Log.d(LOG_TAG, "FILE UPLOAD COMPLETE");
        }

    }
    public void uploadFiles() {
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

    public void uploadFile(File f) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String currentISO8601Timestamp = String.format("%tFT%<tTZ.%<tL",
                Calendar.getInstance(TimeZone.getTimeZone("Z")));

        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference().child("SmartBandageFiles/");


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



    private List<File> queryLogsFile() {
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



}
