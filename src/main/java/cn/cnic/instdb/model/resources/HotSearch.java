package cn.cnic.instdb.model.resources;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "hot_search")
public class HotSearch {
    @Id
    private String hotId;
    private String hotName;


    private int frequency;
    private LocalDateTime createDate;

}
