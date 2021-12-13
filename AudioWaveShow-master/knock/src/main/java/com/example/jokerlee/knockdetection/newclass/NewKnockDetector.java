package com.example.jokerlee.knockdetection.newclass;

import android.content.Context;
import android.hardware.SensorManager;
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
	private NewSoundKnockDetector mSoundKnockDetector = new NewSoundKnockDetector();
	private NewPatternRecognizer mPatt = new NewPatternRecognizer(this);

	public abstract void knockDetected(int knockCount);

	private enum EventGenState_t {
		NoneSet,
		VolumSet,
		AccelSet
	}

	public NewKnockDetector(Context context){
		mContext = context;
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
		mSoundKnockDetector.vol_start(path);
		mAccelSpikeDetector.resumeAccSensing();
	}
	
	private void startEventDetectTimer(){

		mTimer = new Timer();

		eventGen = new TimerTask(){

			int nrTicks = 0;

			EventGenState_t state = EventGenState_t.NoneSet;

			@Override
			public void run() {

				switch(state){
				//None of the bools set
				case NoneSet:
					if		( mSoundKnockDetector.spikeDetected && !mAccelSpikeDetector.spikeDetected) state = EventGenState_t.VolumSet;
					else if	(!mSoundKnockDetector.spikeDetected &&  mAccelSpikeDetector.spikeDetected) state = EventGenState_t.AccelSet; 
					else if	( mSoundKnockDetector.spikeDetected &&  mAccelSpikeDetector.spikeDetected){

						mSoundKnockDetector.spikeDetected = false;
						mAccelSpikeDetector.spikeDetected = false;
						state =  EventGenState_t.NoneSet;
						//generate knock event
						mPatt.knockEvent();
					}
					Log.d("media","NoneSet run state="+state);
					nrTicks = 0;
					break;
					//volum set
				case VolumSet:
					if(mAccelSpikeDetector.spikeDetected){
						mSoundKnockDetector.spikeDetected = false;
						mAccelSpikeDetector.spikeDetected = false;
						state =  EventGenState_t.NoneSet;
						//generate knock event
						mPatt.knockEvent();
						break;
					}else{
						nrTicks+=1;
						if(nrTicks > period){
							nrTicks = 0;
							mSoundKnockDetector.spikeDetected = false;
							state = EventGenState_t.NoneSet;
						}
					}
					Log.d("media","VolumSet run state="+state);
					break;

					//accsel set
				case AccelSet:
					//if(mSoundKnockDetector.spikeDetected){
					if(true){
						mSoundKnockDetector.spikeDetected = false;
						mAccelSpikeDetector.spikeDetected = false;
						state =  EventGenState_t.NoneSet;
						//generate knock event
						mPatt.knockEvent();
						break;
					}else{
						nrTicks+=1;
						if(nrTicks > period){
							nrTicks = 0;
							mAccelSpikeDetector.spikeDetected = false;
							state = EventGenState_t.NoneSet;
						}
					}
					Log.d("media","AccelSet run state="+state);
					break;
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
