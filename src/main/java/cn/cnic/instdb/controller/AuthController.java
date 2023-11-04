package cn.cnic.instdb.controller;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.rbac.ConsumerDTO;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.AuthService;
import cn.cnic.instdb.utils.CommonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.util.AssertionHolder;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Login、Login、Login
 *
 * @author jmal
 */
@RestController
@Api(tags = "Login authentication")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;

    @ApiOperation("Login")
    @PostMapping("/login")
    public Result<Object> login(@RequestParam(name = "emailAccounts") String emailAccounts,
                                @RequestParam(name = "password") String password,
                                HttpServletResponse response,HttpServletRequest request,String unionId) {
        return authService.login(emailAccounts, password, response,request,unionId);
    }

    @ApiOperation("register")
    @PostMapping("/register")
    public Result register(@RequestBody ConsumerDTO consumerDTO,HttpServletRequest request) {
        return authService.register(consumerDTO,request);
    }


    /**
     * Based on logintokenBased on login
     *
     * @param request
     */
    @ApiOperation("Based on logintokenBased on login")
    @GetMapping("/get.u")
    public Result getUserInfo(HttpServletRequest request) {
        String token = CommonUtils.getCookie(request, "token");
        if (token == null) {
            return ResultUtils.error("LOGIN_EXCEPTION");
        }
        return authService.getUserInfo(token);
    }


    @ApiOperation("Resend email on registration page")
    @PostMapping("/register/send/email")
    public Result registerSendEmail(@RequestParam(name = "emailAccounts") String emailAccounts,@RequestParam(name = "name") String name,String type) {
        if(StringUtils.isBlank(emailAccounts) || StringUtils.isBlank(name)|| StringUtils.isBlank(type)){
            return ResultUtils.error("PARAMETER_ERROR");
        }
        return authService.registerSendEmail(emailAccounts,name,type);
    }


    @ApiOperation("Log out")
    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
          authService.logout(request, response);
          return;
    }


    /**
     * Technology Cloud Login
     *
     * @param response
     */
    @ApiOperation("Technology Cloud Login")
    @GetMapping("/umt.log")
    public void umtLogin(HttpServletResponse response) {
        authService.umtLogin(response);
        return;
    }

    @ApiOperation("Technology Cloud Login Callback")
    @GetMapping("/callback")
    public void umpCallback(HttpServletResponse response,HttpServletRequest request,
                            @RequestParam(name = "code", required = false) String code) throws IOException {
        log.info("-- way:ump callback success --");
        if (StringUtils.isEmpty(code)) {
            response.sendError(500);
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.getOutputStream().print("param code error");
            response.flushBuffer();
        }
        authService.umtCallback(code, response,request);
        return;
    }

//    @ApiOperation("according to")
//    @GetMapping("/getAccountNameFromCas")
//    public static String getAccountNameFromCas(HttpServletRequest request) {
//        Assertion assertion = (Assertion) request.getSession().getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);
//        if(assertion!= null){
//            AttributePrincipal principal = assertion.getPrincipal();
//            System.out.println(principal.getName());
//            return principal.getName();
//        }else{
//            return null;
//        }
//    }

    /**
     * casLogin page jump
     *
     * @param response
     */
    @ApiOperation("casLogin")
    @GetMapping("/cas/login")
    public void casLogin(HttpServletResponse response){
        authService.casLogin(response);
        return;
    }

    /*
     * Unified authentication callback
     * */
    @ApiOperation("casLogin callback")
    @GetMapping("/cas/callback")
    public void casCallback(HttpServletRequest request,HttpServletResponse response){
        log.info("-- way:cas callback success --");
        try {
            Assertion assertion = AssertionHolder.getAssertion();
            if(null == assertion) {
                response.setStatus(Integer.valueOf(Constant.StatusCode.NO_LOGIN));
                response.sendError(401);
                return;
            }

            AttributePrincipal principal = assertion.getPrincipal();
            if(null == principal.getName()){
                response.setStatus(Integer.valueOf(Constant.StatusCode.NO_LOGIN));
                response.sendError(401);
                return;
            }
           authService.casCallback(principal,response,request);

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(Integer.valueOf(Constant.StatusCode.NO_LOGIN));
            return;
        }
    }

//    /**
//     * casSuccessful logout callback
//     *
//     * @param session
//     * @return
//     */
//    @RequestMapping("/logoutCAS")
//    public void logoutCAS(HttpSession session,HttpServletResponse response) {
//        try {
//            if(session != null) {
//                //Must be destroyedsession
//                session.invalidate();
//            }
//            //deletecookie
//            Cookie cookie = CommonUtils.setCookie("cas", "", 0, 1);
//            response.addCookie(cookie);
//            //Redirect to client homepage here，Redirect to client homepage here
//            response.sendRedirect(instdbUrl.getCallHost());
//            return;
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }


    /**
     * WeChat login
     *
     * @param response
     */
    @ApiOperation("Shared Network Login")
    @GetMapping("/esc.login")
    public void escLogin(HttpServletResponse response) {
        authService.escLogin(response);
        return;
    }


    /**
     *  Shared network callback
     * @param response
     * @param code
     * @throws IOException
     */
    @ApiOperation("Shared Network Login Callback")
    @GetMapping("/escience/callback")
    public void escCallback(HttpServletResponse response,
                            @RequestParam(name = "code", required = false) String code) throws IOException {
        log.info("-- escience: callback success --");
        if(StringUtils.isEmpty(code)){
            response.sendError(500);
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.getOutputStream().print("param code error");
            response.flushBuffer();
            return;
        }
        authService.escCallback(code,response);
        return;
    }

    /**
     * WeChat login
     *
     * @param response
     */
    @ApiOperation("WeChat login")
    @GetMapping("/wechat.login")
    public void wechatLogin(@RequestParam(name = "type",required = false) String type,
                            HttpServletResponse response) {
        authService.wechatLogin(type,response);
        return;
    }


    @ApiOperation(value = "Obtain personal information of WeChat users")
    @GetMapping("/wechat.info")
    public Result wechatUserinfo(HttpServletRequest request) {
        return authService.wechatUserinfo(request);
    }


    @ApiOperation(value = "WeChat users bind accounts and log in")
    @GetMapping("/wechat.acc")
    public Result wechatAcc(@RequestParam(name = "emailAccounts") String emailAccounts,
                                            @RequestParam(name = "password") String password,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        return authService.wechatAcc(emailAccounts,password,request,response);
    }


    @ApiOperation(value = "WeChat user registration binding user")
    @GetMapping("/wechat.register")
    public Result wechatRegister(@RequestParam(name = "emailAccounts") String emailAccounts,
                                                 @RequestParam(name = "name") String name,
                                                 @RequestParam(name = "org") String org,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        return authService.wechatRegister(emailAccounts, name,org,request, response);
    }

}
