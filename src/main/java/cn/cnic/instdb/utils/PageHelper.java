package cn.cnic.instdb.utils;

import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/27/16:34
 * @Description:
 */

@Data
public class PageHelper<T> {

    private long currentPage;
    private long total;
    private long pageSize;
    private List<T> list;
    private T data;

    public PageHelper(long pageNum, long total, long pageSize, List<T> list,T data) {
        this.currentPage = pageNum;
        this.total = total;
        this.pageSize = pageSize;
        this.list = list;
        this.data = data;
    }

    public PageHelper(long pageNum, long total, long pageSize, List<T> list) {
        this.currentPage = pageNum;
        this.total = total;
        this.pageSize = pageSize;
        this.list = list;
    }

    //Special Purpose wdd
    public PageHelper(long total, List<T> list, long pageSize) {
        this.total = total;
        this.pageSize = pageSize;
        this.list = list;
    }

    public PageHelper(long pageNum, long pageSize, List<T> list) {
        this.currentPage = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

}
