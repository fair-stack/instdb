package cn.cnic.instdb.model.resources;

import lombok.Data;

import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/17:09
 * @Description: Request access approval submission parameters
 */

@Data
public class ApplyAccessApproval {
    private String id;
    private String approvalStatus;
    private String reason;
    //Visit duration  Visit duration unlimited  Visit duration range
    private String accessPeriod;
    private String startTime;
    private String endTime;

    //access0access  access unlimited  access range
    private String accessData;
    //Data access restrictions  Data access restrictions unlimited  Data access restrictions range
    private List<String> filesId;

}
