package cn.cnic.instdb.service;

import cn.cnic.instdb.model.center.Org;
import cn.cnic.instdb.result.Result;

import java.io.File;
import java.util.Map;

public interface ExternalInterService {


    /**
     * datacite doi register
     * @param resourcesId
     * @return
     */
    String dataciteDoi(String resourcesId);


    Result accessDataInfo(String id, String type,String keyword);

    String applyCSTR(String resourcesId,String doi);

    Result checkCstr(String cstrCode);

    String registerDOI(String resourcesId);

    /**
     * China doiregister
     * @param resourcesId
     * @return
     */
    String registerChinaDOI(String resourcesId);

    Result checkDoi(String doiCode);

    Result findTemplateById(String id);

    Result accessOrgList(String id);

    /**
     * Make Trusted  Make Trusted、Make Trusted、Make Trusted、Make Trusted
     * @param resourcesId
     */
   void setCredible(String resourcesId);

    /**
     * Add Institution
     * @param org
     * @return
     */
    Result orgAdd(Org org);

    /**
     * Add paper
     * @param mapParam
     * @return
     */
    Result paperAdd( Map<String,Object> mapParam);

    /**
     * add item
     * @param mapParam
     * @return
     */
    Result projectAdd( Map<String,Object> mapParam);

    /**
     * instdbBinding mechanism
     * @param host
     * @param orgId
     * @param cstr
     * @return
     */
    Result bandOrg(String host,String cstr,String orgId);

    /**
     * Synchronize templates
     * @param orgId
     * @param id
     * @param file
     * @return
     */
    Result syncTemplate (String orgId,String id,String name,String type,String typeName, File file);

    /**
     * Delete Template Delete Template
     * @param id
     * @return
     */
    Result deleteTemplate(String id);

    /**
     * Query corresponding institution information based on account password
     * @param account
     * @param password
     * @return
     */
    Result findOrgByAccount(String account,String password);

}
