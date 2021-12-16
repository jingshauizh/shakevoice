package com.example.jokerlee.knockdetection.newclass;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Detects spikes in accelerometer data (only in z axis) and generates accelerometer events
 * the volatile spikeDetected boolean will be set when a spike is detected
 *
 * *检测加速计数据中的峰值（仅在z轴上），并生成加速计事件
 * *检测到峰值时，将设置volatile spikeDetected布尔值
 *
 * @author tor
 *
 */
public class NewAccelSpikeDetector implements SensorEventListener{

	public volatile boolean spikeDetected = false;
	private SensorManager mSensorManager;

	//Optimization parameters accelerometer
	final public float thresholdZ = 3; //Force needed to trigger event, G = 9.81 methinks
	final public float threshholdX = 5;
	final public float threshholdY = 5;
	final public int updateFrequency = 50;


	//For high pass filter
	private float prevZVal = 0;
	private float currentZVal = 0;
	private float diffZ = 0;

	private float prevXVal = 0;
	private float currentXVal = 0;
	private float diffX = 0;

	private float prevYVal = 0;
	private float currentYVal = 0;
	private float diffY = 0;


	private long lastUpdate = 0;
	private float last_x, last_y, last_z;
	private static final int SHAKE_THRESHOLD = 200;
	private int bb = 0;

	private SensorManager senSensorManager;
	private Sensor senAccelerometer;
	NewAccelSpikeDetector(SensorManager sm){
		mSensorManager = sm;
	}

	public void stopAccSensing(){
		mSensorManager.unregisterListener(this);
	}

	public void resumeAccSensing(){
		//mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 1000000/updateFrequency);
		senAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void onSensorChanged(SensorEvent event) {
//		prevXVal = currentXVal;
//		currentXVal = abs(event.values[0]); // X-axis
//		diffX = currentXVal - prevXVal;
//
//		prevYVal = currentYVal;
//		currentYVal = abs(event.values[1]); // Y-axis
//		diffY = currentYVal - prevYVal;
//
//		prevZVal = currentZVal;
//		currentZVal = abs(event.values[2]); // Z-axis
//		diffZ = currentZVal - prevZVal;
//
//		//Z force must be above some limit, the other forces below some limit to filter out shaking motions
//		if (currentZVal > prevZVal && diffZ > thresholdZ && diffX < threshholdX && diffY < threshholdY){
//			accTapEvent();
//		}

		Sensor mySensor = event.sensor;
		if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];

			long curTime = System.currentTimeMillis();

			if ((curTime - lastUpdate) > 100) {
				long diffTime = (curTime - lastUpdate);
				lastUpdate = curTime;
				float speed = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;
				if (speed > SHAKE_THRESHOLD) {
					Log.d("v9","curTime="+curTime);
					accTapEvent();
				}

				last_x = x;
				last_y = y;
				last_z = z;
			}
		}

	}

	private void accTapEvent(){
		spikeDetected = true;
		stopAccSensing();
	}

	private float abs(float f) {
		if (f<0){
			return -f;
		}
		return f;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}



}
