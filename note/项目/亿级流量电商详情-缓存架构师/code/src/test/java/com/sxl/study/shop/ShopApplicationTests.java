package com.sxl.study.shop;

import com.sxl.study.shop.reqeust.ProductIndex;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ConcurrentLinkedQueue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ShopApplicationTests {

    @Test
    public void contextLoads() {
        String key="111";
        int h;
        int hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
        System.out.println( key.hashCode());
        System.out.println( key.hashCode() >>> 16);
        System.out.println( hash);


    }

}
