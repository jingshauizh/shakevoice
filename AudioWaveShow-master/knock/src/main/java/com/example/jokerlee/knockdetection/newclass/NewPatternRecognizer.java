package com.example.jokerlee.knockdetection.newclass;

import android.util.Log;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NewPatternRecognizer {

	final long minWaitTime_ms = 150; // The time-window when knocks will NOT be acknowledged
	final long waitWindow_ms = 500; // The time-window when knocks WILL be acknowledged

	private ScheduledFuture<?> timerFuture = null ;
	EventGenState_t state = EventGenState_t.Wait;
	ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
	NewKnockDetector p = null;
	private int detectedKnockCount = 0;

	NewPatternRecognizer(NewKnockDetector parent){
		p = parent;
	}
	
	private enum EventGenState_t {
		Wait,
		S1,
		S2, 
		S3,
		S4,
	} 
	
	

	Runnable waitTimer = new Runnable(){
		public void run() {
			timeOutEvent();			
		}
	};

	private void startTimer(long timeToWait){
		if (timerFuture != null){
			timerFuture.cancel(false);
		}
		timerFuture = mExecutor.schedule(waitTimer, timeToWait, TimeUnit.MILLISECONDS);
	}


	public void knockEvent(){
		Log.d("media","state="+state);
		switch(state){
		case Wait:
			detectedKnockCount++;
			startTimer(minWaitTime_ms);
			state =  EventGenState_t.S1;
			break;
		case S1:
			//Do nothing, ignore knock
			break;
		case S2:
			detectedKnockCount++;
			timerFuture.cancel(false);
			startTimer(minWaitTime_ms);
			state = EventGenState_t.S3;
			break;
		case S3:
			//Do nothing, ignore knock
			break;
		case S4:
			timerFuture.cancel(false);
			p.knockDetected(++detectedKnockCount);
			detectedKnockCount = 0;
			state = EventGenState_t.Wait;
			break;
		default:
			break;
		}
	}

	
	public void timeOutEvent(){
		switch(state){
		case Wait:
			break;
		case S1:
			startTimer(waitWindow_ms);
			state = EventGenState_t.S2;
			break;
		case S2:
			detectedKnockCount = 0;
			state = EventGenState_t.Wait;
			break;
		case S3:
			startTimer(waitWindow_ms);
			state = EventGenState_t.S4;
			break;
		case S4:
			p.knockDetected(detectedKnockCount);
			detectedKnockCount = 0;
			state = EventGenState_t.Wait;
			break;
		default:
			break;
		}
	}
}