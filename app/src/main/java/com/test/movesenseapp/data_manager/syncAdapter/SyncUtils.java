package com.test.movesenseapp.data_manager.syncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

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
import java.util.concurrent.TimeUnit;

/**
 * Static helper methods for working with the sync framework.
 */
public class SyncUtils {

    static final String TAG = SyncUtils.class.getName();

    private static final long SYNC_FREQUENCY = TimeUnit.HOURS.toSeconds(1/60);  // 1 hour (in seconds)
    // Sync interval constants
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 1;
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;

    // Both the values below must match the account type specified in
    // res/xml/syncadapter.xml and authenticator.xml
    public static final String ACCOUNT_TYPE = "https://smr.unl.edu/";
    public static final String CONTENT_AUTHORITY = ".syncAdapter.provider";

    // this has to change and has to be unique per user
    // this can be the userid. For google accounts, this is your gmail ID.
    private static final String ACCOUNT_NAME = "bridgetpeterkin@gmail.com";

    /**
     * Create an entry for this application in the system account list,
     * if it isn't already there.
     *
     * @param context Context
     */
    public static void createSyncAccount(Context context) {

        // Create account, if it's missing. (Either first run, or user has deleted account.)
        Account account = new Account(ACCOUNT_NAME, SyncUtils.ACCOUNT_TYPE);
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(account, null, null)) {

            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);

            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);

            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(account, CONTENT_AUTHORITY,
                    new Bundle(), SYNC_INTERVAL);
            Log.i(TAG, "Account added successfully!");

        } else {
            Log.d(TAG, "Account already added, not adding again...");
        }
    }


    private static Account getAccount(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(SyncUtils.ACCOUNT_TYPE);
        Account account;
        if (accounts.length > 0) {
            account = accounts[0];
        } else {
            account = null;
        }

        return account;
    }

    /**
     * Helper method to trigger an immediate sync ("refresh").
     * <p>
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     * <p>
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void forceRefreshAll(Context context) {
        Bundle bundle = new Bundle();
        // Disable sync backoff and ignore sync preferences.
        // In other words...perform sync NOW!
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                getAccount(context),        // Sync account
                CONTENT_AUTHORITY,          // Content authority
                bundle);         // Extras
        Log.d(TAG, "Sync requested");
    }


}