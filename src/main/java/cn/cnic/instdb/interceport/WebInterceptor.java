package cn.cnic.instdb.interceport;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.service.InstdbApiService;
import cn.cnic.instdb.utils.CaffeineUtil;
import cn.cnic.instdb.utils.CommonUtils;
import cn.cnic.instdb.utils.I18nUtil;
import cn.cnic.instdb.utils.JwtTokenUtils;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @Auther wdd
 * @Date 2021/4/7 16:06
 * @Desc request interceptor 
 */
@Slf4j
@Component
public class WebInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    @Autowired
    private InstdbApiService instdbApiService;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        response.addHeader("Content-Type", "application/json;charset=UTF-8");
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/json");



        String authToken = jwtTokenUtils.getToken(request);
        String requestURI = request.getRequestURI();
        boolean judge = true;
        Integer code = null;
        String message = null;
        Map<String, Object> param = new HashMap<>();
//        BasicConfigurationVo basicConfig = systemConfigService.getBasicConfig();
//        if (null != basicConfig && StringUtils.isNotBlank(basicConfig.getNoLoginAccess()) && Constant.Approval.NO.equals(basicConfig.getNoLoginAccess())) {
//            String token = CommonUtils.getCookie(request, "token");
//            if (StringUtils.isBlank(token)) {
//                param.put("code", 401);
//                param.put("message", I18nUtil.get("NEED_TOKEN"));
//                CommonUtils.errorMsg(response, param);
//                response.flushBuffer();
//                return false;
//            }
//            String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
//            if (StringUtils.isBlank(userIdFromToken)) {
//                param.put("code", 401);
//                param.put("message", I18nUtil.get("NEED_TOKEN"));
//                CommonUtils.errorMsg(response, param);
//                response.flushBuffer();
//                return false;
//            }
//        }

        if(Constant.useList(Constant.Api.includeUrls,requestURI)){
            int apiCode = instdbApiService.checkToken(request);
            //checktoken
            if (200 != apiCode){
                code = apiCode;
                message = CommonUtils.getApiMsgByCode(code);
                judge = false;
            }
        }else if (authToken == null) {
            code = 401;
            message = I18nUtil.get("NEED_TOKEN");
            judge = false;
        } else if (!jwtTokenUtils.validateToken(authToken)) {
            code = 401;
            message = I18nUtil.get("LOGIN_EXCEPTION");
            judge = false;
        } else {
            String ifPresent = tokenCache.getIfPresent(authToken);
            if (ifPresent == null) {
                code = 401;
                message = I18nUtil.get("LOGIN_EXCEPTION");
                judge = false;
            }
            //authentication 
            List<String> unAuthPath = jwtTokenUtils.getUnAuthPath(authToken);
            if(null != unAuthPath) {
                if (unAuthPath.contains(requestURI)) {
                    code = 403;
                    message = I18nUtil.get("PERMISSION_DENIED");
                    judge = false;
                }
            }
        }

        if (null != code && 401 == code) {
            String referer = request.getHeader("Referer");
            if (StringUtils.isNotBlank(referer) && !referer.contains("/login") && !referer.contains("/register") && !referer.contains("/logout")) {
                HttpSession session = request.getSession();
                session.setAttribute("umtCallbackPreUrl", referer);
            }
        }

        if (!judge) {
            param.put("code",code);
            param.put("message",message);
            CommonUtils.errorMsg(response, param);
            response.flushBuffer();
        }
        String lang = request.getHeader("lang");
        if(StringUtils.isNotBlank(lang)){
            tokenCache.put("lang",lang);
        }
        return judge;
    }



}
