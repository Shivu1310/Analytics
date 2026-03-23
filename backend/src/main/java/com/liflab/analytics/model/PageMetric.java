package com.liflab.analytics.model;

public class PageMetric {
    private String page;
    private long count;

    public PageMetric(String page, long count) {
        this.page = page;
        this.count = count;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
