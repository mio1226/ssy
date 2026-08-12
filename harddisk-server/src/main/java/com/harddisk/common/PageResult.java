package com.harddisk.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long page;
    private long pageSize;
    private List<T> list;

    public static <T> PageResult<T> of(long total, long page, long pageSize, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.total = total;
        r.page = page;
        r.pageSize = pageSize;
        r.list = list;
        return r;
    }
}
