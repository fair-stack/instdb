package cn.cnic.instdb.model.findata;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ZJ
 * @description InclusionInstDbInclusion
 * @Create by 2022/10/12 12:07
 */
//@Validated
@Data
public class DatasetFromInstdb implements Serializable {

    private String dataSetId; // data setid

    private String title;// Dataset Title

    private String introduction;// Dataset Introduction

    private Date publishDate;  //Publication time

    private String keyword;// keyword

    private List<String> author;// Dataset author

    private String from; //Institutions that push data

    private String institutions; //mechanism

    private String status;


    private List<String> textUrl;

    /**
     * Dataset Source Websiteurl
     */
    private String simpleSource;

    /**
     * data set-data set
     */
    private String source;

    private String doi;

    private String dataSetType; //Dataset Type Dataset Type Dataset Type”“，

    private String version; //version version，version1

    private String code;    //Dataset organization identification

    private String cstr;    //Technology resource identification

    private String year; //The year the dataset was published


    /**
     * Physical file data
     */
    private List<FileInfoFromInstdb> fileInfo;

}
