package com.sxl.study.shop.listener;

import com.sxl.study.shop.thread.RequestProcessorThreadPool;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

@Component
public class InitThreadPoolListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        RequestProcessorThreadPool.getInstance();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
