package com.example.jokerlee.knockdetection.utils;

public class AudioUtil {


    /**
     * buffer 数据进行平方求和
     * @param buffer
     * @return
     */
    public static long calcBufferSize(byte[] buffer, int length) {
        long v = 0;
        // 将 buffer 内容取出，进行平方和运算
        int readLength = length;
        if (readLength >= buffer.length) {
            readLength = buffer.length;
        }
        for (int i = 0; i < readLength; i++) {
            v += Math.abs(buffer[i]) * Math.abs(buffer[i]);
        }
        return v;
    }
}
