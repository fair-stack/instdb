package cn.cnic.instdb.service;

import cn.cnic.instdb.model.special.Special;
import cn.cnic.instdb.model.special.SpecialVo;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SpecialService {

    /**
     * Creating a Theme
     * @param specialDTO
     * @return
     */
    Result save(String token,Special.SpecialDTO specialDTO);

    Result update(String token,Special.SpecialDTO specialDTO);

    Result delete(String token,String id);

    /**
     * Topic List
     * @param specialName
     * @param pageOffset
     * @param pageSize
     * @return
     */
    PageHelper findAllSpecial(String specialName, Integer pageOffset, Integer pageSize,String sort);


    /**
     * Topic Details
     * @param id
     * @return
     */
    SpecialVo getSpecialById(String id);


    /**
     * Update three numbers of topics
     * @param specialVo
     */
    void updateNumSpecial(List<SpecialVo> specialVo);




    /**
     * Topic Add Administrator List
     * @param specialId
     * @param pageOffset
     * @param pageSize
     * @return
     */
    List<Map<String, String>> userList(String specialId,String username, Integer pageOffset, Integer pageSize);


    /**
     * Add administrator list before creating a topic
     * @param pageOffset
     * @param pageSize
     * @return
     */
    List<Map<String, String>> addSpecialUserList( String username, Integer pageOffset, Integer pageSize);

    /**
     * Topic Add Administrator
     * @param specialId
     * @param userId
     */
    void addAdministrators(String specialId, String userId);

    /**
     * Topic deletion administrator
     * @param specialId
     * @param userId
     */
    void deleteAdministrators(String specialId, String userId);
}
