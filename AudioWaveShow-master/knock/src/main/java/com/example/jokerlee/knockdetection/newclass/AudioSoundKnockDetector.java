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
    private static final int BUFFER_SIZE = 2048;
    private boolean mIsPlaying = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private AudioManager mAudioManager = null;
    private Context mContext;
    private StringBuilder resultFFTBuilder;

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

                //找到平方求和最大的窗口 backmBuffer 就是找到的数据
                //long add = AudioUtil.calcBufferComplexSize(mBuffer);
                //double max = AudioUtil.calcMaxComplex(mBuffer);

               // float max = AudioUtil.calcMaxFloat(mBuffer);

                //float[] pcmAsFloats = AudioUtil.floatMe(AudioUtil.shortMe(mBuffer));
                float[] pcmAsFloats = AudioUtil.floatMeNew(mBuffer);
                // 将 buffer 找出最大值
                float v = 0;
                int max_index = 0;
                //Log.i("float", "temp= 1111111111111111111");
                for (int i = 0; i < pcmAsFloats.length; i++) {
                    //double temp = Math.abs(complex[i].phase());
                    float temp = Math.abs(pcmAsFloats[i]);
                    //Log.i("float", "temp=" + temp);
                    if(temp > v){
                        v = temp;
                        max_index = i;
                    }
                }

                float max = v;
                if(setMaxFlag){
                    setMaxFlag = false;
                    System.arraycopy(mBuffer, 0, backmBufferNext, 0, mBuffer.length);
                    resultFFTBuilder.append("\n find result cache nex="  );
                }

                resultFFTBuilder.append("\n find max=" + max + "  max_index="+max_index +"  count="+count );
                if (max > result) {
                    result = max;
                    findcount = count;
                    findMaxIndex = max_index;
                    setMaxFlag = true;
                    System.arraycopy(mBuffer, 0, backmBuffer, 0, mBuffer.length);
                }

                count++;

            }
            resultFFTBuilder.append("\n find result=" + result );
            resultFFTBuilder.append("\n find count=" + findcount + "  total count"+count);

            //向最大值前面偏移50个点
            if(findMaxIndex > 50){
                findMaxIndex = findMaxIndex-50;
            }
            else{
                findMaxIndex = 0;
            }

            findBuffer = AudioUtil.cutBufferFromIndex(backmBuffer,backmBufferNext,findMaxIndex);

            double[] pcmAsDoubles = AudioUtil.doubleMeNew(findBuffer);

            Log.i("audio", "pcmAsDoubles=" + pcmAsDoubles.length);
//            pcmAsDoubles = new double[4];
//            pcmAsDoubles[0] = 1;
//            pcmAsDoubles[1] = 2;
//            pcmAsDoubles[2] = 3;
//            pcmAsDoubles[3] = 0;

            //Complex FFT
            Complex[] findComplex = AudioUtil.doubleToComplex(pcmAsDoubles);
            Log.i("audio", "findComplex=" + findComplex.length);
            Complex[] fftfindComplex = NewFFT.fft(findComplex);
            Log.i("audio", "fftfindComplex=" + fftfindComplex.length);
            //进行数据 FFT 处理
            //https://stackoverflow.com/questions/7651633/using-fft-in-android
//            DoubleFFT_1D fft = new DoubleFFT_1D(pcmAsDoubles.length-1);
//
//
////
//            fft.realForward(pcmAsDoubles);
            StringBuilder fftstrBuid = new StringBuilder();
            //求 FFT 的平均值
            double addsub = 0;
//            for (int i = 0; i < pcmAsDoubles.length; i++) {
//                fftstrBuid.append(pcmAsDoubles[i]+"\n");
//            }

            fftstrBuid.append("FFT 1111111111111111=============\n");

            for (int i = 0; i < fftfindComplex.length; i++) {
                addsub += Math.abs(fftfindComplex[i].phase());//数据取了绝对值
                fftstrBuid.append(fftfindComplex[i].toString()+"\n");
            }


            StringBuilder strBuid = new StringBuilder();



//            //求 FFT 的平均值
            //double addsub = 0;
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


//            double avgFFT = addsub / fftfindComplex.length;//平均值
//            resultFFTBuilder.append("\n 平均值 avgFFT=" + avgFFT);
//            Log.i("audio", "avgFFT=" + avgFFT);
//            double checkValur = avgFFT * 3;//3倍平均值
//            resultFFTBuilder.append("\n 3倍平均值 checkValur=" + checkValur);
//            double findValue = 0;
//            for (int i = 0; i < fftfindComplex.length; i++) {
//                //第一个大于3倍平均值
//                if (Math.abs(fftfindComplex[i].phase()) > checkValur) {
//                    findValue = fftfindComplex[i].phase();
//                    Log.i("audio", "找到的第一个大于3被平均值 findValue=" + findValue + "  i=" + i);
//                    resultFFTBuilder.append("\n 找到的第一个大于3被平均值 findValue=" + findValue + "  i=" + i);
//                    break;
//                }
//            }

            fftResult = strBuid.toString();

            //退出循环，停止录音，释放资源
            Log.i("audio", "vol_start stopRecorder mIsRecording=" + mIsRecording);
            Log.i("audio", "vol_start stopRecorder second=" + second);
            Log.i("audio", "vol_start stopRecorder result=" + result);
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


    public void findFrequency(double[] dData) {

        double frequency;
        int audioFrames = 2048;
        DoubleFFT_1D fft = new DoubleFFT_1D(audioFrames);

        double[] re = new double[audioFrames * 2];
        double[] im = new double[audioFrames * 2];
        double[] mag = new double[audioFrames * 2];

        /* edu/emory/mathcs/jtransforms/fft/DoubleFFT_1D.java */

        fft.complexForward(dData); // do the magic so we can find peak

        for (int i = 0; i < audioFrames; i++) {
            re[i] = dData[i * 2];
            im[i] = dData[(i * 2) + 1];
            mag[i] = Math.sqrt((re[i] * re[i]) + (im[i] * im[i]));
        }

        double peak = -1.0;
        int peakIn = -1;
        for (int i = 0; i < audioFrames; i++) {
            if (peak < mag[i]) {
                peakIn = i;
                peak = mag[i];
            }
        }

//        frequency = (sampleRate * (double)peakIn) / (double)audioFrames;
//
//        System.out.print("Peak: "+peakIn+", Frequency: "+frequency+"\n");

    }
//————————————————
//    版权声明：本文为CSDN博主「拯救大兵张嘎」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
//    原文链接：https://blog.csdn.net/weixin_31212247/article/details/114589939


    public double[] byteToDouble(byte[] byteData) {

        //byte []byteData= new byte[2048 * 2]; //two bytes per audio frame, 16 bits

        double[] dData = new double[2048 * 2]; // real & imaginary

        ByteBuffer buf = ByteBuffer.wrap(byteData);

        buf.order(ByteOrder.BIG_ENDIAN);

        int i = 0;

        while (buf.remaining() > 1) {

            short s = buf.getShort();

            dData[2 * i] = (double) s / 32768.0; //real

            dData[2 * i + 1] = 0.0; // imag

            ++i;

        }
        return dData;

    }
//————————————————
//    版权声明：本文为CSDN博主「拯救大兵张嘎」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
//    原文链接：https://blog.csdn.net/weixin_31212247/article/details/114589939


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



    /**
     * 播放音频文件
     *
     * @param
     */
    private void doDataTrans() {
        String path = "";
        File audioFile = new File(path);
        if (audioFile != null&audioFile.exists()) {
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
        else{
            Log.i("Tag8", "文件不存在");
        }
    }



    public byte[] getBackmBuffer() {
        return backmBuffer;
    }

    public void playAudio() {
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
