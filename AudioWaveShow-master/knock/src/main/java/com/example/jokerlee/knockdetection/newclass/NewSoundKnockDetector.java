package com.example.jokerlee.knockdetection.newclass;

import android.app.Application;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class NewSoundKnockDetector {
    /**
     * Triggers a volume event if the sound detected is the maximum volume possible on the device
     * <p>
     * 如果检测到的声音是设备上可能的最大音量，则触发音量事件
     */

    //VOLUM STUFF
    private static final String TAG = "VolRec";
    private MediaRecorder mRecorder = null;
    private Timer mTimer = null;
    private TimerTask volListener = null;
    public volatile boolean spikeDetected = false;

    //VOLUM STUFF END

    //Starts sensor measurements
    public void startVolKnockListener() {

        mTimer = new Timer();

        volListener = new TimerTask() {

            int MAX_VAL = 32767;
            int THRESHOLD = 16000;

            @Override
            public void run() {
                int amp = getAmplitude();
                if (amp > THRESHOLD) {
                    spikeDetected = true;
                }
            }
        };
        mTimer.scheduleAtFixedRate(volListener, 0, 20); //start after 0 ms
    }

    //Stops sensor measurements
    public void stopVolKnockListener() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    public void vol_start(String path) {

        startVolKnockListener();

        if (mRecorder == null) {
            mRecorder = new MediaRecorder();

            int audioMax = mRecorder.getAudioSourceMax();

            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //MIC

            //mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER); //MIC

//            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); //THREE_GPP);
//            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); // .AMR_NB);


            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            try {
                //TODO handle so that the audio file doesn't overflow memory
                if (isFolderExists(path)) {
                    String fileName = DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.CHINA)) + ".m4a";
                    String voicefile = path + "/" + fileName;
                    Log.d("media", "vol_start run voicefile=" + voicefile);
                    mRecorder.setOutputFile(voicefile);
                }

            } catch (IllegalStateException e) {
                e.printStackTrace();

            }

            try {
                mRecorder.prepare();
                mRecorder.start();
                Log.d("media2", "mRecorder.start()" );
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    boolean isFolderExists(String strFolder) {
        File file = new File(strFolder);

        if (!file.exists()) {
            if (file.mkdir()) {
                return true;
            } else
                return false;
        }
        return true;
    }

    public void vol_stop() {
        Log.d("media2", "vol_stop mRecorder.stop()" );
        stopVolKnockListener();
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        clearTempFile();
    }

    //clear the temp recorder file.
    private void clearTempFile() {
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/both");
        if (file.exists()) {
            file.delete();
        }
    }

    private int getAmplitude() {
        if (mRecorder != null)
            return mRecorder.getMaxAmplitude(); ///2700.0;
        else
            return 0;
    }
}
