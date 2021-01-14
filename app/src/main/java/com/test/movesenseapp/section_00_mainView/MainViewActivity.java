package com.test.movesenseapp.section_00_mainView;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.test.movesenseapp.BuildConfig;
import com.test.movesenseapp.R;
import com.test.movesenseapp.section_01_movesense.MovesenseActivity;
import com.test.movesenseapp.google_drive.SendLogsToGoogleDriveActivity;
import com.test.movesenseapp.section_01_movesense.tests.MultiSubscribeActivity;
import com.test.movesenseapp.section_02_multi_connection.connection.MultiConnectionActivity;
//import com.test.showcaserebuild.section_02_multi_connection.connection.MultiConnectionActivity;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainViewActivity extends AppCompatActivity {

    private final String TAG = "mainMenuDebug";

    @BindView(R.id.mainView_movesense_Ll) RelativeLayout mMainViewMovesenseLl;
    @BindView(R.id.mainView_multiConnection_Ll) RelativeLayout mMainViewMultiConnectionLl;
   // @BindView(R.id.mainView_dfu_Ll) RelativeLayout mMainViewDfuLl;
    @BindView(R.id.mainView_savedData_Ll) RelativeLayout mMainViewSavedDataLl;
    @BindView(R.id.mainView_appVersion_tv) TextView mMainViewAppVersionTv;
    @BindView(R.id.mainView_libraryVersion_tv) TextView mMainViewLibraryVersionTv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);
        ButterKnife.bind(this);

       String versionName = BuildConfig.VERSION_NAME;
        //hardcoded because access through BuildConfig gave error

        String libraryVersion = "1.44.0";

        mMainViewAppVersionTv.setText(getString(R.string.application_version, versionName));
        //mMainViewAppVersionTv.setText(getString(R.string.application_version, versionName));
        // mMainViewLibraryVersionTv.setText(getString(R.string.library_version, libraryVersion));
        mMainViewLibraryVersionTv.setText(getString(R.string.library_version, libraryVersion));

    }

    @OnClick({R.id.mainView_movesense_Ll, R.id.mainView_multiConnection_Ll, R.id.mainView_savedData_Ll})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.mainView_movesense_Ll:
                Log.d(TAG, "Starting connection intent");
                startActivity(new Intent(com.test.movesenseapp.section_00_mainView.MainViewActivity.this, MovesenseActivity.class));

                break;
            case R.id.mainView_multiConnection_Ll:
                Log.d(TAG, "Starting multi connection intent");
                startActivity(new Intent(com.test.movesenseapp.section_00_mainView.MainViewActivity.this, MultiConnectionActivity.class));
                break;

            case R.id.mainView_savedData_Ll:
                Log.d(TAG, "Starting google drive intent");
                startActivity(new Intent(com.test.movesenseapp.section_00_mainView.MainViewActivity.this, SendLogsToGoogleDriveActivity.class));
                break;
        }
    }
}
