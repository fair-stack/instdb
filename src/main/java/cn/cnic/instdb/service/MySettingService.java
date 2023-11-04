package cn.cnic.instdb.service;

import cn.cnic.instdb.model.system.*;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/04/07/19:03
 * @Description:
 */
public interface MySettingService {



    /**
     * Password strength verification
     *
     * @param password
     * @return
     */
    String checkPassword(String password);


    /**
     * security setting-security setting
     *
     * @param token
     * @return
     */
    String verification(String token);


    /**
     * Retrieve password-Retrieve password
     * @param email
     * @param name
     * @param request
     * @return
     */
    Result pwdEmail(String email, String name,String randomStr,String captcha, HttpServletRequest request);

    /**
     * Change password
     *
     * @param password
     * @param confirmPassword
     */
    Result updatePwd(String password, String confirmPassword, HttpServletRequest request);


    Result passActivation(String code, HttpServletRequest request, HttpServletResponse response) throws IOException;
    Result expertReview(String code, HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Query alternate email
     *
     * @param token
     * @return
     */
    String spareEmail(String token);


    /**
     * Set up an alternate mailbox
     *
     * @param token
     * @param spareEmail
     * @return
     */
    Result setEmail(String token, String spareEmail);

    /**
     * Get a personal overview
     * @param token
     * @return
     */
   Result getOverview(String token);


    /**
     * Line resource statistics
     * @param token
     * @return
     */
    Result getStatisticsResources(String token);
    /**
     * Line user statistics
     * @param token
     * @return
     */
    Result getStatisticsUser(String token);


}
