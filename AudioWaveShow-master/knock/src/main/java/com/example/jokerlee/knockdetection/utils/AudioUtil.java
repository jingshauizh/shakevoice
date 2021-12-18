package com.example.jokerlee.knockdetection.utils;

public class AudioUtil {


    /**
     * buffer 数据进行平方求和
     * @param buffer
     * @return
     *
     * https://blog.csdn.net/weixin_36300275/article/details/114147628
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

}
