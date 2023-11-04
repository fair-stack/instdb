package cn.cnic.instdb.service;

import cn.cnic.instdb.result.Result;

import java.util.Map;

public interface EsDataService {

    /**
     * Reset initializationes Reset initialization
     * The purpose of setting an account password is to The purpose of setting an account password is to
     *
     * @return
     */
    Result resetES();


    Result save(String id);
    Result saveEsAll();

    Result update(String id, String field, String value);

    Result delete(String id);
    Result deleteAll(String id);

    //Based on the dataset'sidBased on the dataset's
    void updateProject(String resourcesId);
    //Update all project numbers  Update all project numbers：Update all project numbers  Update all project numbersidUpdate all project numbers Update all project numbers Update all project numbers
    Result updateProjectAll();

    String add(Map map);

    Result updateDate(String status,String state);
}
