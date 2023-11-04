package cn.cnic.instdb.model.resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


/**
 * community community
 */
@Data
@Document(collection = "community")
public class Community {

    @Id
    private String id;
    private String name;

    //Task Status
    private String state;

    //Is it in execution status
    private String status;

    private int visit;
    private int files_count;
    private int byte_size;
    private int download;
    private int datasets;
    //Number of synchronization completed
    private int sync_count;
    //Record Log
    private String logs;
    //Creation time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    //Modification time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    //Last synchronization time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;
    //Next execution time
    private String nextTime;

}
