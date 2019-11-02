package com.sxl.study.util.security;

import org.junit.Test;

import java.io.File;

public class MD5UtilTest {

    @Test
    public void testMd5(){
        String testStr="tom";
        System.out.println(MD5Util.encode(testStr)); //34b7da764b21d298ef307d04d8152dc5
    }

    @Test
    public void testMd5File(){
        String md5 = MD5Util.encodeFile(new File("C:\\Users\\Administrator\\Desktop\\abc.txt"));
        System.out.println(md5); // b44989e04a9a7f538a196e499d710534
    }
}
