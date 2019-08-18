package com.sxl.study.shop.model;

public class Product {
    private String pid;
    private Integer inventoryCnt;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Integer getInventoryCnt() {
        return inventoryCnt;
    }

    public void setInventoryCnt(Integer inventoryCnt) {
        this.inventoryCnt = inventoryCnt;
    }
}
