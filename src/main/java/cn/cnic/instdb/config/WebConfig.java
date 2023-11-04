package cn.cnic.instdb.config;

import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.interceport.WebInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig
 *
 * @author jmal
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private WebInterceptor webInterceptor;

    @Value("${data.special.logo}")
    private String specialLogo;

    @Value("${data.user.logo}")
    private String userLogo;

    @Value("${data.subject.logo}")
    private String subjectLogo;

    @Value("${data.banaer_favicon.logo}")
    private String banaer_icoLogo;

    @Value("${data.resources.picture}")
    private String dataset_Logo;


    @Value("${data.install_component_source}")
    private String install_component_source;


    @Value("${data.resources.file}")
    private String resourcesFile;



    /**
     * Register Interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInterceptor).addPathPatterns(
          Constant.addInterceptors()
        ).excludePathPatterns(Constant.getExcludePathPatternsList());
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/specialLogo/**").addResourceLocations("file:" + specialLogo);
        registry.addResourceHandler("/userLogo/**").addResourceLocations("file:" + userLogo);
        registry.addResourceHandler("/subjectLogo/**").addResourceLocations("file:" + subjectLogo);
        registry.addResourceHandler("/banaer_icoLogo/**").addResourceLocations("file:" + banaer_icoLogo);
        registry.addResourceHandler("/datasetLogo/**").addResourceLocations("file:" + dataset_Logo);
        registry.addResourceHandler("/package/**").addResourceLocations("file:" + install_component_source);
        registry.addResourceHandler("/dwn/**").addResourceLocations("file:" + resourcesFile);
        WebMvcConfigurer.super.addResourceHandlers(registry);
    }

}
