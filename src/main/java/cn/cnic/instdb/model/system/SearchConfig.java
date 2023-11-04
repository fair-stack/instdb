package cn.cnic.instdb.model.system;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


@Data
@Document(collection = "search_config")
public class SearchConfig {


    @Id
    private String id;

    private String name;
    private String nameEn;
    //Corresponding field
    private String field;
    //Field Type
    private String fieldType;
    private String sort;
    //1 apply -1  apply
    private String status;
    //left right
    private String type;

    private String username;

    //Update time
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public SearchConfig( String name, String nameEn, String field,String fieldType, String sort, String status, String type, String username) {
        this.name = name;
        this.nameEn = nameEn;
        this.field = field;
        this.fieldType = fieldType;
        this.sort = sort;
        this.status = status;
        this.type = type;
        this.username = username;
        this.updateTime = LocalDateTime.now();
    }

    public SearchConfig() {
    }
}
