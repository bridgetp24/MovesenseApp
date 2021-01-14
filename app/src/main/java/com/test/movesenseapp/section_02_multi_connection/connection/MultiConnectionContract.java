package com.test.movesenseapp.section_02_multi_connection.connection;

import com.polidea.rxandroidble2.RxBleDevice;
import com.test.movesenseapp.BasePresenter;
import com.test.movesenseapp.BaseView;

public interface MultiConnectionContract {

    interface Presenter extends BasePresenter {
        void scanFirstDevice();
        void scanSecondDevice();
        void connect(RxBleDevice rxBleDevice);
        void disconnect(RxBleDevice rxBleDevice);

    }

    interface View extends BaseView<Presenter> {
        void onFirsDeviceSelectedResult(RxBleDevice rxBleDevice);
        void onSecondDeviceSelectedResult(RxBleDevice rxBleDevice);
        void displayErrorMessage(String message);
    }
}
