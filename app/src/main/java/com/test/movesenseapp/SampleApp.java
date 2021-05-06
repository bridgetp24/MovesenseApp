package com.test.movesenseapp;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.test.movesenseapp.bluetooth.BluetoothStatusMonitor;
import com.test.movesenseapp.bluetooth.MdsRx;
import com.test.movesenseapp.bluetooth.RxBle;
import com.test.movesenseapp.data_manager.User;
import com.test.movesenseapp.utils.Util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Application for making all initializations
 */
public class SampleApp extends Application {



    /**
     * User field that stores instance of firebase app as user id
     */
    private static User user;
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize RxBleWrapper
        Log.d("SampleApp", "Running initialize RxBle");
        RxBle.Instance.initialize(this);

        // Copy necessary configuration file to proper place
        copyRawResourceToFile(R.raw.kompostisettings, "KompostiSettings.xml");

        // Initialize MDS
        MdsRx.Instance.initialize(this);

        BluetoothStatusMonitor.INSTANCE.initBluetoothStatus();
        if (user == null) {
            user = new User();

        }


    }



    @Override
    public void onTerminate() {
        super.onTerminate();

    }

    /**
     * Copy raw resource file to file.
     *
     * @param resourceId Resource id.
     * @param fileName   Target file name.
     */
    private void copyRawResourceToFile(int resourceId, String fileName) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = getResources().openRawResource(resourceId);
            out = openFileOutput(fileName, Context.MODE_PRIVATE);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not copy configuration file to: " + fileName);
        } finally {
            Util.safeClose(out);
            Util.safeClose(in);
        }
    }

    public static User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
