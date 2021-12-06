
package com.tian.audio.wave.activity;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;

import com.tian.audio.wave.R;
import com.tian.audio.wave.utils.DemoPermissionUtil;


/**
 * Demo to show how to use VisualizerView
 */
public abstract class BaseActivity extends Activity {

    private static String[] REQUEST_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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