package cn.cnic.instdb.model.system;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Installation component information - Installation component information
 */
@Data
public class ComponentUpdate {
    @NotNull(message = "id is not null")
    private String id;

    private List<Map<String,Object>> parameters;

}
