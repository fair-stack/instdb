package cn.cnic.instdb.result;

import java.io.Serializable;
import java.util.List;

/**
 * Table Paging Data Object
 * 
 * @author ruoyi
 */
public class TableDataInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** Total Records */
    private long total;

    /** List data */
    private List<?> data;

    /** Message status code */
    private int code;

    /** Message content */
    private String message;

    /**
     * Table Data Object
     */
    public TableDataInfo()
    {
    }

    /**
     * paging
     * 
     * @param list List data
     * @param total Total Records
     */
    public TableDataInfo(List<?> list, int total)
    {
        this.data = list;
        this.total = total;
    }

    public long getTotal()
    {
        return total;
    }

    public void setTotal(long total)
    {
        this.total = total;
    }

    public List<?> getData() {
        return data;
    }

    public void setData(List<?> data) {
        this.data = data;
    }

    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String msg)
    {
        this.message = message;
    }
}
