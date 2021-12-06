package com.example.jokerlee.knockdetection;

import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class SoundKnockDetector {
	/**
	 * Triggers a volume event if the sound detected is the maximum volume possible on the device
	 */

	//VOLUM STUFF
	private static final String TAG = "VolRec";
	private MediaRecorder mRecorder = null;
	private Timer mTimer = null;
	private TimerTask volListener = null;
	public volatile boolean spikeDetected = false;
	
	//VOLUM STUFF END

	//Starts sensor measurements
	public void startVolKnockListener(){

		mTimer = new Timer();

		volListener = new TimerTask(){

			int MAX_VAL = 32767;
			int THRESHOLD = 16000;		

			@Override
			public void run() {
				int amp = getAmplitude();
				if(amp>THRESHOLD){
					spikeDetected = true;
				}
			}
		};
		mTimer.scheduleAtFixedRate(volListener, 0, 20); //start after 0 ms
	}

	//Stops sensor measurements
	public void stopVolKnockListener(){
		if( mTimer != null ){
			mTimer.cancel();
			mTimer.purge();
			mTimer = null;
		}
	}

	public void vol_start() {

		startVolKnockListener();

		if (mRecorder == null) {
			mRecorder = new MediaRecorder();

			int audioMax = mRecorder.getAudioSourceMax ();

			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //MIC

			//mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER); //MIC

			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); //THREE_GPP);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); // .AMR_NB);

			//TODO handle so that the audio file doesn't overflow memory
			mRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath()+"/both"); 

			try {
				mRecorder.prepare();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mRecorder.start();
		}
	}

	public void vol_stop() {
		stopVolKnockListener();
		if (mRecorder != null) {
			mRecorder.stop();       
			mRecorder.release();
			mRecorder = null;
		}
		clearTempFile();
	}

	//clear the temp recorder file.
	private void clearTempFile(){
		File file = new File(Environment.getExternalStorageDirectory().getPath()+"/both");
		if(file.exists()){
			file.delete();
		}
	}

	private int getAmplitude() {
		if (mRecorder != null)
			return  mRecorder.getMaxAmplitude(); ///2700.0;
		else
			return 0;
	}
}
