package com.sxl.study.shop.reqeust;

public interface Request {
    // 查库
    void process();
    // 获取商品id
    String getProductId();
    // 是否强制刷新缓存
    boolean isForceRefresh();
}
