package cn.cnic.instdb.controller;

import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.EsDataService;
import cn.cnic.instdb.utils.SMS4;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

/**
 * @Auther: wdd
 * @Date: 2021/04/07/10:01
 * @Description:
 */

@RestController
@Slf4j
@RequestMapping(value = "/es")
public class EsDataController extends ResultUtils {

    @Resource
    private EsDataService esDataService;

    @ApiOperation("initialization-elasticsearch(initialization)")
    @RequestMapping(value = "/resetES", method = RequestMethod.POST)
    public Result resetES(String decrypt) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }
        return esDataService.resetES();
    }

//    public static void main(String[] args) {
//
//
//        String encryption = SMS4.Encryption(System.currentTimeMillis() + "");
//        System.out.println(encryption);
//        encryption  = "EBCDC750F0C1438AB7F38E1D092284D0930B0EE5D7F9E7A1CCCFCD5D5FE3917F";
//        boolean s = checkUserName(encryption);
//        System.out.println(s);
//
//    }

    private static boolean checkUserName(String decrypt) {
        try {
            decrypt = SMS4.Decrypt(decrypt);
            if (StringUtils.isBlank(decrypt)) {
                return false;
            }
            Long activationTime = Long.valueOf(decrypt);
            //Get the current time24Get the current time
            Calendar c = Calendar.getInstance();
            c.setTime(new Date());
            c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) - 2);
            long time = c.getTime().getTime();
            if (activationTime.longValue() < time) {
                log.error("overdue ");
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @PostMapping("/saveEsAll")
    public Result saveEsAll(String decrypt) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }
        return esDataService.saveEsAll();
    }


    @PostMapping("/save")
    public Result entry(@RequestHeader("id") String id, String decrypt) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }
        return esDataService.save(id);
    }

    @PostMapping("/update")
    public Result update(@RequestHeader("id") String id, String decrypt, String field, String value) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }
        return esDataService.update(id, field, value);
    }

    @PostMapping("/delete")
    public Result delete(@RequestHeader("id") String id, String decrypt) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }
        return esDataService.delete(id);
    }


    @PostMapping("/deleteAll")
    public Result deleteAll(@RequestHeader("id") String id, String decrypt) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }

        if (StringUtils.isBlank(id)) {
            return ResultUtils.error("id Is empty ！！！");
        }
        return esDataService.deleteAll(id);
    }


    @PostMapping("/updateProject")
    public Result updateProject(@RequestHeader("id") String resourcesId, String decrypt) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }
        esDataService.updateProject(resourcesId);
        return success();
    }


    @PostMapping("/updateProjectAll")
    public Result updateProjectAll(String decrypt) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }
        return esDataService.updateProjectAll();
    }

    @PostMapping("/updateDate")
    public Result updateDate(String decrypt, String status, String state) {
        if (!checkUserName(decrypt)) {
            return ResultUtils.error("Password input error！！！");
        }
        return esDataService.updateDate(status, state);
    }

}

