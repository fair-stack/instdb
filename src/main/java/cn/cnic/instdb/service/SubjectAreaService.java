package cn.cnic.instdb.service;

import cn.cnic.instdb.model.system.Subject;
import cn.cnic.instdb.model.system.SubjectArea;
import cn.cnic.instdb.model.system.SubjectAreaDTO;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.utils.PageHelper;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @Auther: wdd
 * @Date: 2021/04/07/19:03
 * @Description:
 */
public interface SubjectAreaService {

    Result save(SubjectAreaDTO subjectAreaDTO);

    Result update(SubjectAreaDTO subjectAreaDTO);

    void deleteById(String id);

    void deleteByIds(List<String> ids);

    SubjectArea getSubjectAreaById(String id);

    PageHelper getSubjectAreaAll(Map<String, Object> condition);

    List<String> getSubjectByName(List<String> subject,String lang);

    /**
     * Discipline configuration cascade
     * @return
     */
    List<Subject> getSubjectAreaInfo();
    List<Subject> getSubjectAreaInfoTwo(String id,String no);
    List<Subject> getSubjectAreaInfoThree(String id,String no);

    /**
     * Query all disciplines
     * @return
     */
    Result getSubjectList();

    //Dropdown list of subject areas
    Result getSubjectArea();

    /**
     * Upload small icons for subject areas
     * @param icon
     * @param iconColor
     * @return
     */
    Result uploadIco(String id,String icon,String iconColor);



}
