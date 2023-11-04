package cn.cnic.instdb.service;

import cn.cnic.instdb.model.config.*;
import cn.cnic.instdb.result.Result;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/04/07/19:03
 * @Description:
 */
public interface SystemConfigService {


    /**
     * Basic configuration-Basic configuration
     */
    Result setBasicConfig(String token, BasicConfigurationDTO basicConfigurationDTO);

    /**
     * Resource allocation
     * @param resourcesTopLogo
     * @param resourcesEndLogo
     * @return
     */
    Result setResourcesConfig(String resourcesTopLogo,String resourcesEndLogo);

    /**
     * Basic Configuration Query
     *
     * @return
     */
    BasicConfigurationVo getBasicConfig();


    /**
     * About our configuration
     * @param token
     * @param aboutConfiguration
     * @return
     */
    Result  setAboutConfig( String token,  AboutConfigurationDTO aboutConfiguration);


    /**
     * Get information about us
     * @return
     */
    AboutConfiguration  getAboutConfig();

    /**
     * Homepage Configuration
     * @param token
     * @param indexConfig
     * @return
     */
    Result  setIndexConfig( String token,  IndexConfigurationDTO indexConfig);


    /**
     * Get homepage configuration
     * @return
     */
    IndexConfiguration  getIndexConfig();

    Result setSubjectAreaConfig(SubjectAreaIndex subjectAreaIndex);


    Result setDsUrl(String dsUrl);
}
