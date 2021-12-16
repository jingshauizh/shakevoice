package com.example.jokerlee.knockdetection;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jokerlee.knockdetection.base.BaseActivity;
import com.example.jokerlee.knockdetection.newclass.NewKnockDetector;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainAudioRecordActivity extends BaseActivity {
    private Button bt_stream_recorder;
    private TextView tv_stream_msg;
    private TextView tv_fft_msg;
    private ExecutorService mExecutorService;
    private long startRecorderTime, stopRecorderTime;
    private volatile boolean mIsRecording = true;
    private AudioRecord mAudioRecord;
    private FileOutputStream mFileOutputStream;
    private File mAudioRecordFile;
    private byte[] mBuffer;
    private byte[] backmBuffer;
    //buffer值不能太大，避免OOM
    private static final int BUFFER_SIZE = 4096;
    private boolean mIsPlaying = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private AudioManager mAudioManager = null;


    private NewKnockDetector mKnockDetector;

    private String audioFilePath = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_audio_record);
        setTitle("字节流录音");
        initView();
        mExecutorService = Executors.newSingleThreadExecutor();
        mBuffer = new byte[BUFFER_SIZE];
        backmBuffer = new byte[BUFFER_SIZE];
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mKnockDetector = new NewKnockDetector(this){
            @Override
            public void knockDetected(int knockCount) {
                switch (knockCount){
                    default:
                        Toast.makeText(MainAudioRecordActivity.this,"default",Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void knockResult(String result) {

            }
        };
//
    }

    private void initView() {
        bt_stream_recorder = (Button) findViewById(R.id.bt_stream_recorder);
        tv_stream_msg = (TextView) findViewById(R.id.tv_stream_msg);
        tv_fft_msg = (TextView) findViewById(R.id.media_record);

    }

    public void toMediaRecord(View view) {
        startActivity(new Intent(this, MainAudioRecordActivity.class));
    }

    public void recorderaudio(View view) {
        if (mIsRecording) {

            bt_stream_recorder.setText("停止录音");
            //在开始录音中如果这个值没有变false，则一直进行，当再次点击变false时，录音才停止
            mIsRecording = false;
            audioFilePath = MainAudioRecordActivity.this.getExternalFilesDir("")+"/audio";
            audioFilePath = audioFilePath + "/recorderdemo/" + System.currentTimeMillis() + ".pcm";
            mKnockDetector.startDetecting(audioFilePath);

        } else {

            bt_stream_recorder.setText("开始录音");
            //提交后台任务，执行录音逻辑
            mIsRecording = true;
            mKnockDetector.stopDetecting();

        }



    }







    /**
     * 录制失败
     * @return
     */
    private boolean recorderFail() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                bt_stream_recorder.setText("开始录音");
                tv_stream_msg.setText("录取失败，请重新录入");

                mIsRecording = false;
                Log.i("Tag8", "go here111111111");
            }
        });

        return false;
    }

    private void realeseRecorder() {
        mAudioRecord.release();
    }



    /**
     * 播放声音
     *
     * @param view
     */
    public void player(View view) {
        Log.d("audio","player audioFilePath="+audioFilePath);
        Log.d("audio","player mIsPlaying="+mIsPlaying);
        File file = new File(audioFilePath);
        if(file.exists()){
            Toast.makeText(MainAudioRecordActivity.this,"audioFilePath="+audioFilePath,Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(MainAudioRecordActivity.this,"录制失败 文件不存在",Toast.LENGTH_LONG).show();
        }

//        String fftResult = mKnockDetector.getFftResult();
//        tv_stream_msg.setText(fftResult);
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!mIsPlaying) {
                    Log.i("Tag8", "go here");
                    Log.d("audio","player audioFilePath="+audioFilePath);
                    mAudioRecordFile = new File(audioFilePath);
                    if(mAudioRecordFile.exists()){
                        mIsPlaying = true;
                        doPlay(mAudioRecordFile);
                    }

                }

            }
        });
    }

    public void show(View view) {

        String fftResult = mKnockDetector.getFftResult();
        tv_stream_msg.setText(fftResult);

    }


    public void showResult(View view) {

        String fftResult = mKnockDetector.getResultFFTBuilder();
        if(!TextUtils.isEmpty(fftResult)){
            tv_fft_msg.setVisibility(View.VISIBLE);
            tv_fft_msg.setText(fftResult);
        }
        else{
            tv_fft_msg.setVisibility(View.GONE);
            tv_fft_msg.setText("");
        }


    }

    /**
     * 播放音频文件
     *
     * @param audioFile
     */
    private void doPlay(File audioFile) {
        if (audioFile != null) {
            Log.i("Tag8", "go there");
            //配置播放器
            //音乐类型，扬声器播放
            int streamType = AudioManager.STREAM_MUSIC;
            //录音时采用的采样频率，所以播放时同样的采样频率
            int sampleRate = 10240;
            //单声道，和录音时设置的一样
            int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            //录音时使用16bit，所以播放时同样采用该方式
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            //流模式
            int mode = AudioTrack.MODE_STREAM;

            //计算最小buffer大小
            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            //构造AudioTrack  不能小于AudioTrack的最低要求，也不能小于我们每次读的大小
            AudioTrack audioTrack = new AudioTrack(streamType, sampleRate, channelConfig, audioFormat,
                    Math.max(minBufferSize, BUFFER_SIZE), mode);
            audioTrack.play();
            //从文件流读数据
            FileInputStream inputStream = null;
            try {
                //循环读数据，写到播放器去播放
                inputStream = new FileInputStream(audioFile);

                //循环读数据，写到播放器去播放
                int read;
                //只要没读完，循环播放

                while ((read = inputStream.read(mBuffer)) > 0) {
                    Log.i("Tag8", "read:" + read);
                    int ret = audioTrack.write(mBuffer, 0, read);
                    //检查write的返回值，处理错误

                    switch (ret) {
                        case AudioTrack.ERROR_INVALID_OPERATION:
                        case AudioTrack.ERROR_BAD_VALUE:
                        case AudioManager.ERROR_DEAD_OBJECT:
                            playFail();
                            return;
                        default:
                            break;
                    }

                }
                //播放结束
                audioTrack.stop();
            } catch (Exception e) {
                e.printStackTrace();
                //读取失败
                playFail();
            } finally {
                mIsPlaying = false;
                //关闭文件输入流
                if (inputStream != null) {
                    closeStream(inputStream);
                }
                //播放器释放
                resetQuietly(audioTrack);
            }

            //循环读数据，写到播放器去播放


            //错误处理，防止闪退

        }
    }

    /**
     * 关闭输入流
     *
     * @param inputStream
     */
    private void closeStream(FileInputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetQuietly(AudioTrack audioTrack) {
        try {
            audioTrack.stop();
            audioTrack.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 播放失败
     */
    private void playFail() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                tv_stream_msg.setText("播放失败");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExecutorService != null) {
            mExecutorService.shutdownNow();
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }
}