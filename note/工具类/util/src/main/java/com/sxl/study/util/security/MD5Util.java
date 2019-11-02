package com.sxl.study.util.security;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {

    private static final String HEX_DIGTIS [] = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};

    /**
     * 直接对 origin 进行md5
     * @param origin
     * @return
     */
    public static String encode(String origin){
        return encodeSlat(origin,null,null);
    }

    /**
     * 直接对 origin 进行md5
     * @param origin
     * @param charset 字符编码
     * @return
     */
    public static String encode(String origin,String charset){
        return encodeSlat(origin,null,charset);
    }

    public static String encodeSlat(String origin,String slat){
        return encodeSlat(origin,slat,null);
    }

    public static String encodeSlat(String origin,String slat,String charset){
        String param = origin;
        if(null != slat){
            param =  origin + slat;
        }
        if(null == charset){
            charset = "UTF8";
        }
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] digestArr = digest.digest(param.getBytes(charset));
            result = byteArrToHexStr(digestArr);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String byteArrToHexStr(byte b[]){
        StringBuffer resultSb = new StringBuffer();
        for(int i = 0; i < b.length; i++){
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b){
        int num = b;
        if(num < 0){
            num += 256;
        }
        int digit1 = num / 16;
        int digit2 = num % 16;
        return HEX_DIGTIS[digit1] + HEX_DIGTIS[digit2];
    }

    /**
     * 对文件生成摘要
     * @param file
     * @return
     */
    public static String encodeFile(File file){
        FileInputStream in = null;
        FileChannel channel = null;
        MappedByteBuffer byteBuffer = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            channel = in.getChannel();
            byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            digest.update(byteBuffer);

            return byteArrToHexStr(digest.digest());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != byteBuffer){
                byteBuffer = null;
            }
            if(null != channel){
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(null != in){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
