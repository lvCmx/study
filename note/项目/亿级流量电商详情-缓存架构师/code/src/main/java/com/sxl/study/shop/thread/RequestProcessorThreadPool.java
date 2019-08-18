package com.sxl.study.shop.thread;

import com.sxl.study.shop.reqeust.Request;
import com.sxl.study.shop.reqeust.RequestQueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 请求处理线程池：单例
 */
public class RequestProcessorThreadPool {

    private ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public RequestProcessorThreadPool() {
        // 为每个线程初始化一个队列
        RequestQueue requestQueue = RequestQueue.getInstance();

        for(int i = 0; i < 10; i++) {
            // 每个队列的大小
            ArrayBlockingQueue<Request> queue = new ArrayBlockingQueue<Request>(100);
            requestQueue.addQueue(queue);
            threadPool.submit(new RequestProcessorThread(queue));
        }
    }

    private static class Singleton {
        private static RequestProcessorThreadPool instance;
        static {
            instance = new RequestProcessorThreadPool();
        }
        public static RequestProcessorThreadPool getInstance() {
            return instance;
        }
    }

    public static RequestProcessorThreadPool getInstance() {
        return Singleton.getInstance();
    }

    /**
     * 初始化的便捷方法
     */
    public static void init() {
        getInstance();
    }
}
