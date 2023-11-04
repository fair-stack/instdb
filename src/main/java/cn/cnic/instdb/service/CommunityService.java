package cn.cnic.instdb.service;

import cn.cnic.instdb.model.findata.PushFinDatasParamVo;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

public interface CommunityService {


    /**
     * validateapiKey validate
     *
     * @param token
     * @param apiKey
     * @return
     */
    Result getScidbCommunity(String token, String apiKey, String name);


    /**
     * Start a series of data processing
     *
     * @param token
     * @param apiKey
     * @param name
     * @return
     */
    void getScidbCommunityData();


    /**
     * Modifying Community Data
     * @param token
     * @param apiKey
     * @param name
     * @return
     */
    Result updateScidbCommunity(@RequestHeader("Authorization") String token, String apiKey, String name);


    /**
     * Delete Community Data
     * @param token
     * @param id
     */
    Result deleteScidbCommunity(@RequestHeader("Authorization") String token, String id);


    /**
     * cease/cease
     * @param token
     * @param apiKey
     * @param state
     * @return
     */
    Result disableCommunityState(String token, String apiKey, String state);


    /**
     * Community List
     *
     * @param token
     * @param name
     * @return
     */
    Result getCommunityList(String token, String name);

    /**
     * Pull againftpPull again
     *
     */
    void getFtpFileByDoi();

    /**
     * Push tofindata
     * @param ids
     */
    Result manualPushFinData(String type,List<String> ids);


    /**
     * PushfindataPush
     * @param type
     * @param resourceType
     * @param version
     * @param name
     * @param startDate
     * @param endDate
     * @param pageOffset
     * @param pageSize
     * @return
     */
    PageHelper getPushFinDatas(String type, String resourceType, String version, String name, String startDate, String endDate, Integer pageOffset, Integer pageSize);

    /**
     * queryfinddataquery
     * @param from
     * @return
     */
    Result getFindataStatistics();

    /**
     * Batch recommendation rule settings
     * @param pushFinDatasParam
     * @return
     */
    Result batchPushDataToFindata(String token,PushFinDatasParamVo pushFinDatasParam);

    /**
     * findataRecommended configuration
     * @param token
     * @return
     */
    Result getPushDataToFindataConfig(String token);

    /**
     * allocationfindataallocation
     * @param token
     * @param status
     * @return
     */
    Result setfindataStatus(String token, String status);


}
