package cn.cnic.instdb.service;

import cn.cnic.instdb.model.system.Component;
import cn.cnic.instdb.model.system.ComponentUpdate;
import cn.cnic.instdb.result.Result;

import javax.servlet.http.HttpServletRequest;

/**
 * @Auther: wdd
 * @Date: 2021/04/07/19:03
 * @Description:
 */
public interface FairmanComponentService {


    /**
     * List of installed market components
     * @param token
     * @param page
     * @param size
     * @param category
     * @param name
     * @return
     */
    Result installList(String token, Integer page, Integer size,String category,String name);


    /**
     * Obtain a list of market components
     * @param token
     * @param page
     * @param size
     * @param sort
     * @param category
     * @param name
     * @return
     */
    Result component(String token, Integer page,Integer size,  Integer sort, String category, String name);


    /**
     * Aggregated statistical data on the left side of market components
     * @param token
     * @return
     */
    Result aggData(String token);


    /**
     * Install market components
     * @param token
     * @param component
     * @return
     */
    Result componentInstall(String token,Component component);


    /**
     * Edit installed market component configuration information
     * @param token
     * @param component
     * @return
     */
    Result componentEdit(String token,ComponentUpdate component);


    /**
     * Remove installed market components
     * @param token
     * @param id
     * @return
     */
    Result componentRemove(String token,String id);


    /**
     * File preview-File preview
     * @param token
     * @param resourcesId
     * @param fileId
     * @return
     */
    Result getComponent(String token,
                        String resourcesId,
                        String fileId
    );


    /**
     * File preview-File preview
     *
     * @param token
     * @param resourcesId
     * @param fileId
     * @param componentId
     * @param request
     */
    Result previewData(String token,
                       String resourcesId,
                       String fileId,
                       String componentId,
                       HttpServletRequest request);

}
