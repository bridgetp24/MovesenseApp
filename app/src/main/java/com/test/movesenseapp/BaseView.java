package com.test.movesenseapp;


/**
 * TODO: Add a class header comment!
 */

public interface BaseView<T extends BasePresenter> {

    void setPresenter(T presenter);
}
