package com.test.movesenseapp.section_01_movesense.tests;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.MdsSubscription;
import com.movesense.mds.internal.connectivity.BleManager;
import com.movesense.mds.internal.connectivity.MovesenseConnectedDevices;
import com.polidea.rxandroidble2.RxBleDevice;
import com.test.movesenseapp.BaseActivity;
import com.test.movesenseapp.R;
import com.test.movesenseapp.bluetooth.ConnectionLostDialog;
import com.test.movesenseapp.bluetooth.MdsRx;
import com.test.movesenseapp.csv.CsvLogger;
import com.test.movesenseapp.model.InfoResponse;
import com.test.movesenseapp.model.LinearAcceleration;
import com.test.movesenseapp.utils.FormatHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnItemSelected;

public class PulseOximeterActivity extends BaseActivity implements BleManager.IBleConnectionMonitor {

    private final String LOG_TAG = "PODebug";
    private final String LINEAR_ACCELERATION_PATH = "Meas/Acc/";
    private final String LINEAR_INFO_PATH = "/Meas/Acc/Info";
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";

    //Spinner value lists
    private final List<String> spinnerRates = new ArrayList<>();
    private final List<String> ranges = new ArrayList<>();
    private final List<String> LEDModes = new ArrayList<>();
    private final List<String> pulseWidths = new ArrayList<>();
    private final List<String> brightnessLevels = new ArrayList<>();
    private final List<String> avgReadings = new ArrayList<>();

    //selected settings
    private String rate;
    private String LEDMode;
    private String pulseWidth;
    private String brightness;
    private String avgCount;

    private MdsSubscription mdsSubscription;

    @BindView(R.id.switchSubscription)
    SwitchCompat switchSubscription;
    @BindView(R.id.spinner) Spinner spinner;
    @BindView(R.id.LEDModeSpinner) Spinner LEDModeSpinner;
    @BindView(R.id.pulseWidthSpinner) Spinner pulseWidthSpinner ;
    @BindView(R.id.brightnessSpinner) Spinner brightnessSpinner ;
    @BindView(R.id.avgCountSpinner) Spinner avgCountSpinner ;


    @BindView(R.id.x_axis_textView) TextView xAxisTextView;
    @BindView(R.id.y_axis_textView) TextView yAxisTextView;
    @BindView(R.id.connected_device_name_textView) TextView mConnectedDeviceNameTextView;
    @BindView(R.id.connected_device_swVersion_textView) TextView mConnectedDeviceSwVersionTextView;
    @BindView(R.id.linearAcc_lineChart) LineChart mChart;
    private AlertDialog alertDialog;
    private CsvLogger mCsvLogger;
    private boolean isLogSaved = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_oximeter);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Oxygen Saturation");
        }

        mCsvLogger = new CsvLogger();

        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.please_wait)
                .setMessage(R.string.loading_information)
                .create();

        mConnectedDeviceNameTextView.setText("Serial: " + MovesenseConnectedDevices.getConnectedDevice(0)
                .getSerial());

        mConnectedDeviceSwVersionTextView.setText("Sw version: " + MovesenseConnectedDevices.getConnectedDevice(0)
                .getSwVersion());

        xAxisTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        yAxisTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));


        // Init Empty Chart
        mChart.setData(new LineData());
        mChart.getDescription().setText("Oxygen Levels");
        mChart.setTouchEnabled(false);
        mChart.setAutoScaleMinMaxEnabled(true);
        mChart.invalidate();


        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, spinnerRates);

        final ArrayAdapter<String> ADCRangesAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, ranges);

        final ArrayAdapter<String> LEDModeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, LEDModes);

        final ArrayAdapter<String> pulseWidthAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, pulseWidths);

        final ArrayAdapter<String> brightnessAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, brightnessLevels);

        final ArrayAdapter<String> avgReadingsAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, avgReadings);


        spinner.setAdapter(spinnerAdapter);
        LEDModeSpinner.setAdapter(LEDModeAdapter);
        pulseWidthSpinner.setAdapter(pulseWidthAdapter);
        brightnessSpinner.setAdapter(brightnessAdapter);
        avgCountSpinner.setAdapter(avgReadingsAdapter);

        // Display dialog
        alertDialog.show();

        Mds.builder().build(this).get(MdsRx.SCHEME_PREFIX
                        + MovesenseConnectedDevices.getConnectedDevice(0).getSerial() + LINEAR_INFO_PATH,
                null, new MdsResponseListener() {
                    @Override
                    public void onSuccess(String data) {
                        Log.d(LOG_TAG, "onSuccess(): " + data);

                        // Hide dialog
                        alertDialog.dismiss();

                        InfoResponse.Content POContent = new InfoResponse.Content();
                        InfoResponse infoResponse = new InfoResponse(POContent);

                        if (infoResponse != null) {


                            for (Integer inforate : infoResponse.content.sampleRates) {
                                Log.d(LOG_TAG, "Assign sampleRate "+ inforate);
                                spinnerRates.add(String.valueOf(inforate));

                                // Set first rate as default
                                if (rate == null) {
                                    rate = String.valueOf(inforate);
                                }
                            }


                            for (String mode : infoResponse.content.LEDMode){
                                LEDModes.add(String.valueOf(mode));

                                // Set first rate as default
                                if (LEDMode == null) {
                                    LEDMode = String.valueOf(mode);
                                }

                            }


                            for (Integer width : infoResponse.content.pulseWidth){
                                pulseWidths.add(String.valueOf(width));

                                // Set first rate as default
                                if (pulseWidth == null) {
                                    pulseWidth = String.valueOf(width);
                                }
                            }

                            for (String brightness : infoResponse.content.brightness){
                                brightnessLevels.add(String.valueOf(brightness));

                                // Set first rate as default
                                if (brightness == null) {
                                    brightness = String.valueOf(brightness);
                                }
                            }

                            for (Integer avgReading : infoResponse.content.avgReading){
                                avgReadings.add(String.valueOf(avgReading));

                                // Set first rate as default
                                if (avgCount == null) {
                                    avgCount = String.valueOf(avgReading);
                                }
                            }


                            spinnerAdapter.notifyDataSetChanged();
                            LEDModeAdapter.notifyDataSetChanged();
                            pulseWidthAdapter.notifyDataSetChanged();
                            brightnessAdapter.notifyDataSetChanged();
                            avgReadingsAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(MdsException error) {
                        Log.e(LOG_TAG, "onError(): ", error);

                        // Hide dialog
                        alertDialog.dismiss();
                    }
                });

        BleManager.INSTANCE.addBleConnectionMonitorListener(this);

    }

    @OnCheckedChanged(R.id.switchSubscription)
    public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
//
//
//        if (isChecked) {
//            disableSpinner();
//
//            isLogSaved = false;
//
//            mCsvLogger.checkRuntimeWriteExternalStoragePermission(this, this);
//
//            final LineData mLineData = mChart.getData();
//
//            ILineDataSet xSet = mLineData.getDataSetByIndex(0);
//            ILineDataSet ySet = mLineData.getDataSetByIndex(1);
//        //    ILineDataSet zSet = mLineData.getDataSetByIndex(2);
//
//            if (xSet == null) {
//                xSet = createSet("Data x", getResources().getColor(android.R.color.holo_red_dark));
//                ySet = createSet("Data y", getResources().getColor(android.R.color.holo_green_dark));
//              //  zSet = createSet("Data z", getResources().getColor(android.R.color.holo_blue_dark));
//                mLineData.addDataSet(xSet);
//                mLineData.addDataSet(ySet);
//              //  mLineData.addDataSet(zSet);
//            }
//
//            mdsSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
//                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
//                            .getSerial(), LINEAR_ACCELERATION_PATH + rate), new MdsNotificationListener() {
//                        @Override
//                        public void onNotification(String data) {
//                            Log.d(LOG_TAG, "onSuccess(): " + data);
//
//                            LinearAcceleration linearAccelerationData = new Gson().fromJson(
//                                    data, LinearAcceleration.class);
//
//                            if (linearAccelerationData != null) {
//
//                                LinearAcceleration.Array arrayData = linearAccelerationData.body.array[0];
//
//                                mCsvLogger.appendHeader("Timestamp (ms),X: (m/s^2),Y: (m/s^2)");
//
//                                mCsvLogger.appendLine(String.format(Locale.getDefault(),
//                                        "%d,%.6f,%.6f,%.6f, ", linearAccelerationData.body.timestamp,
//                                        arrayData.x, arrayData.y));
//
//                                xAxisTextView.setText(String.format(Locale.getDefault(),
//                                        "x: %.6f", arrayData.x));
//                                yAxisTextView.setText(String.format(Locale.getDefault(),
//                                        "y: %.6f", arrayData.y));
//
//
//                                Log.e(LOG_TAG, "onNotification: timestamp: " + linearAccelerationData.body.timestamp + " x: " + arrayData.x);
//                                Log.e(LOG_TAG, "onNotification: lineData.getEntryCount(): " + mLineData.getEntryCount());
//                                mLineData.addEntry(new Entry(linearAccelerationData.body.timestamp / 100, (float) arrayData.x), 0);
//                                mLineData.addEntry(new Entry(linearAccelerationData.body.timestamp / 100, (float) arrayData.y), 1);
//                                //mLineData.addEntry(new Entry(linearAccelerationData.body.timestamp / 100, (float) arrayData.z), 2);
//                                mLineData.notifyDataChanged();
//
//                                // let the chart know it's data has changed
//                                mChart.notifyDataSetChanged();
//
//                                // limit the number of visible entries
//                                mChart.setVisibleXRangeMaximum(50);
//
//                                // move to the latest entry
//                                mChart.moveViewToX(linearAccelerationData.body.timestamp / 100);
//                            }
//                        }
//
//                        @Override
//                        public void onError(MdsException error) {
//                            Log.e(LOG_TAG, "onError(): ", error);
//
//                            Toast.makeText(com.test.movesenseapp.section_01_movesense.tests.PulseOximeterActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
//                            buttonView.setChecked(false);
//                        }
//                    });
//        } else {
//            unSubscribe();
//            enableSpinner();
//        }
    }

    @OnItemSelected(R.id.spinner)
    public void onItemSelectedRate(AdapterView<?> parent, View view, int position, long id) {
        rate = spinnerRates.get(position);
    }
    @OnItemSelected(R.id.LEDModeSpinner)
    public void onItemSelectedLEDMode(AdapterView<?> parent, View view, int position, long id) {
       LEDMode = spinnerRates.get(position);
    }

    @OnItemSelected(R.id.pulseWidthSpinner)
    public void onItemSelectedPulseWidth(AdapterView<?> parent, View view, int position, long id) {
        pulseWidth = spinnerRates.get(position);
    }

    @OnItemSelected(R.id.brightnessSpinner)
    public void onItemSelectedBrightness(AdapterView<?> parent, View view, int position, long id) {
        brightness= spinnerRates.get(position);
    }

    @OnItemSelected(R.id.avgCountSpinner)
    public void onItemSelectedAvgCount(AdapterView<?> parent, View view, int position, long id) {
        avgCount = spinnerRates.get(position);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unSubscribe();

        BleManager.INSTANCE.removeBleConnectionMonitorListener(this);
    }

    private void unSubscribe() {
        if (mdsSubscription != null) {
            mdsSubscription.unsubscribe();
            mdsSubscription = null;
        }

        if (!isLogSaved) {
            mCsvLogger.finishSavingLogs(this, "PulseOximeter");
            isLogSaved = true;
        }
    }

    private void disableSpinner() {
        spinner.setEnabled(false);
        LEDModeSpinner.setEnabled(false);
        pulseWidthSpinner.setEnabled(false);
        brightnessSpinner.setEnabled(false);
        avgCountSpinner.setEnabled(false);
    }

    private void enableSpinner() {

        spinner.setEnabled(true);
        LEDModeSpinner.setEnabled(true);
        pulseWidthSpinner.setEnabled(true);
        brightnessSpinner.setEnabled(true);
        avgCountSpinner.setEnabled(true);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == LogsManager.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
//            // if request is cancelled grantResults array is empty
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                        == PackageManager.PERMISSION_GRANTED) {
//                }
//            }
//        }
    }

    @Override
    public void onDisconnect(String s) {
        Log.d(LOG_TAG, "onDisconnect: " + s);
        if (!isFinishing()) {
            runOnUiThread(() -> ConnectionLostDialog.INSTANCE.showDialog(com.test.movesenseapp.section_01_movesense.tests.PulseOximeterActivity.this));
        }
    }

    @Override
    public void onConnect(RxBleDevice rxBleDevice) {
        Log.d(LOG_TAG, "onConnect: " + rxBleDevice.getName() + " " + rxBleDevice.getMacAddress());
        ConnectionLostDialog.INSTANCE.dismissDialog();
        mChart.getData().clearValues();
    }

    @Override
    public void onConnectError(String s, Throwable throwable) {

    }

    private LineDataSet createSet(String name, int color) {
        LineDataSet set = new LineDataSet(null, name);
        set.setLineWidth(2.5f);
        set.setColor(color);
        set.setDrawCircleHole(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(0f);

        return set;
    }

}
