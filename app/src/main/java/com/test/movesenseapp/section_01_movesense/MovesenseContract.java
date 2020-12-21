package com.test.movesenseapp.section_01_movesense;


import android.content.BroadcastReceiver;
import android.content.Intent;

import com.polidea.rxandroidble2.RxBleDevice;
import com.test.movesenseapp.BasePresenter;
import com.test.movesenseapp.BaseView;

public interface MovesenseContract {

    interface Presenter extends BasePresenter {
        void startScanning();

        void stopScanning();

        void onBluetoothResult(int requestCode, int resultCode, Intent data);
    }

    interface View extends BaseView<Presenter> {
        void displayScanResult(RxBleDevice bluetoothDevice, int rssi);

        void displayErrorMessage(String message);

        void registerReceiver(BroadcastReceiver broadcastReceiver);

        void unregisterReceiver(BroadcastReceiver broadcastReceiver);

        boolean checkLocationPermissionIsGranted();
    }
}
