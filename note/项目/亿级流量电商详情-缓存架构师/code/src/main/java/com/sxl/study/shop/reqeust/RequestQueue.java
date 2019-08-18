package com.sxl.study.shop.reqeust;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RequestQueue {
    /**
     * 内存队列，为什么使用ArrayBlockingQueue，因为阻塞队列没有或者满了，都会阻塞。
     */
    private List<ArrayBlockingQueue<Request>> queues =
            new ArrayList<ArrayBlockingQueue<Request>>();
    /**
     * 标识位map，多线程情况下要使用线程安全的map
     */
    private Map<String, ConcurrentLinkedQueue<ProductIndex>> flagMap =
            new ConcurrentHashMap<String, ConcurrentLinkedQueue<ProductIndex>>();

    private static final String PRODUCT_PREFIX="FLAG_";
    private static final String PRODUCT_READ="READ_";
    private static final String PRODUCT_WRITE="WRITE_";


    /**
     * 单例有很多种方式去实现：我采取绝对线程安全的一种方式
     */
    private static class Singleton{
        private static RequestQueue instance;
        static {
            instance = new RequestQueue();
        }
        public static RequestQueue getInstance() {
            return instance;
        }
    }

    /**
     jvm的机制去保证多线程并发安全
     内部类的初始化，一定只会发生一次，不管多少个线程并发去初始化
     */
    public static RequestQueue getInstance() {
        return Singleton.getInstance();
    }

    /**
     * 添加一个内存队列
     * @param queue
     */
    public void addQueue(ArrayBlockingQueue<Request> queue) {
        this.queues.add(queue);
    }

    /**
     * 获取内存队列的数量
     * @return
     */
    public int queueSize() {
        return queues.size();
    }

    /**
     * 获取内存队列
     * @param index
     * @return
     */
    public ArrayBlockingQueue<Request> getQueue(int index) {
        return queues.get(index);
    }

    public Map<String, ConcurrentLinkedQueue<ProductIndex>> getFlagMap() {
        return flagMap;
    }

    public static String getPrefix(){
        return PRODUCT_PREFIX;
    }

    public static String getRead(){
        return PRODUCT_READ;
    }
    public static String getWrite(){
        return PRODUCT_WRITE;
    }
}
