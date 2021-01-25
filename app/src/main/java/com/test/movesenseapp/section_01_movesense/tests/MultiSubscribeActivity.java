package com.test.movesenseapp.section_01_movesense.tests;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.google.gson.Gson;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
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
import com.test.movesenseapp.model.EcgModel;
import com.test.movesenseapp.model.HeartRate;
import com.test.movesenseapp.model.LinearAcceleration;
import com.test.movesenseapp.model.MagneticField;
import com.test.movesenseapp.section_01_movesense.MovesenseActivity;
import com.test.movesenseapp.utils.FormatHelper;

import java.util.ArrayList;
import java.util.List;
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
    SwitchCompat switchSubscriptionECG;
    @BindView(R.id.heart_rate_textView) TextView heartRateTextView;

    @BindView(R.id.switchSubscriptionPulseOximeter)
    SwitchCompat switchSubscriptionAngularVelocity;
    @BindView(R.id.x_axis_angularVelocity_textView) TextView xAxisAngularVelocityTextView;
    @BindView(R.id.y_axis_angularVelocity_textView) TextView yAxisAngularVelocityTextView;

    private final String LOG_TAG = "MultiSubDebug";

    //ECG
    private final int MS_IN_SECOND = 1000;
    private LineGraphSeries<DataPoint> mSeriesECG;
    private int mDataPointsAppended;
    final int ecgSampleRate = 128;

    private final String LINEAR_ACCELERATION_PATH = "Meas/Acc/";
    private final String ECG_VELOCITY_PATH = "Meas/ECG/";
    private final String HEART_RATE_PATH = "Meas/Hr";

    //Not yet implemented. Using angular velocity path for the moment
    private final String PULSE_OXIMETER_PATH = "Meas/Gyro/";

    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";

    @BindView(R.id.connected_device_name_textView) TextView mConnectedDeviceNameTextView;
    @BindView(R.id.connected_device_swVersion_textView) TextView mConnectedDeviceSwVersionTextView;

    private MdsSubscription mdsSubscriptionLinearAcc;
    private MdsSubscription mdsSubscriptionAngularVelocity;

    private MdsSubscription mdsSubscriptionHr;
    private MdsSubscription mdsSubscriptionEcg;

    private CsvLogger mCsvLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_subscribe);
        ButterKnife.bind(this);

        mCsvLogger = new CsvLogger();
        mSeriesECG = new LineGraphSeries<DataPoint>();

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


        if (mdsSubscriptionAngularVelocity != null) {
            mdsSubscriptionAngularVelocity.unsubscribe();
            mdsSubscriptionAngularVelocity = null;
        }

        if(mdsSubscriptionEcg != null) {
            mdsSubscriptionEcg.unsubscribe();
            mdsSubscriptionEcg = null;
        }

        if(mdsSubscriptionHr != null) {
            mdsSubscriptionHr.unsubscribe();
            mdsSubscriptionHr = null;
        }
    }

    @OnCheckedChanged(R.id.switchSubscriptionLinearAcc)
    public void onCheckedChangedLinear(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            Log.d(LOG_TAG, "+++ Subscribe LinearAcc");

            mCsvLogger.checkRuntimeWriteExternalStoragePermission(this, this);
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
                            Toast.makeText(MultiSubscribeActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.d(LOG_TAG, "--- Unsubscribe LinearAcc");
            mdsSubscriptionLinearAcc.unsubscribe();
        }
    }
    private void updateHeartRate(CompoundButton buttonView) {
        if(buttonView.isChecked()) {
            Log.d(LOG_TAG, "+++ Subscribe HeartRate");

            mdsSubscriptionHr = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                            .getSerial(), HEART_RATE_PATH), new MdsNotificationListener() {

                        @Override
                        public void onNotification(String data) {
                            Log.d(LOG_TAG, "onSuccess(): " + data);


                            HeartRate heartRate = new Gson().fromJson(data, HeartRate.class);

                            if (heartRate != null) {


                                mCsvLogger.appendHeader("Heart Rate");

                                //update UI
                                Log.d(LOG_TAG, "Print heart rate");
                                heartRateTextView.setText(String.format(Locale.getDefault(),
                                        "Heart rate: %.0f [bpm]", (60.0 / heartRate.body.rrData[0]) * 1000));

                            }
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError(): ", error);
                            Toast.makeText(MultiSubscribeActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            buttonView.setChecked(false);
                        }
                    });
        }
    }

    public void updateECG(CompoundButton buttonView) {
        Log.d(LOG_TAG, "+++ Subscribe ECG");

        mDataPointsAppended = 0;
        mCsvLogger.checkRuntimeWriteExternalStoragePermission(this, this);
        int width = 128 * 3;

        mSeriesECG.resetData(new DataPoint[0]);

        String subscribedSampleRate = String.valueOf(ecgSampleRate);

        mdsSubscriptionHr = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
                FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                        .getSerial(), ECG_VELOCITY_PATH + subscribedSampleRate), new MdsNotificationListener() {

                    @Override
                    public void onNotification(String data) {
                        Log.d(LOG_TAG, "onSuccess(): " + data);


                        final EcgModel ecgModel = new Gson().fromJson(
                                data, EcgModel.class);

                        final int[] ecgSamples = ecgModel.getBody().getData();
                        final int sampleCount = ecgSamples.length;
                        final float sampleInterval = (float) MS_IN_SECOND / ecgSampleRate;

                        if (ecgModel.getBody() != null) {

                            for (int i = 0; i < sampleCount; i++) {
                                try {
                                    mCsvLogger.appendHeader("Timestamp,Count");

                                    if (ecgModel.mBody.timestamp != null) {
                                        mCsvLogger.appendLine(String.format(Locale.getDefault(),
                                                "%d,%s", ecgModel.mBody.timestamp + Math.round(sampleInterval * i),
                                                String.valueOf(ecgSamples[i])));
                                    } else {
                                        mCsvLogger.appendLine("," + String.valueOf(ecgSamples[i]));
                                    }

                                    mSeriesECG.appendData(
                                            new DataPoint(mDataPointsAppended, ecgSamples[i]), false,
                                            width);
                                } catch (IllegalArgumentException e) {
                                    Log.e(LOG_TAG, "GraphView error ", e);
                                }
                                mDataPointsAppended++;

                                if (mDataPointsAppended == 400) {
                                    mDataPointsAppended = 0;
                                    mSeriesECG.resetData(new DataPoint[0]);
                                }


                            }


                        }

                    }

                    @Override
                    public void onError(MdsException error) {
                        Log.e(LOG_TAG, "onError(): ", error);
                        Toast.makeText(MultiSubscribeActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }
                });
    }


    @OnCheckedChanged(R.id.switchSubscriptionECG)
    public void onCheckedChangedECG(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            updateHeartRate(buttonView);
            updateECG(buttonView);
        } else {
            Log.d(LOG_TAG, "--- Unsubscribe Heart Rate and ECG");
            if(mdsSubscriptionEcg != null) {
                mdsSubscriptionEcg.unsubscribe();
            }
            if(mdsSubscriptionHr != null) {
                mdsSubscriptionHr.unsubscribe();
            }


        }
    }

    private void updatePulseOximeter(CompoundButton buttonView) {
        if(buttonView.isChecked()) {
            Log.d(LOG_TAG, "+++ Subscribe Pulse Oximeter");

            mdsSubscriptionAngularVelocity = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                            .getSerial(), PULSE_OXIMETER_PATH), new MdsNotificationListener() {

                        @Override
                        public void onNotification(String data) {
                            Log.d(LOG_TAG, "onSuccess(): " + data);

                        //Update with PO specifications later. Use this format as an example.

//                            AngularVelocity angularVelocity = new Gson().fromJson(
//                                    data, AngularVelocity.class);

//                            if (angularVelocity != null) {
//
//                                AngularVelocity.Array arrayData = angularVelocity.body.array[0];
//
//                                mCsvLogger.appendHeader("Service,X,Y");
//
//                                mCsvLogger.appendLine(String.format(Locale.getDefault(),
//                                        "AngularVelocity,%.6f,%.6f,%.6f, ", arrayData.x, arrayData.y, arrayData.z));
//
//                                xAxisAngularVelocityTextView.setText(String.format(Locale.getDefault(),
//                                        "x: %.6f", arrayData.x));
//                                yAxisAngularVelocityTextView.setText(String.format(Locale.getDefault(),
//                                        "y: %.6f", arrayData.y));
//
//                            }
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError(): ", error);
                            Toast.makeText(MultiSubscribeActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            buttonView.setChecked(false);
                        }
                    });
        }
    }

    @OnCheckedChanged(R.id.switchSubscriptionPulseOximeter)
    public void onCheckedChangedAngularVelocity(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
          updatePulseOximeter(buttonView);
        } else {
            Log.d(LOG_TAG, "--- Unsubscribe Pulse Oximeter");

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
