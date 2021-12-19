package com.example.jokerlee.knockdetection.utils;

import android.util.Log;

import java.nio.ByteBuffer;

public class AudioUtil {


    /**
     * buffer 数据进行平方求和
     * @param buffer
     * @return
     *
     * https://blog.csdn.net/weixin_36300275/article/details/114147628
     *
     *
     * https://blog.csdn.net/weixin_29944865/article/details/114440581
     *
     * java byte转double_Java 中字节数组和基本类型之间的相互转换
     *
     *
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


    public static Complex[] audioToComplex(byte[] buffer) {
        int complexLenght = buffer.length / 2;
        Complex[] returnComplex = new Complex[complexLenght];
        for (int i = 0; i < complexLenght; i++) {
            byte re = buffer[i * 2];
            byte im = buffer[(i * 2) + 1];
            Complex complexTemp = new Complex(re, im);
            returnComplex[i] = complexTemp;
        }
        return returnComplex;
    }


    public static long calcBufferComplexSize(byte[] buffer) {
        Complex[] complex = audioToComplex(buffer);
        // 将 buffer 内容取出，进行平方和运算
        long v = 0;
        for (int i = 0; i < complex.length; i++) {
            v += Math.abs(complex[i].phase()) * Math.abs(complex[i].phase());
            //v += complex[i].abs() * complex[i].abs();
        }
        return v;
    }

    public static double calcMaxComplex(byte[] buffer) {
        Complex[] complex = audioToComplex(buffer);
        // 将 buffer 找出最大值
        double v = 0;
        for (int i = 0; i < complex.length; i++) {
            //double temp = Math.abs(complex[i].phase());
            double temp = complex[i].abs();
            if(temp > v){
                v = temp;
            }
        }
        return v;
    }



//    //将16位pcm数据转换成8位有符号的pcm
//
//    byte[] readBuffer = new byte[4096];
//    byte[] sendBuffer = new byte[readBuffer.length / 2];
//                for (int i = 0; i<readBuffer.length; i += 2) {
//        if ((readBuffer[i + 1] & 0x80) == 0x80) {
//            sendBuffer[i / 2] = (byte) (readBuffer[i + 1] & 0x7f);
//        } else {
//            sendBuffer[i / 2] = (byte) (readBuffer[i + 1] + 0x80);
//        }
//
//    }
//
//
//
//    //将8位有符号的的pcm数据转换成16位
//
//    byte[] readBuffer = new byte[4096];
//    int audioDataLen = readBuffer.length * 2;
//    byte[] audioBuffer = new byte[readBuffer.length * 2];
//
//                for (int i = 0; i<readBuffer.length; i++) {
//                    /*if (readBuffer[i] == 63 && i != 0 && i != readBuffer.length - 1) {
//                        readBuffer[i] = (byte) ((readBuffer[i + 1] + readBuffer[i - 1]) / 2);
//                    }*/
//        if ((readBuffer[i] & 0x80) == 0x80) {
//            audioBuffer[2 * i] = 0x00;
//            audioBuffer[2 * i + 1] = (byte) (readBuffer[i] - 0x80);
//        } else {
//            audioBuffer[2 * i] = (byte) 0xff;
//            audioBuffer[2 * i + 1] = (byte) (readBuffer[i] - 0x80);
//        }
//    }
//————————————————
//    版权声明：本文为CSDN博主「ross」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
//    原文链接：https://blog.csdn.net/fycghy0803/article/details/107051985



//    //将16位pcm数据转换成8位有符号的pcm
//
//    byte[] readBuffer = new byte[4096];
//
//    byte[] sendBuffer = new byte[readBuffer.length / 2];
//
//for (int i = 0; iif ((readBuffer[i + 1] & 0x80) == 0x80) {
//
//        sendBuffer[i / 2] = (byte) (readBuffer[i + 1] & 0x7f);
//
//    } else {
//
//        sendBuffer[i / 2] = (byte) (readBuffer[i + 1] + 0x80);
//
//    }
//
//}
//
////将8位有符号的的pcm数据转换成16位
//    public static byte[]  buffer8to16(byte[] readBuffer) {
//        //byte[] readBuffer = new byte[4096];
//
//        int audioDataLen = readBuffer.length * 2;
//
//        byte[] audioBuffer = new byte[readBuffer.length * 2];
//
//        for (int i = 0; i/*if (readBuffer[i] == 63 && i != 0 && i != readBuffer.length - 1) {
//
//readBuffer[i] = (byte) ((readBuffer[i + 1] + readBuffer[i - 1]) / 2);
//
//}*/
//
//        if ((readBuffer[i] & 0x80) == 0x80) {
//
//            audioBuffer[2 * i] = 0x00;
//
//            audioBuffer[2 * i + 1] = (byte) (readBuffer[i] - 0x80);
//
//        } else {
//
//            audioBuffer[2 * i] = (byte) 0xff;
//
//            audioBuffer[2 * i + 1] = (byte) (readBuffer[i] - 0x80);
//
//        }
//
//    }
////        ————————————————
////        版权声明：本文为CSDN博主「蛋疼得有理」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
////        原文链接：https://blog.csdn.net/weixin_36300275/article/details/114147628
//    }
//

    public static short[] shortMe(byte[] bytes) {
        short[] out = new short[bytes.length / 2]; // will drop last byte if odd number
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        return out;
    }

    public static float[] floatMe(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    //float[] pcmAsFloats = floatMe(shortMe(bytes));

    public static float calcMaxFloat(byte[] buffer) {
        float[] pcmAsFloats = floatMe(shortMe(buffer));
        // 将 buffer 找出最大值
        float v = 0;
        //Log.i("float", "temp= 1111111111111111111");
        for (int i = 0; i < pcmAsFloats.length; i++) {
            //double temp = Math.abs(complex[i].phase());
            float temp = Math.abs(pcmAsFloats[i]);
            //Log.i("float", "temp=" + temp);
            if(temp > v){
                v = temp;
            }
        }
        return v;
    }


    public static float getFloat(byte[] b) {
        int accum = 0;
        accum = accum|(b[1] & 0xff) << 0;
        accum = accum|(b[0] & 0xff) << 8;
        return accum;
    }
    /**
     * 字节转换为浮点
     *
     * @param b 字节（至少4个字节）
     * @param index 开始位置
     * @return
     */
    public static float byte2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        return l;
    }

    public static float[] floatMeNew(byte[] bytes) {
        int length = bytes.length / 2;
        float[] floaters = new float[length];
        byte[] tempByte = new byte[2];
        for (int i = 0; i < length; i++) {
            tempByte[0] = bytes[2*i];
            tempByte[1] = bytes[2*i+1];
            float temp = byte2float(tempByte,0);
            floaters[i] = temp;
        }
        return floaters;
    }


}
