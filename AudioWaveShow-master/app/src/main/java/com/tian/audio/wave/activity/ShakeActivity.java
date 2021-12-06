package com.tian.audio.wave.activity;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.tian.audio.wave.R;
import com.tian.audio.wave.dialog.DemoAppUpgradeDialog;


/**
 *
 * 1 判断 手机敲击
 * 2 录制声音
 * 3 声音文件分析
 * https://blog.csdn.net/dahaohan/article/details/52883743
 *
 */
public class ShakeActivity extends Activity implements SensorEventListener {

    private DemoAppUpgradeDialog appUpgradeDialog;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;
    private int bb = 0;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shake);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);



    }


    /**
     * 摇一摇传感器处理
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    bb++;
                    if (bb%3==0){
                        Log.i("lgq","yyyyyy=--------"+bb);
                        showDialog();
                    }


                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }

    }

    /**
     * 如果显示了 就隐藏  隐藏了就显示
     */
    private void showDialog(){

        if(null == appUpgradeDialog){
            appUpgradeDialog = new DemoAppUpgradeDialog(ShakeActivity.this);
            appUpgradeDialog.setOnDialogClickListener(new DemoAppUpgradeDialog.OnDialogClickListener() {
                @Override
                public void onSureClickListener() {
                    //开始升级
                    appUpgradeDialog.dismiss();

                }

                @Override
                public void onCancelClickListener() {
                    //cancel

                    appUpgradeDialog.dismiss();
                }
            });

            appUpgradeDialog.show();
        }
        if(appUpgradeDialog.isShowing()){
            appUpgradeDialog.dismiss();
        }
        else{
            appUpgradeDialog.show();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
