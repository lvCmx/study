package com.sxl.study.shop.reqeust;

public class ProductIndex {
    private String pid;
    private Long index;
    private boolean flag;

    public ProductIndex(Builder builder){
        this.pid=builder.pid;
        this.index=builder.index;
        this.flag=builder.flag;
    }

    public static class Builder {
        private String pid;
        private Long index;
        private boolean flag;

        public Builder setPid(String pid) {
            this.pid = pid;
            return this;
        }

        public Builder setIndex(Long index) {
            this.index = index;
            return this;
        }

        public Builder setFlag(boolean flag) {
            this.flag = flag;
            return this;
        }

        public ProductIndex build() {
            return new ProductIndex(this);
        }
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Long getIndex() {
        return index;
    }

    public void setIndex(Long index) {
        this.index = index;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
