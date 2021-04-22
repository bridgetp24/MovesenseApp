package com.test.movesenseapp.data_manager;

import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.test.movesenseapp.BuildConfig;
import com.test.movesenseapp.R;
import com.test.movesenseapp.data_manager.syncAdapter.SyncUtils;
import com.test.movesenseapp.logs.LogsListAdapter;

import java.io.File;
import java.util.ArrayList;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;


public class DataManagerActivity extends AppCompatActivity {

    @BindView(R.id.resultTextView) TextView resultTextView;
    @BindView(R.id.logsFileListView) ListView logsFileListView;


    private final String LOG_TAG = "driveDebug";

    private final List<File> logsFileList = new ArrayList<>();
    private LogsListAdapter logsFileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_logs_to_cloud);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Saved Data");
        }

        // Create Adapter for listView
        logsFileAdapter = new LogsListAdapter(logsFileList);
        logsFileListView.setAdapter(logsFileAdapter);


        // Query logs from Movesense folder
        queryLogsFile();

    }

    private void queryLogsFile() {
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

                logsFileAdapter.notifyDataSetChanged();
            } else {
                Log.e(LOG_TAG, "Query file failed. File[] = null");
                resultTextView.setText("Logs directory is empty or not loaded. Please subscribe sensor and back.");
            }
        } else {
            Log.e(LOG_TAG, "Movesense logs dir not exists.");
            resultTextView.setText("Logs directory not exists");
        }
    }


    @OnClick(R.id.UploadData)
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.UploadData:
                Log.d(LOG_TAG, "upload data");
                SyncUtils.forceRefreshAll(com.test.movesenseapp.data_manager.DataManagerActivity.this);
                Toast.makeText(com.test.movesenseapp.data_manager.DataManagerActivity.this, "Files Uploading to Cloud", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void uploadFileFirebase(File fileToSend) {
        //update from Storage Uploader activity with mAuth used
        StorageUploader uploader = new StorageUploader();
        uploader.uploadFileFirebase(fileToSend);
    }


    @OnItemClick({R.id.logsFileListView})
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Show dialog for open / send file

            final File clickedFile = logsFileList.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("Choose a file action")
                    .setItems(new CharSequence[]{"Open file", "Send file to Cloud"},
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0:
                                            // Open File
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_VIEW);
                                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            Uri uri = FileProvider.getUriForFile(com.test.movesenseapp.data_manager.DataManagerActivity.this,
                                                    BuildConfig.APPLICATION_ID, clickedFile);
                                            intent.setDataAndType(uri, getMimeType(clickedFile.getName()));
                                            startActivity(intent);
                                            break;

                                        case 1:
                                            // Send file to Firebase storage
                                            uploadFileFirebase(clickedFile);
                                            break;
                                    }
                                }
                            })
                    .show();



    }

    private String getMimeType(String url) {
        String parts[] = url.split("\\.");
        String extension = parts[parts.length - 1];
        String type = null;
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.send_logs_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }


}
