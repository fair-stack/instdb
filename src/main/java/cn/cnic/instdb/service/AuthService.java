package cn.cnic.instdb.service;

import cn.cnic.instdb.model.rbac.ConsumerDO;
import cn.cnic.instdb.model.rbac.ConsumerDTO;
import cn.cnic.instdb.result.Result;
import org.jasig.cas.client.authentication.AttributePrincipal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface AuthService {

    /**
     *  Login authentication
     * @param emailAccounts
     * @param password
     * @return
     */
    Result login(String emailAccounts, String password,HttpServletResponse response,HttpServletRequest request,String unionId);

    Result emailActivation(String code);

    void logout(HttpServletRequest request, HttpServletResponse response) throws IOException;

    Result register(ConsumerDTO consumerDTO,HttpServletRequest request);
    Result registerSendEmail(String emailAccounts,String name,String type);

    Result getUserInfo(String token);

    void umtLogin(HttpServletResponse response);

    void umtCallback(String code, HttpServletResponse response,HttpServletRequest request) throws IOException;


    void casLogin(HttpServletResponse response);

    void casCallback(AttributePrincipal principal, HttpServletResponse response,HttpServletRequest request) throws IOException;

    ConsumerDO getUserBytoken(String token);




    void wechatLogin(String type,HttpServletResponse response);
    void wechatCallback(String code, String state,HttpServletRequest request, HttpServletResponse response) throws IOException;
    Result wechatAcc(String emailAccounts, String password,HttpServletRequest request, HttpServletResponse response);

    Result wechatRegister(String emailAccounts, String name, String org, HttpServletRequest request,HttpServletResponse response);

    void escLogin(HttpServletResponse response);

    Result wechatUserinfo(HttpServletRequest request);

    void escCallback(String code, HttpServletResponse response) throws IOException;
}
