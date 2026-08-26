package com.liquid.org;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Launcher shell. The UI is loaded by the Xposed ClassLoader path. */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finishAndRemoveTask();
    }
}
