package com.tian.audio.wave.activity;


import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tian.audio.wave.R;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class RecordActivity extends BaseActivity {


    // 录音界面相关
    Button btnStart;
    Button btnStop;
    TextView textTime;

    // 录音功能相关
    MediaRecorder mMediaRecorder; // MediaRecorder 实例
    boolean isRecording; // 录音状态
    String fileName; // 录音文件的名称
    String filePath; // 录音文件存储路径
    Thread timeThread; // 记录录音时长的线程
    int timeCount; // 录音时长 计数
    final int TIME_COUNT = 0x101;
    // 录音文件存放目录
     String audioSaveDir ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        textTime = (TextView) findViewById(R.id.text_time);
        audioSaveDir = getApplicationContext().getExternalCacheDir() + "/audiodemo/";

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开始录音
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);

                startRecord();
                isRecording = true;
                // 初始化录音时长记录
                timeThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        countTime();
                    }
                });
                timeThread.start();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 停止录音
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);

                stopRecord();
                isRecording = false;
            }
        });

    }

    // 记录录音时长
    private void countTime() {
        while (isRecording) {
            Log.d("record", "正在录音");
            timeCount++;
            Message msg = Message.obtain();
            msg.what = TIME_COUNT;
            msg.obj = timeCount;
            myHandler.sendMessage(msg);
            try {
                timeThread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("record", "结束录音");
        timeCount = 0;
        Message msg = Message.obtain();
        msg.what = TIME_COUNT;
        msg.obj = timeCount;
        myHandler.sendMessage(msg);
    }


    /**
     * 开始录音 使用amr格式
     * 录音文件
     *
     * @return
     */
    public void startRecord() {
        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        try {
            /* ②setAudioSource/setVedioSource */
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            fileName = DateFormat.format("yyyyMMdd_HHmmss", Calendar.getInstance(Locale.CHINA)) + ".m4a";
            Log.d("record", "audioSaveDir="+audioSaveDir);
            if (isFolderExists(audioSaveDir)) {
                filePath = audioSaveDir + fileName;
                /* ③准备 */
                mMediaRecorder.setOutputFile(filePath);
                mMediaRecorder.prepare();
                /* ④开始 */
                mMediaRecorder.start();
            }

        } catch (IllegalStateException e) {
            Log.i("record", "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        } catch (IOException e) {
            Log.i("record", "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
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

    /**
     * 停止录音
     */
    public void stopRecord() {
        //有一些网友反应在5.0以上在调用stop的时候会报错，翻阅了一下谷歌文档发现上面确实写的有可能会报错的情况，捕获异常清理一下就行了，感谢大家反馈！
        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            filePath = "";
            Toast.makeText(getApplicationContext(),"录音文件存放路径"+audioSaveDir,Toast.LENGTH_LONG).show();

        } catch (RuntimeException e) {
            Log.e("record", e.toString());
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            File file = new File(filePath);
            if (file.exists())
                file.delete();

            filePath = "";
        }
    }

    // 格式化 录音时长为 时:分:秒
    public static String FormatMiss(int miss) {
        String hh = miss / 3600 > 9 ? miss / 3600 + "" : "0" + miss / 3600;
        String mm = (miss % 3600) / 60 > 9 ? (miss % 3600) / 60 + "" : "0" + (miss % 3600) / 60;
        String ss = (miss % 3600) % 60 > 9 ? (miss % 3600) % 60 + "" : "0" + (miss % 3600) % 60;
        return hh + ":" + mm + ":" + ss;
    }


    Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TIME_COUNT:
                    int count = (int) msg.obj;
                    Log.d("record", "count == " + count);
                    textTime.setText(FormatMiss(count));

                    break;
            }
        }
    };
}