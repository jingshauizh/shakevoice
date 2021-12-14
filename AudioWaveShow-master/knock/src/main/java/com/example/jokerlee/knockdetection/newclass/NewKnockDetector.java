package com.example.jokerlee.knockdetection.newclass;

import android.content.Context;
import android.hardware.SensorManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;


abstract public class NewKnockDetector {

	/**
	 * Makes sure that accelerometer event and sound event only triggers a knock event
	 * if and only if they happen at the same time or very close together in time
	 *
	 * *确保加速计事件和声音事件仅触发爆震事件
	 * *当且仅当它们同时发生或在时间上非常接近时
	 *
	 *
	 */

	private Context mContext;
	private TimerTask eventGen = null;
	private Timer mTimer = null;
	private final int MaxTimeBetweenEvents = 25;
	private int period = MaxTimeBetweenEvents;

	private NewAccelSpikeDetector mAccelSpikeDetector;
	private AudioSoundKnockDetector mSoundKnockDetector ;
	private NewPatternRecognizer mPatt = new NewPatternRecognizer(this);

	public abstract void knockDetected(int knockCount);

	private String filePath = "";

	private enum EventGenState_t {
		NoneSet,
		VolumSet,
		AccelSet
	}

	public NewKnockDetector(Context context){
		mContext = context;
		mSoundKnockDetector = new AudioSoundKnockDetector(context);
		mAccelSpikeDetector = new NewAccelSpikeDetector(
				(SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE));
	}

	//Stop detecting.
	public void stopDetecting(){
		if( mTimer == null ) return;
		stopDetectTimer(); // stop the timer task first.
		mSoundKnockDetector.vol_stop();
		mAccelSpikeDetector.stopAccSensing();
	}

	public boolean isDetecting(){
		return mTimer != null;
	}

	//Start detecting.
	public void startDetecting(String path){
		if( mTimer != null ) return;
		startEventDetectTimer(); // start the timer task first.
		//mSoundKnockDetector.vol_start_public(path);
		filePath = path;
		mAccelSpikeDetector.resumeAccSensing();
	}
	
	private void startEventDetectTimer(){

		mTimer = new Timer();

		eventGen = new TimerTask(){

			@Override
			public void run() {
				//监测到重力加速有效 则进行 声音录制
				if (mAccelSpikeDetector.spikeDetected) {
					if (!TextUtils.isEmpty(filePath)) {
						mSoundKnockDetector.vol_start_public(filePath);
						mAccelSpikeDetector.spikeDetected = false;
					}

				}

			}
		};
		mTimer.scheduleAtFixedRate(eventGen, 0, period); //start after 0 ms
	}

	private void stopDetectTimer(){
		if( mTimer != null ){
			mTimer.cancel();
			mTimer.purge();
			mTimer = null;
		}
	}

}
