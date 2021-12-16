package com.example.jokerlee.knockdetection.newclass;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.jokerlee.knockdetection.utils.AudioUtil;

import org.jtransforms.fft.DoubleFFT_1D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AudioSoundKnockDetector {
    /**
     * Triggers a volume event if the sound detected is the maximum volume possible on the device
     * <p>
     * 如果检测到的声音是设备上可能的最大音量，则触发音量事件
     * https://sites.google.com/site/piotrwendykier/software/jtransforms
     *
     * https://stackoverflow.com/questions/9272232/fft-library-in-android-sdk
     *
     * https://stackoverflow.com/questions/7651633/using-fft-in-android
     *
     */

    //VOLUM STUFF
    private static final String TAG = "VolRec";
    private MediaRecorder mRecorder = null;
    private Timer mTimer = null;
    private TimerTask volListener = null;
    public volatile boolean spikeDetected = false;

    //VOLUM STUFF END


    private ExecutorService mExecutorService;
    private long startRecorderTime, stopRecorderTime;
    private volatile boolean mIsRecording = false;
    private AudioRecord mAudioRecord;
    private FileOutputStream mFileOutputStream;
    private File mAudioRecordFile;
    private byte[] mBuffer;
    private byte[] backmBuffer;
    private String fftResult = "";
    //buffer值不能太大，避免OOM
    private static final int BUFFER_SIZE = 4096;
    private boolean mIsPlaying = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private AudioManager mAudioManager = null;
    private Context mContext;
    private StringBuilder resultFFTBuilder ;

    public AudioSoundKnockDetector(Context context) {
        mContext = context;
        mExecutorService = Executors.newSingleThreadExecutor();
        mBuffer = new byte[BUFFER_SIZE];
        backmBuffer = new byte[BUFFER_SIZE];
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

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
    public void vol_start_public(final String filepath) {

        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {

                vol_start(filepath);
            }
        });
    }

    public void vol_start(String path) {

        try {
            //记录开始录音时间
            mIsRecording = true;
            resultFFTBuilder = new StringBuilder();
            startRecorderTime = System.currentTimeMillis();
            Log.d("v9","startRecorderTime="+startRecorderTime);
            //创建录音文件
            mAudioRecordFile = new File(path);
            Log.d("media", "vol_start run voicefile=" + mAudioRecordFile.getAbsolutePath());
            if (!mAudioRecordFile.getParentFile().exists()) {
                mAudioRecordFile.getParentFile().mkdirs();
                mAudioRecordFile.createNewFile();
            }

            //创建文件输出流
            mFileOutputStream = new FileOutputStream(mAudioRecordFile);
            //配置AudioRecord
            int audioSource = MediaRecorder.AudioSource.MIC;
            //所有android系统都支持
            int sampleRate = 10240;
            //单声道输入
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            //PCM_16是所有android系统都支持的
            int autioFormat = AudioFormat.ENCODING_PCM_16BIT;
            //计算AudioRecord内部buffer最小
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, autioFormat);
            //buffer不能小于最低要求，也不能小于我们每次我们读取的大小。
            mAudioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, autioFormat, Math.max(minBufferSize, BUFFER_SIZE));


            //开始录音
            mAudioRecord.startRecording();

            //循环读取数据，写入输出流中
            long currentRecorderTime = System.currentTimeMillis();
            //大于3秒算成功，在主线程更新UI
             long second = currentRecorderTime - startRecorderTime;
            long result = 1;
            while (mIsRecording && (second < 1000)) {
                //只要还在录音就一直读取
                int read = mAudioRecord.read(mBuffer, 0, BUFFER_SIZE);
                if (read <= 0) {
                    return ;
                } else {
                    mFileOutputStream.write(mBuffer, 0, read);
                }

                currentRecorderTime = System.currentTimeMillis();
                second = currentRecorderTime - startRecorderTime;

                //找到平方求和最大的窗口 backmBuffer 就是找到的数据
                long add = AudioUtil.calcBufferSize(mBuffer,read);
                if(add > result){
                    result = add;
                    System.arraycopy(mBuffer,0,backmBuffer,0,mBuffer.length);
                }

            }



            //进行数据 FFT 处理
            //https://stackoverflow.com/questions/7651633/using-fft-in-android
            DoubleFFT_1D fft = new DoubleFFT_1D(BUFFER_SIZE-1);
            double[] audioDataDoubles = new double[BUFFER_SIZE];

            for (int j=0; j < BUFFER_SIZE; j++) { // get audio data in double[] format
                audioDataDoubles[j] = (double)backmBuffer[j];
            }

            fft.realForward(audioDataDoubles);
            //fft.realForwardFull(audioDataDoubles);
            StringBuilder strBuid = new StringBuilder();
            //求 FFT 的平均值
            double addsub = 0;
            for (int i = 0; i < BUFFER_SIZE; i++) {
                String resultFFTData = "FFTaudiodata=" + audioDataDoubles[i] + "   pcm mBuffer=" + backmBuffer[i] + " no= " + i;
                //Log.v(TAG, resultFFTData);
                strBuid.append(resultFFTData + "\n") ;
                addsub += Math.abs(audioDataDoubles[i]);
            }

            double avgFFT = addsub/BUFFER_SIZE;//平均值
            resultFFTBuilder.append("\n avgFFT="+avgFFT );
            Log.i("audio", "avgFFT="+avgFFT );
            double checkValur = avgFFT*3;//3倍平均值
            resultFFTBuilder.append("\n checkValur="+checkValur );
            double findValue = 0;
            for (int i = 0; i < BUFFER_SIZE; i++) {

                if(Math.abs(audioDataDoubles[i]) > checkValur) {
                    findValue = audioDataDoubles[i];
                    Log.i("audio", "findValue="+findValue + "  i="+i);
                    resultFFTBuilder.append("\n findValue="+findValue + "  i="+i);
                    break;
                }
            }

            fftResult = strBuid.toString();

            //退出循环，停止录音，释放资源
            Log.i("audio", "vol_start stopRecorder mIsRecording="+mIsRecording);
            Log.i("audio", "vol_start stopRecorder second="+second);
            Log.i("audio", "vol_start stopRecorder result="+result);
            stopRecorder();
            // playAudio();


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mAudioRecord != null) {
                mAudioRecord.release();
            }
        }
    }


    /**
     * 停止录音
     */
    private void stopRecorder() {
        mIsRecording = false;
        Log.i("Tag8", "stopRecorder");
        if (!doStop()) {
            recorderFail();
        }

    }

    public String getResultFFTBuilder() {
        return resultFFTBuilder.toString();
    }

    public String getFftResult() {
        return fftResult;
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
            int sampleRate = 44100;
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
                //playFail();
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


    public byte[] getBackmBuffer() {
        return backmBuffer;
    }

    public void playAudio(){
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!mIsPlaying) {
                    Log.i("Tag8", "go here");
                    mIsPlaying = true;
                    doPlay(mAudioRecordFile);
                }
            }
        });
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
                //tv_stream_msg.setText("播放失败");
                Log.i("Tag8", "播放失败");
            }
        });
    }

    /**
     * 停止录音
     *
     * @return
     */
    private boolean doStop() {
        //停止录音，关闭文件输出流
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        Log.i("Tag8", "go here");
        //记录结束时间，统计录音时长
        stopRecorderTime = System.currentTimeMillis();
        //大于3秒算成功，在主线程更新UI
        final int send = (int) (stopRecorderTime - startRecorderTime) / 1000;
        if (send > 3) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {

                    Log.i("Tag8", "go there");
                }
            });
        } else {
            recorderFail();
            return false;
        }
        return true;
    }

    /**
     * 录制失败
     *
     * @return
     */
    private boolean recorderFail() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                mIsRecording = false;
                Log.i("Tag8", "go 录取失败，请重新录入");
            }
        });

        return false;
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
        Log.d("media2", "vol_stop mRecorder.stop()");
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
