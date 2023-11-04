//package cn.cnic.instdb.config;
//
//import cn.cnic.instdb.constant.Constant;
//import cn.cnic.instdb.model.login.LoginConfigCas;
//import org.jasig.cas.client.authentication.AuthenticationFilter;
//import org.jasig.cas.client.session.SingleSignOutFilter;
//import org.jasig.cas.client.session.SingleSignOutHttpSessionListener;
//import org.jasig.cas.client.util.AssertionThreadLocalFilter;
//import org.jasig.cas.client.util.HttpServletRequestWrapperFilter;
//import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.EventListener;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Configuration
//@Component
//public class FilterConfig {
//
//
////    //casCertification Service Center Address
////    private static final String CAS_SERVER_URL_PREFIX = "http://127.0.0.1:8089/cas";
////
////    //casCertification Service Center   Certification Service Center
////    private static final String CAS_SERVER_URL_LOGIN = "http://127.0.0.1:8089/cas/login";
////
////    //Your own client1Your own client
// //  private static final String SERVER_NAME = "http://127.0.0.1:81";
//
//
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    private LoginConfigCas getCasCig() {
//
//        List<LoginConfigCas> all = mongoTemplate.findAll(LoginConfigCas.class);
//        if (all.size() > 0) {
//            return all.get(0);
//        }
//        return null;
//    }
//
//
//    /**
//     * description: Login Filter
//     * @param: []
//     * @return: org.springframework.boot.web.servlet.FilterRegistrationBean
//     */
//    @Bean
//    public FilterRegistrationBean filterSingleRegistration() {
//
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        LoginConfigCas casCig = getCasCig();
//        if(null == casCig){
//            return registration;
//        }
//        registration.setFilter(new SingleSignOutFilter());
//        // Set matching paths
//       registration.addUrlPatterns("/*");
//        Map<String,String> initParameters = new HashMap<>();
//        initParameters.put("casServerUrlPrefix", casCig.getCasServerUrl());
//        registration.setInitParameters(initParameters);
//        // Set the loading order
//        registration.setOrder(1);
//        return registration;
//    }
//
//
//    /**
//     * description:Filter validator
//     *     * @param: []
//     * @return: org.springframework.boot.web.servlet.FilterRegistrationBean
//     */
//    @Bean
//    public FilterRegistrationBean filterValidationRegistration() {
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        LoginConfigCas casCig = getCasCig();
//        if(null == casCig){
//            return registration;
//        }
//        registration.setFilter(new Cas20ProxyReceivingTicketValidationFilter());
//        // Set matching paths
////        List<String> strings = Constant.addInterceptors();
////        for (String str:strings) {
////            registration.addUrlPatterns(str);
////        }
//        registration.addUrlPatterns("/*");
//        Map<String,String>  initParameters = new HashMap<String, String>();
//        initParameters.put("casServerUrlPrefix", casCig.getCasServerUrl());
//        initParameters.put("serverName", casCig.getHomePage());
//        initParameters.put("useSession", "true");
//        initParameters.put("redirectAfterValidation", "true");
//        registration.setInitParameters(initParameters);
//        // Set the loading order
//        registration.setOrder(1);
//        return registration;
//    }
//
//
//    /**
//     * description:Authorization Filter
//     * @param: []
//     * @return: org.springframework.boot.web.servlet.FilterRegistrationBean
//     */
//    @Bean
//    public FilterRegistrationBean filterAuthenticationRegistration() {
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        LoginConfigCas casCig = getCasCig();
//        if(null == casCig){
//            return registration;
//        }
//        registration.setFilter(new AuthenticationFilter());
////        // Set matching paths
//        List<String> strings = Constant.addInterceptors();
//        for (String str:strings) {
//            registration.addUrlPatterns(str);
//        }
//    //    registration.addUrlPatterns("/*");
//        Map<String,String>  initParameters = new HashMap<String, String>();
//        initParameters.put("casServerLoginUrl", casCig.getCasServerUrlLogin());
//        initParameters.put("serverName", casCig.getHomePage());
//
//
//        //Set Ignore  Set Ignore
////        initParameters.put("ignorePattern", "/doc.html");
////        initParameters.put("ignorePattern", "/*.css");
////        initParameters.put("ignorePattern", "/*.js");
//
//        registration.setInitParameters(initParameters);
//        // Set the loading order
//        registration.setOrder(2);
//        return registration;
//    }
//
//
//    /**
//     * wraperfilter
//     * @return
//     */
//    @Bean
//    public FilterRegistrationBean filterWrapperRegistration() {
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        LoginConfigCas casCig = getCasCig();
//        if(null == casCig){
//            return registration;
//        }
//        registration.setFilter(new HttpServletRequestWrapperFilter());
//        // Set matching paths
//        registration.addUrlPatterns("/*");
//        // Set the loading order
//        registration.setOrder(3);
//        return registration;
//    }
//
//    /**
//     * Add Listener
//     * @return
//     */
//    @Bean
//    public ServletListenerRegistrationBean<EventListener> singleSignOutListenerRegistration(){
//        ServletListenerRegistrationBean<EventListener> registrationBean = new ServletListenerRegistrationBean<EventListener>();
//        registrationBean.setListener(new SingleSignOutHttpSessionListener());
//        registrationBean.setOrder(1);
//        return registrationBean;
//    }
//
//    @Bean
//    public FilterRegistrationBean assertionFilter() {
//        FilterRegistrationBean registration = new FilterRegistrationBean();
//        registration.setName("CAS Assertion Thread Local Filter");
//        registration.setFilter(new AssertionThreadLocalFilter());
//        registration.addUrlPatterns("/*");
//        registration.setOrder(4);  //The smaller the value，FilterThe smaller the value。
//        return registration;
//    }
//
//
//}
