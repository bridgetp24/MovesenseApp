package com.test.movesenseapp.section_04_saved_data;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.test.movesenseapp.R;

public class SavedDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_data);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Saved Data");
        }
    }
}
