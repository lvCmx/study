package com.sxl.study.shop.reqeust;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 配置成单例的
 */
public class AtomicIndex {
    private static AtomicLong index=new AtomicLong(0);
    private static final Long MAX_INDEX_SIZE=1000000L;
    public static Long getIndex(){
        if(index.get()>MAX_INDEX_SIZE){
            index.set(index.get()%MAX_INDEX_SIZE);
        }
        return index.incrementAndGet();
    }
}
