package com.test.movesenseapp.section_01_movesense.sensors.sensors_list;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.movesense.mds.internal.connectivity.BleManager;
import com.movesense.mds.internal.connectivity.MovesenseConnectedDevices;
import com.test.movesenseapp.BaseActivity;
import com.test.movesenseapp.R;
import com.test.movesenseapp.bluetooth.MdsRx;
import com.test.movesenseapp.model.MdsConnectedDevice;
import com.test.movesenseapp.section_00_mainView.MainViewActivity;
import com.test.movesenseapp.section_01_movesense.device_settings.DeviceSettingsActivity;
//import com.test.movesenseapp.section_01_movesense.tests.AngularVelocityActivity;
//import com.test.movesenseapp.section_01_movesense.tests.AppInfoActivity;
//import com.test.movesenseapp.section_01_movesense.tests.BatteryActivity;
//import com.test.movesenseapp.section_01_movesense.tests.EcgActivityGraphView;
//import com.test.movesenseapp.section_01_movesense.tests.HeartRateTestActivity;
//import com.test.movesenseapp.section_01_movesense.tests.ImuActivity;
//import com.test.movesenseapp.section_01_movesense.tests.LedTestActivity;
//import com.test.movesenseapp.section_01_movesense.tests.LinearAccelerationTestActivity;
//import com.test.movesenseapp.section_01_movesense.tests.MagneticFieldTestActivity;
//import com.test.movesenseapp.section_01_movesense.tests.MemoryDiagnosticActivity;
//import com.test.movesenseapp.section_01_movesense.tests.MultiSubscribeActivity;
//import com.test.movesenseapp.section_01_movesense.tests.TemperatureTestActivity;
import com.test.movesenseapp.section_01_movesense.tests.BatteryActivity;
import com.test.movesenseapp.section_01_movesense.tests.EcgActivityGraphView;
import com.test.movesenseapp.section_01_movesense.tests.LinearAccelerationTestActivity;
import com.test.movesenseapp.section_01_movesense.tests.MultiSubscribeActivity;
import com.test.movesenseapp.section_01_movesense.tests.PulseOximeterActivity;
import com.test.movesenseapp.utils.ThrowableToastingAction;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class SensorListActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.sensorList_recyclerView) RecyclerView mSensorListRecyclerView;
    @BindView(R.id.sensorList_deviceInfo_title_tv) TextView mSensorListDeviceInfoTitleTv;
    @BindView(R.id.sensorList_deviceInfo_serial_tv) TextView mSensorListDeviceInfoSerialTv;
    @BindView(R.id.sensorList_deviceInfo_sw_tv) TextView mSensorListDeviceInfoSwTv;

    private CompositeDisposable subscriptions;

    private final String TAG = SensorListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_list);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sensors List");
        }

        subscriptions = new CompositeDisposable();

        ArrayList<SensorListItemModel> sensorListItemModels = new ArrayList<>();

        sensorListItemModels.add(new SensorListItemModel(getString(R.string.linear_acceleration_name)));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.ecg)));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.pulse_oximeter)));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.multi_subscription_name)));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.settings_name)));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.battery_energy)));


        SensorsListAdapter sensorsListAdapter = new SensorsListAdapter(sensorListItemModels, this);
        mSensorListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSensorListRecyclerView.setAdapter(sensorsListAdapter);

        sensorsListAdapter.notifyDataSetChanged();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_device_settings:
                startActivity(new Intent(SensorListActivity.this, DeviceSettingsActivity.class));
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorListDeviceInfoSerialTv.setText("Serial: " + MovesenseConnectedDevices.getConnectedDevice(0).getSerial());
        mSensorListDeviceInfoSwTv.setText("Sw version: " + MovesenseConnectedDevices.getConnectedDevice(0).getSwVersion());

        subscriptions.add(MdsRx.Instance.connectedDeviceObservable()
                .subscribe(new Consumer<MdsConnectedDevice>() {
                    @Override
                    public void accept(MdsConnectedDevice mdsConnectedDevice) {
                        if (mdsConnectedDevice.getConnection() == null) {
                            Log.d(TAG, "Disconnected");

                            if (MovesenseConnectedDevices.getConnectedDevices().size() == 1) {
                                MovesenseConnectedDevices.getConnectedDevices().remove(0);
                            } else {
                                Log.e(TAG, "ERROR: Wrong MovesenseConnectedDevices list size");
                            }

                            startActivity(new Intent(SensorListActivity.this, MainViewActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

                        }
                    }
                }, new ThrowableToastingAction(this)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        subscriptions.clear();
    }

    @Override
    public void onClick(View v) {
        String sensorName = (String) v.getTag();

        subscriptions.clear();

        if (getString(R.string.pulse_oximeter).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, PulseOximeterActivity.class));
            return;
        } else if (getString(R.string.linear_acceleration_name).equals(sensorName)) {
           startActivity(new Intent(SensorListActivity.this, LinearAccelerationTestActivity.class));
            return;
        } else if (getString(R.string.multi_subscription_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, MultiSubscribeActivity.class));
            return;
        } else if (getString(R.string.ecg).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, EcgActivityGraphView.class));
            return;
        } else if (getString(R.string.battery_energy).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, BatteryActivity.class));
            return;
        } else if (getString(R.string.settings_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, DeviceSettingsActivity.class));
            return;
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit)
                .setMessage(R.string.disconnect_dialog_text)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Disconnecting...");

                        BleManager.INSTANCE.disconnect(MovesenseConnectedDevices.getConnectedRxDevice(0));
                    }
                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }
}
