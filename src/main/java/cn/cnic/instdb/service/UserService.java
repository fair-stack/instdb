package cn.cnic.instdb.service;

import cn.cnic.instdb.model.rbac.*;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface UserService {

    // User addition
    Result add(String token, ManualAdd manualAdd);

    Result update(String token, ConsumerInfoDTO consumerInfoDTO);

    Result changeThePicture(String token,String id,String avatar);

    Result getUserInfoByUserId(String userId);

   ConsumerDO getUserInfoByName(String emailAccounts);

    /**
     * Modify Users
     * @param token
     * @param manualAdd
     * @return
     */
    Result adminUserUpdate(String token, ManualAdd manualAdd);

    /**
     * User List
     * @param token
     * @param pageOffset
     * @param pageSize
     * @param name
     * @param email
     * @return
     */
    PageHelper userList(String token, int pageOffset, int pageSize, String name, String email,String orgChineseName,String role,String sort);

    /**
     * Disabled/Disabled
     * @param token
     * @param userId
     * @param state
     */
    void disable(String token,String userId, String state);

    /**
     * Role List
     * @param token
     * @return
     */
    List<Role> roleList(String token);

    HSSFWorkbook export();

    Result importUser(String token, MultipartFile blobAvatar);

    Result addUserList(String token, ManualAddList manualAddList);

    /**
     * User deletion
     * @param id
     */
    Result deleteUserById(String token,String id);


    /**
     * List of administrators and auditors  List of administrators and auditors
     * @param token
     * @return
     */
    List<Map> adminUserList();

}
