
package com.example.jokerlee.knockdetection.base;

import android.Manifest;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import com.example.jokerlee.knockdetection.utils.DemoPermissionUtil;


/**
 * Demo to show how to use VisualizerView
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static String[] REQUEST_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoPermissionUtil.requestMultiPermissions(this, REQUEST_PERMISSIONS,
                1122, mPermissionGrant);
    }

    private DemoPermissionUtil.PermissionGrant mPermissionGrant =
            new DemoPermissionUtil.PermissionGrant() {

                @Override
                public void onPermissionGranted(int requestCode) {


                }
            };

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }


}