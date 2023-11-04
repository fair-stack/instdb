package cn.cnic.instdb.model.findata;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ZJ
 * @description
 * @Create by 2022/10/12 13:00
 */
@Data
public class FileInfoFromInstdb implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * fileID
     */
    private String id;

    /**
     * file name
     */
    private String name;

    /**
     * file type
     */
    private String type;

    /**
     * file size
     */
    private Long size;

    /**
     * Official download address for files
     */
    private String url;

    /**
     * URLIs the address valid (0-Is the address valid；1-Is the address valid)
     */
    private String urlEffective;
}
