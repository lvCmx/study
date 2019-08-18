package com.sxl.study.shop;

import com.sxl.study.shop.listener.InitThreadPoolListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ShopApplication {

    public static void main(String[] args) {
        System.out.println("项目启动中");
        SpringApplication.run(ShopApplication.class, args);
        System.out.println("项目启动后");
    }

    /**
     * 加载Listener
     * @return
     */
    @Bean
    public ServletListenerRegistrationBean servletListenerRegistrationBean() {
        ServletListenerRegistrationBean servletListenerRegistrationBean =
                new ServletListenerRegistrationBean();
        servletListenerRegistrationBean.setListener(new InitThreadPoolListener());
        return servletListenerRegistrationBean;
    }
}
