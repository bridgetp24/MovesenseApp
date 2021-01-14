package com.test.movesenseapp.section_01_movesense.tests;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;
import com.movesense.mds.internal.connectivity.BleManager;
import com.movesense.mds.internal.connectivity.MovesenseConnectedDevices;
import com.polidea.rxandroidble2.RxBleDevice;
import com.test.movesenseapp.BaseActivity;
import com.test.movesenseapp.R;
import com.test.movesenseapp.bluetooth.ConnectionLostDialog;
import com.test.movesenseapp.csv.CsvLogger;
import com.test.movesenseapp.model.AngularVelocity;
import com.test.movesenseapp.model.LinearAcceleration;
import com.test.movesenseapp.model.MagneticField;
import com.test.movesenseapp.section_01_movesense.MovesenseActivity;
import com.test.movesenseapp.utils.FormatHelper;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class MultiSubscribeActivity extends BaseActivity implements BleManager.IBleConnectionMonitor {

    @BindView(R.id.switchSubscriptionLinearAcc)
    SwitchCompat switchSubscriptionLinearAcc;
    @BindView(R.id.x_axis_linearAcc_textView) TextView xAxisLinearAccTextView;
    @BindView(R.id.y_axis_linearAcc_textView) TextView yAxisLinearAccTextView;
    @BindView(R.id.z_axis_linearAcc_textView) TextView zAxisLinearAccTextView;
    @BindView(R.id.switchSubscriptionECG)
    SwitchCompat switchSubscriptionMagneticField;
    @BindView(R.id.x_axis_magneticField_textView) TextView xAxisMagneticFieldTextView;
    @BindView(R.id.y_axis_magneticField_textView) TextView yAxisMagneticFieldTextView;
    @BindView(R.id.heart_rate_textView) TextView zAxisMagneticFieldTextView;
    @BindView(R.id.switchSubscriptionPulseOximeter)
    SwitchCompat switchSubscriptionAngularVelocity;
    @BindView(R.id.x_axis_angularVelocity_textView) TextView xAxisAngularVelocityTextView;
    @BindView(R.id.y_axis_angularVelocity_textView) TextView yAxisAngularVelocityTextView;
   // @BindView(R.id.z_axis_angularVelocity_textView) TextView zAxisAngularVelocityTextView;

    private final String LOG_TAG = com.test.movesenseapp.section_01_movesense.tests.MultiSubscribeActivity.class.getSimpleName();
    private final String LINEAR_ACCELERATION_PATH = "Meas/Acc/";
    private final String MAGNETIC_FIELD_PATH = "Meas/Magn/";
    private final String ANGULAR_VELOCITY_PATH = "Meas/Gyro/";
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    @BindView(R.id.connected_device_name_textView) TextView mConnectedDeviceNameTextView;
    @BindView(R.id.connected_device_swVersion_textView) TextView mConnectedDeviceSwVersionTextView;
    private MdsSubscription mdsSubscriptionLinearAcc;
    private MdsSubscription mdsSubscriptionMagneticField;
    private MdsSubscription mdsSubscriptionAngularVelocity;

    private CsvLogger mCsvLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_subscribe);
        ButterKnife.bind(this);

        mCsvLogger = new CsvLogger();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Multi Subscribe");
        }



        try {
            mConnectedDeviceNameTextView.setText("Serial: " + MovesenseConnectedDevices.getConnectedDevice(0)
                    .getSerial());

            mConnectedDeviceSwVersionTextView.setText("Sw version: " + MovesenseConnectedDevices.getConnectedDevice(0)
                    .getSwVersion());
        }catch(IndexOutOfBoundsException E) {
            Toast.makeText(com.test.movesenseapp.section_01_movesense.tests.MultiSubscribeActivity.this, "Please Connect a device first to use multi-connect",Toast.LENGTH_LONG).show();
            startActivity(new Intent(com.test.movesenseapp.section_01_movesense.tests.MultiSubscribeActivity.this, MovesenseActivity.class));
        }
        BleManager.INSTANCE.addBleConnectionMonitorListener(this);

        mCsvLogger.checkRuntimeWriteExternalStoragePermission(this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BleManager.INSTANCE.removeBleConnectionMonitorListener(this);

        mCsvLogger.finishSavingLogs(this, LOG_TAG);

        unsubscribe();
    }

    private void unsubscribe() {
        if (mdsSubscriptionLinearAcc != null) {
            mdsSubscriptionLinearAcc.unsubscribe();
            mdsSubscriptionLinearAcc = null;
        }

        if (mdsSubscriptionMagneticField != null) {
            mdsSubscriptionMagneticField.unsubscribe();
            mdsSubscriptionMagneticField = null;
        }

        if (mdsSubscriptionAngularVelocity != null) {
            mdsSubscriptionAngularVelocity.unsubscribe();
            mdsSubscriptionAngularVelocity = null;
        }
    }

    @OnCheckedChanged(R.id.switchSubscriptionLinearAcc)
    public void onCheckedChangedLinear(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            Log.d(LOG_TAG, "+++ Subscribe LinearAcc");
            mdsSubscriptionLinearAcc = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                            .getSerial(), LINEAR_ACCELERATION_PATH + "13"), new MdsNotificationListener() {
                        @Override
                        public void onNotification(String data) {
                            Log.d(LOG_TAG, "onSuccess(): " + data);

                            LinearAcceleration linearAccelerationData = new Gson().fromJson(
                                    data, LinearAcceleration.class);

                            if (linearAccelerationData != null) {

                                LinearAcceleration.Array arrayData = linearAccelerationData.body.array[0];

                                mCsvLogger.appendHeader("Service,X,Y,Z");

                                mCsvLogger.appendLine(String.format(Locale.getDefault(),
                                        "LinearAcc,%.6f,%.6f,%.6f, ", arrayData.x, arrayData.y, arrayData.z));

                                xAxisLinearAccTextView.setText(String.format(Locale.getDefault(),
                                        "x: %.6f", arrayData.x));
                                yAxisLinearAccTextView.setText(String.format(Locale.getDefault(),
                                        "y: %.6f", arrayData.y));
                                zAxisLinearAccTextView.setText(String.format(Locale.getDefault(),
                                        "z: %.6f", arrayData.z));

                            }
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError(): ", error);
                        }
                    });
        } else {
            Log.d(LOG_TAG, "--- Unsubscribe LinearAcc");
            mdsSubscriptionLinearAcc.unsubscribe();
        }
    }


    @OnCheckedChanged(R.id.switchSubscriptionECG)
    public void onCheckedChangedMagnetic(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            Log.d(LOG_TAG, "+++ Subscribe MagneticField");
            mdsSubscriptionMagneticField = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                            .getSerial(), MAGNETIC_FIELD_PATH + "13"), new MdsNotificationListener() {
                        @Override
                        public void onNotification(String data) {
                            Log.d(LOG_TAG, "onSuccess(): " + data);

                            MagneticField magneticField = new Gson().fromJson(
                                    data, MagneticField.class);

                            if (magneticField != null) {

                                MagneticField.Array arrayData = magneticField.body.array[0];

                                mCsvLogger.appendHeader("Service,X,Y,Z");

                                mCsvLogger.appendLine(String.format(Locale.getDefault(),
                                        "MagneticField,%.6f,%.6f,%.6f, ", arrayData.x, arrayData.y, arrayData.z));

                                xAxisMagneticFieldTextView.setText(String.format(Locale.getDefault(),
                                        "x: %.6f", arrayData.x));
                                yAxisMagneticFieldTextView.setText(String.format(Locale.getDefault(),
                                        "y: %.6f", arrayData.y));
                                zAxisMagneticFieldTextView.setText(String.format(Locale.getDefault(),
                                        "z: %.6f", arrayData.z));

                            }
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError(): ", error);
                        }
                    });
        } else {
            Log.d(LOG_TAG, "--- Unsubscribe MagneticField");
            mdsSubscriptionMagneticField.unsubscribe();
        }
    }

    @OnCheckedChanged(R.id.switchSubscriptionPulseOximeter)
    public void onCheckedChangedAngularVielocity(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            Log.d(LOG_TAG, "+++ Subscribe AngularVelocity");
            mdsSubscriptionAngularVelocity = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                            .getSerial(), ANGULAR_VELOCITY_PATH + "13"), new MdsNotificationListener() {
                        @Override
                        public void onNotification(String data) {
                            Log.d(LOG_TAG, "onSuccess(): " + data);

                            AngularVelocity angularVelocity = new Gson().fromJson(
                                    data, AngularVelocity.class);

                            if (angularVelocity != null) {

                                AngularVelocity.Array arrayData = angularVelocity.body.array[0];

                                mCsvLogger.appendHeader("Service,X,Y,Z");

                                mCsvLogger.appendLine(String.format(Locale.getDefault(),
                                        "AngularVelocity,%.6f,%.6f,%.6f, ", arrayData.x, arrayData.y, arrayData.z));

                                xAxisAngularVelocityTextView.setText(String.format(Locale.getDefault(),
                                        "x: %.6f", arrayData.x));
                                yAxisAngularVelocityTextView.setText(String.format(Locale.getDefault(),
                                        "y: %.6f", arrayData.y));
//                                zAxisAngularVelocityTextView.setText(String.format(Locale.getDefault(),
//                                        "z: %.6f", arrayData.z));

                            }
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError(): ", error);
                        }
                    });
        } else {
            Log.d(LOG_TAG, "--- Unsubscribe AngularVelocity");
            mdsSubscriptionAngularVelocity.unsubscribe();
        }
    }

    @Override
    public void onDisconnect(String s) {
        Log.d(LOG_TAG, "onDisconnect: " + s);
        if (!isFinishing()) {
            runOnUiThread(() -> ConnectionLostDialog.INSTANCE.showDialog(com.test.movesenseapp.section_01_movesense.tests.MultiSubscribeActivity.this));
        }
    }

    @Override
    public void onConnect(RxBleDevice rxBleDevice) {
        Log.d(LOG_TAG, "onConnect: " + rxBleDevice.getName() + " " + rxBleDevice.getMacAddress());
        ConnectionLostDialog.INSTANCE.dismissDialog();
    }

    @Override
    public void onConnectError(String s, Throwable throwable) {

    }
}
