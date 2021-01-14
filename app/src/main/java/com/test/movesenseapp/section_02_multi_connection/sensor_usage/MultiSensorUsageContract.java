package com.test.movesenseapp.section_02_multi_connection.sensor_usage;


import com.test.movesenseapp.BasePresenter;
import com.test.movesenseapp.BaseView;

import io.reactivex.Observable;

public interface MultiSensorUsageContract {

    interface Presenter extends BasePresenter {
        Observable<String> subscribeLinearAcc(String uri);

    }

    interface View extends BaseView<Presenter> {

    }
}
