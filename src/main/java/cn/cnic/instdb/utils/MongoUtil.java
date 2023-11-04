package cn.cnic.instdb.utils;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @Auther: wdd
 * @Date: 2021/03/27/16:33
 * @Description:
 */

@Data
@Component
public class MongoUtil<T> {
    public Integer pageSize;
    private Integer currentPage;


    public void start(Integer currentPage, Integer pageSize, Query query) {
//        pageSize = pageSize == 0 ? 10 : pageSize;
//        query.limit(pageSize);
//        //query.skip((currentPage - 1) * pageSize);
//        long size = currentPage * pageSize;
//        query.skip(size);
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        //   currentPage = currentPage == 0 ? currentPage : currentPage - 1;
        query.with(PageRequest.of(currentPage, pageSize));
    }

    public PageHelper pageHelper(long total, List<T> list) {
        return new PageHelper(this.currentPage, total, this.pageSize, list);
    }

    public PageHelper pageHelper(long total, List<T> list,T data) {
        return new PageHelper(this.currentPage, total, this.pageSize, list,data);
    }

    //Special Purpose wdd
    public PageHelper pageHelper(long total, List<T> list,long count) {
        return new PageHelper( total, list,count);
    }

    public PageHelper pageHelper(List<T> list) {
        return new PageHelper(this.currentPage, this.pageSize, list);
    }

    public PageHelper pageHelper(long currentPage, long total, long pageSize, List<T> list) {
        return new PageHelper(currentPage, total, pageSize, list);
    }

    public PageHelper pageHelper(long currentPage, long pageSize, List<T> list) {
        return new PageHelper(currentPage, pageSize, list);
    }


    /**
     * Used for fuzzy queries ignoring case
     *
     * @param string
     * @return
     */
    public Pattern getPattern(String string) {
        Pattern pattern = Pattern.compile("^.*" + string + ".*$", Pattern.CASE_INSENSITIVE);
        return pattern;
    }
}
