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
import com.example.jokerlee.knockdetection.utils.Complex;
import com.example.jokerlee.knockdetection.utils.FFT;
import com.example.jokerlee.knockdetection.utils.NewFFT;

import org.jtransforms.fft.DoubleFFT_1D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
     * <p>
     * https://stackoverflow.com/questions/9272232/fft-library-in-android-sdk
     * <p>
     * https://stackoverflow.com/questions/7651633/using-fft-in-android
     * <p>
     * https://introcs.cs.princeton.edu/java/97data/FFT.java.html
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
    private byte[] backmBufferNext;
    private byte[] findBuffer;
    private String fftResult = "";
    //buffer值不能太大，避免OOM
    private static final int BUFFER_SIZE = 4096;
    private static final int FFT_CHECK_Times = 5;
    private boolean mIsPlaying = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private AudioManager mAudioManager = null;
    private Context mContext;
    private StringBuilder resultFFTBuilder;

    private double[] finderDoubles;

    public AudioSoundKnockDetector(Context context) {
        mContext = context;
        mExecutorService = Executors.newSingleThreadExecutor();
        mBuffer = new byte[BUFFER_SIZE];
        backmBuffer = new byte[BUFFER_SIZE];
        backmBufferNext = new byte[BUFFER_SIZE];
        findBuffer = new byte[BUFFER_SIZE];
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
            Log.d("v9", "startRecorderTime=" + startRecorderTime);
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
            float result = 1;
            int count = 0;
            int findcount = 0;
            boolean setMaxFlag = false;
            int findMaxIndex = 0;
            while (mIsRecording && (second < 1000)) {
                //只要还在录音就一直读取
                int read = mAudioRecord.read(mBuffer, 0, BUFFER_SIZE);
                if (read <= 0) {
                    return;
                } else {
                    mFileOutputStream.write(mBuffer, 0, read);
                }

                currentRecorderTime = System.currentTimeMillis();
                second = currentRecorderTime - startRecorderTime;


                float[] pcmAsFloats = AudioUtil.floatMeNew(mBuffer);
                // 将 buffer 找出最大值
                float v = 0;
                int max_index = 0;
                //Log.i("float", "temp= 1111111111111111111");
                for (int i = 0; i < pcmAsFloats.length; i++) {
                    //double temp = Math.abs(complex[i].phase());
                    float temp = Math.abs(pcmAsFloats[i]);
                    //Log.i("float", "temp=" + temp);
                    if (temp > v) {
                        v = temp;
                        max_index = i;
                    }
                }

                float max = v;
                if (setMaxFlag) {
                    setMaxFlag = false;
                    System.arraycopy(mBuffer, 0, backmBufferNext, 0, mBuffer.length);
                    resultFFTBuilder.append("\n find result cache nex=");
                }

                resultFFTBuilder.append("\n find max=" + max + "  max_index=" + max_index + "  count=" + count);
                if (max > result) {
                    result = max;
                    findcount = count;
                    findMaxIndex = max_index;
                    setMaxFlag = true;
                    System.arraycopy(mBuffer, 0, backmBuffer, 0, mBuffer.length);
                }

                count++;

            }
            resultFFTBuilder.append("\n find result=" + result);
            resultFFTBuilder.append("\n find count=" + findcount + "  total count" + count);

            //向最大值前面偏移50个点
            if (findMaxIndex > 50) {
                findMaxIndex = findMaxIndex - 50;
            } else {
                findMaxIndex = 0;
            }

            findBuffer = AudioUtil.cutBufferFromIndex(backmBuffer, backmBufferNext, findMaxIndex);

            //2byte  转成一个double
            double[] pcmAsDoubles = AudioUtil.doubleMeNew(findBuffer);
            finderDoubles = pcmAsDoubles;
            Log.i("audio", "pcmAsDoubles=" + pcmAsDoubles.length);


            //double 转成 Complex 数组
            Complex[] findComplex = AudioUtil.doubleToComplex(pcmAsDoubles);
            Log.i("audio", "findComplex=" + findComplex.length);

            // Complex 数组 做FFT
            Complex[] fftfindComplex = NewFFT.fft(findComplex);
            Log.i("audio", "fftfindComplex=" + fftfindComplex.length);

            fftResult = ayncDoubleDatas(pcmAsDoubles);
            // 对FFT结果进行分析
            fftResult += ayncFFTComplexDatas(fftfindComplex, path);

            StringBuilder fftstrBuid = new StringBuilder();
            fftstrBuid.append("FFT 1111111111111111=============\n");
            for (int i = 0; i < fftfindComplex.length; i++) {
                fftstrBuid.append(fftfindComplex[i].toString() + "\n");
            }

            StringBuilder strBuid = new StringBuilder();


            for (int i = 0; i < pcmAsDoubles.length; i++) {
                //String resultFFTData = "FFTaudiodata=" + audioDataDoubles[i] + "   pcm mBuffer=" + backmBuffer[i] + " no= " + i;
                String resultFFTData = pcmAsDoubles[i] + ",";
                strBuid.append(resultFFTData);
                //addsub += Math.abs(audioDataDoubles[i]);//数据取了绝对值
            }

            String tempFile1 = path.replace(".pcm", "_byte_find.pcm");
            File tempFileFile1 = new File(tempFile1);
            FileOutputStream tempmFileOutputStream1 = new FileOutputStream(tempFileFile1);
            tempmFileOutputStream1.write(findBuffer);

            String tempFile = path.replace(".pcm", "_double_find.pcm");
            File tempFileFile = new File(tempFile);
            FileOutputStream tempmFileOutputStream = new FileOutputStream(tempFileFile);
            tempmFileOutputStream.write(strBuid.toString().getBytes());

            String tempFile2 = path.replace(".pcm", "_find_fft.pcm");
            File tempFileFile2 = new File(tempFile2);
            FileOutputStream tempmFileOutputStream2 = new FileOutputStream(tempFileFile2);
            tempmFileOutputStream2.write(fftstrBuid.toString().getBytes());


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

    private String ayncDoubleDatas(double[] doubleDatas) {
        StringBuilder doubleBuid = new StringBuilder();
        double valueSub256 = 0;
        double valueSubAll = 0;
        double valueSub1024 = 0;
        for (int i = 0; i < 256; i++) {
            double temp = Math.abs(doubleDatas[i])*Math.abs(doubleDatas[i]);
            valueSub256 += temp;
        }

        for (int j = 0; j < doubleDatas.length; j++) {
            double temp = Math.abs(doubleDatas[j])*Math.abs(doubleDatas[j]);
            valueSubAll += temp;
        }

        for (int k = 1024; k < doubleDatas.length; k++) {
            double temp = Math.abs(doubleDatas[k])*Math.abs(doubleDatas[k]);
            valueSub1024 += temp;
        }

        doubleBuid.append("  aync  DoubleDatas  valueSub256=" + valueSub256 + "\n");
        doubleBuid.append("  aync  DoubleDatas  valueSubAll=" + valueSubAll + "\n");
        doubleBuid.append("  aync  DoubleDatas  valueSub1024=" + valueSub1024 + "\n");

        double sub256toall = valueSub256/valueSubAll;
        double sub256to1024 = valueSub256/valueSub1024;

        doubleBuid.append("  aync  DoubleDatas  sub256toall=" + sub256toall + "\n");
        doubleBuid.append("  aync  DoubleDatas  sub256to1024=" + sub256to1024 + "\n");
        return doubleBuid.toString();
    }
    /**
     * 对FFT结果进行分析
     *
     * @param fftfindComplex
     * @param path
     * @return
     */
    private String ayncFFTComplexDatas(Complex[] fftfindComplex, String path) {
        //记录分析数据 在页面展示
        StringBuilder fftstrBuid = new StringBuilder();
        //求 FFT 的平均值
        double maxvalue = 0;
        double subValue = 0;
        //找到所有结果的最大值
        for (int i = 0; i < fftfindComplex.length; i++) {
            double temp = fftfindComplex[i].abs();
            subValue += temp;
            if(temp > maxvalue){
                maxvalue = temp;
            }
        }
        fftstrBuid.append("FFT ayncFFT ComplexDatas  checked data=============\n");
        double avg = subValue/fftfindComplex.length;
        fftstrBuid.append("FFT ayncFFT ComplexDatas  avg=" + avg + "\n");

        fftstrBuid.append("FFT ayncFFT ComplexDatas  maxvalue=" + maxvalue + "\n");

        //最大值的一半 做一个检测标准
        double checkValue = maxvalue/2;
        fftstrBuid.append("FFT ayncFFT ComplexDatas  checkValue=" + checkValue + "\n");

        //大于平均值 的 5倍 数据记录下来
        for (int i = 0; i < fftfindComplex.length; i++) {
            double temp = fftfindComplex[i].abs();//数据取了绝对值
            if (temp > checkValue) {
                fftstrBuid.append("index=" + i + "  abs=" + temp + "\n");
            }
        }
        Log.i("audio", "fftstrBuid=" + fftstrBuid.toString());

        //记录数据写文件
        String tempFile3 = path.replace(".pcm", "_sync_fft.pcm");
        try {
            File tempFileFile2 = new File(tempFile3);
            FileOutputStream tempmFileOutputStream2 = new FileOutputStream(tempFileFile2);
            tempmFileOutputStream2.write(fftstrBuid.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fftstrBuid.toString();

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

    public double [] getFindeDoubles() {
        return finderDoubles;
    }


    public byte[] getBackmBuffer() {
        return backmBuffer;
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
