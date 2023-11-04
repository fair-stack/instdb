package cn.cnic.instdb.service.impl;

import cn.cnic.instdb.async.AsyncDeal;
import cn.cnic.instdb.constant.Constant;
import cn.cnic.instdb.model.rbac.*;
import cn.cnic.instdb.model.system.EmailModel;
import cn.cnic.instdb.model.system.ToEmail;
import cn.cnic.instdb.repository.UserRepository;
import cn.cnic.instdb.result.Result;
import cn.cnic.instdb.result.ResultUtils;
import cn.cnic.instdb.service.UserService;
import cn.cnic.instdb.utils.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * user-service
 *
 * @author chl
 * @date 2021/3/19
 */

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, Integer> errorPwdCache = CaffeineUtil.getErrorPwd();

    private final Cache<String, String> errorPwdCheck = CaffeineUtil.getErrorPwdCheck();

    public static final String COLLECTION_NAME = "db_user";
    //127.0.0.1

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    @Lazy
    private AsyncDeal asyncDeal;

    @Resource
    private MongoUtil mongoUtil;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Resource
    private InstdbUrl instdbUrl;

    @Resource
    private UserRepository userRepository;


    @Override
    public Result add(String token, ManualAdd manualAdd) {
        Token user = jwtTokenUtils.getToken(token);
        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(null != roles, I18nUtil.get("LOGIN_EXCEPTION"));

        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        String email = RSAEncrypt.decrypt(manualAdd.getEmailAccounts());
        //Email verification
        if (!CommonUtils.isEmail(email)) {
            return ResultUtils.error("EMAIL_INCORRECT");
        }

        ConsumerDO consumer = this.getUserInfoByName(email);
        if (null != consumer) {
            return ResultUtils.error("USER_ALREADY_EXISTS");
        }

        String name = RSAEncrypt.decrypt(manualAdd.getName());
        String orgEnglishName = RSAEncrypt.decrypt(manualAdd.getOrgChineseName());

        ConsumerDO consumerDO = new ConsumerDO();
        BeanUtils.copyProperties(manualAdd, consumerDO);
        consumerDO.setName(name);
        consumerDO.setEmailAccounts(email);
        consumerDO.setState(1);
        consumerDO.setOrgChineseName(orgEnglishName);
        consumerDO.setAddWay("Page addition");
        consumerDO.setCreateTime(LocalDateTime.now());
        consumerDO.setRoles(manualAdd.getRoles());
        mongoTemplate.save(consumerDO, COLLECTION_NAME);

        //Send email
        long stringTime = new Date().getTime();
        String code = SMS4.Encryption("pwdBack&" + email + "&" + stringTime);
        Map<String, Object> param = new HashMap<>(16);
        param.put("name", user.getName());
        param.put("email", user.getEmailAccounts());
        param.put("url", instdbUrl.getCallHost() + instdbUrl.getSetUpPsd() + code);

        ToEmail toEmail = new ToEmail();
        toEmail.setTos(new String[]{consumerDO.getEmailAccounts()});
        asyncDeal.send(toEmail, param, EmailModel.EMAIL_INVITE());

        return ResultUtils.success();
    }


    @Override
    public Result update(String token, ConsumerInfoDTO user) {

        String email = tokenCache.getIfPresent(token);
        if (StringUtils.isBlank(email)) {
            return ResultUtils.error("LOGIN_EXCEPTION");
        }
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            String emailAccounts = user.getEmailAccounts();
            if (!email.equals(emailAccounts)) {
                return ResultUtils.error("USER_OTHER");
            }
        }

        if (!CommonUtils.isPhone(user.getTelephone())) {
            return ResultUtils.error("PHONE_INCORRECT");
        }

        Query query = new Query();
        String userId = user.getUserId();
        if (!StringUtils.isEmpty(userId)) {
            query.addCriteria(Criteria.where("_id").is(userId));
        } else {
            String emailAccounts = user.getEmailAccounts();
            if (!StringUtils.isEmpty(emailAccounts)) {
                query.addCriteria(Criteria.where("emailAccounts").is(emailAccounts));
            } else {
                return ResultUtils.success();
            }
        }
        ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class, COLLECTION_NAME);

        Update update = new Update();
        if (!StringUtils.isEmpty(user.getName())) {
            update.set("name", user.getName());
        }
        if (!StringUtils.isEmpty(user.getEnglishName())) {
            update.set("englishName", user.getEnglishName());
        }
        if (!StringUtils.isEmpty(user.getOrgChineseName())) {
            update.set("orgChineseName", user.getOrgChineseName());
        }
        if (!StringUtils.isEmpty(user.getOrgEnglishName())) {
            update.set("orgEnglishName", user.getOrgEnglishName());
        }
        if (!StringUtils.isEmpty(user.getTelephone())) {
            update.set("telephone", user.getTelephone());
        }
        update.set("orcId", user.getOrcId());
        String introduction = user.getIntroduction();
        if (!StringUtils.isEmpty(introduction)) {
            if (200 < introduction.length()) {
                ResultUtils.error("DESC_TOO_LONG");
            }
        }
        update.set("introduction", introduction);

        if (StringUtils.isNotBlank(user.getAvatar())) {
            if (user.getAvatar().contains("data:image")) {
                String uuid = CommonUtils.generateUUID();
                CommonUtils.base64ToFile(instdbUrl.getUserLogo(), user.getAvatar(), uuid);
                update.set("avatar", uuid + Constant.PNG);
                if (StringUtils.isNotBlank(consumerDO.getAvatar())) {
                    FileUtils.deleteFile(instdbUrl.getUserLogo() + consumerDO.getAvatar());
                }
            }
        }

        mongoTemplate.upsert(query, update, COLLECTION_NAME);
        return ResultUtils.success("UPDATE_SUCCESS");
    }

    @Override
    public Result changeThePicture(String token, String id, String avatar) {
        String email = tokenCache.getIfPresent(token);
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class, COLLECTION_NAME);
        if (null == consumerDO) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        if (!email.equals(consumerDO.getEmailAccounts())) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        Update update = new Update();
        update.set("avatar", avatar);
        mongoTemplate.updateFirst(query, update, ConsumerDO.class);

        return ResultUtils.success("USER_AVATAR");
    }

    @Override
    public Result getUserInfoByUserId(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(userId));
        ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class, COLLECTION_NAME);
        if (null == consumerDO) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }

//        //If the avatar is not empty
//        if(StringUtils.isNotBlank(consumerDO.getAvatar()) && !consumerDO.getAvatar().contains("data:image")){
//            String avatarBase64 = CommonUtils.imageToBase64Str(instdbUrl.getUserLogo() + consumerDO.getAvatar());
//            Update update = new Update();
//            update.set("avatar",avatarBase64);
//            mongoTemplate.updateFirst(query,update,ConsumerDO.class);
//        }

//        File file = new File(instdbUrl.getUserLogo()+consumerDO.getAvatar());
//        if(!file.exists()){
//            consumerDO.setAvatar("");
//        }
        ConsumerVo vo = new ConsumerVo();
        BeanUtils.copyProperties(consumerDO, vo);
        vo.setRoles(consumerDO.getRoles());
        return ResultUtils.success(vo);
    }

    @Override
    public ConsumerDO getUserInfoByName(String email) {
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        return mongoTemplate.findOne(query, ConsumerDO.class, COLLECTION_NAME);
    }

    @Override
    public PageHelper userList(String token, int pageOffset, int pageSize, String name, String email,String orgChineseName, String role,String sort) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        String lang = tokenCache.getIfPresent("lang");

        Query query = new Query();
        if (StringUtils.isNotBlank(sort) && sort.contains("&")) {
            String[] s = sort.split("&");
            if (s[0].equals("asc")) {
                query.with(Sort.by(Sort.Direction.ASC, s[1]));
            } else if (s[0].equals("desc")) {
                query.with(Sort.by(Sort.Direction.DESC, s[1]));
            }
        }else {
            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
        }
        if (StringUtils.isNotBlank(name)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(name) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("name").regex(pattern));
        }
        if (StringUtils.isNotBlank(email)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(email) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("emailAccounts").regex(pattern));
        }
        if (StringUtils.isNotBlank(role)) {
            query.addCriteria(Criteria.where("roles").is(role));
        }

        if (StringUtils.isNotBlank(orgChineseName)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(orgChineseName) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("orgChineseName").regex(pattern));
        }

        long count = mongoTemplate.count(query, COLLECTION_NAME);
        mongoUtil.start(pageOffset, pageSize, query);
        List<ConsumerDO> userList = mongoTemplate.find(query, ConsumerDO.class, COLLECTION_NAME);
        for (ConsumerDO user : userList) {
            if (StringUtils.isNotBlank(lang) && Constant.Language.english.equals(lang)) {
                user.setAddWay("register".equals(user.getAddWay()) ? "Sign up" :
                        "Page addition".equals(user.getAddWay()) ? "Page add" :
                                "Batch Add".equals(user.getAddWay()) ? "Batch add" :
                                        "Technology Cloud".equals(user.getAddWay()) ? "CSTCloud" : "Sign up");
            }
        }

        return mongoUtil.pageHelper(count, userList);

    }

    @Override
    public void disable(String token, String userId, String state) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(null != roles, I18nUtil.get("LOGIN_EXCEPTION"));
        Assert.isTrue(roles.contains(Constant.ADMIN), I18nUtil.get("PERMISSION_FORBIDDEN"));

        Query query = new Query().addCriteria(Criteria.where("_id").is(userId));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class);
        Assert.isTrue(null != one, I18nUtil.get("DATA_QUERY_EXCEPTION"));

        Update update = new Update();
        update.set("state", Integer.parseInt(state));

        //Locked state  Locked state
        if (1 == Integer.parseInt(state) &&  2 == one.getState()) {
            //Remove next time login information
            update.unset("nextLoginTime");
            errorPwdCache.invalidate(one.getEmailAccounts() + "_login");
            errorPwdCheck.invalidate(one.getEmailAccounts());
        }
        mongoTemplate.updateFirst(query, update, COLLECTION_NAME);
    }

    @Override
    public List<Role> roleList(String token) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        Assert.isTrue(null != roles, I18nUtil.get("LOGIN_EXCEPTION"));
        List<Role> all = mongoTemplate.findAll(Role.class);
        if (null != all && all.size() > 0) {
            for (Role role : all) {
                String lang = tokenCache.getIfPresent("lang");
                if (Constant.Language.english.equals(lang)) {
                    role.setName(role.getNameEn());
                }
            }
        }
        return all;
    }

    @Override
    public HSSFWorkbook export() {
        HSSFWorkbook workBook = null;
        try {
            workBook = new HSSFWorkbook();
            HSSFSheet sheet = workBook.createSheet();
            HSSFRow row = sheet.createRow(0);
            HSSFCellStyle setBorder = workBook.createCellStyle();
            HSSFFont font2 = workBook.createFont();
            font2.setFontName("Imitation of Song Dynasty_GB2312");
            font2.setBold(true);
            font2.setFontHeightInPoints((short) 12);
            setBorder.setFont(font2);//Choose the font format you want to use
            String[] models = {"mailbox", "mailbox", "mailbox"};
            for (int i = 0; i < models.length; i++) {
                HSSFCell cell = row.createCell(i);
                cell.setCellStyle(setBorder);
                cell.setCellValue(models[i]);
            }
            return workBook;
        } catch (Exception e) {
            log.error("context", e);
            return workBook;
        } finally {
            try {
                workBook.close();
            } catch (Exception e) {
                log.error("context", e);
            }
        }
    }

    @Override
    public Result importUser(String token, MultipartFile blobAvatar) {

        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        if (null == blobAvatar) {
            return ResultUtils.error("Please upload the imported file");
        }
        String fileName = blobAvatar.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (!suffix.equals("xls")) {
            return ResultUtils.error("Please uploadexcelPlease upload");
        }
        List<Map<String, Object>> errorList = new ArrayList<>();
        // List<ConsumerDO> successList = new ArrayList<>();
        InputStream inputStream = null;
        HSSFWorkbook work = null;
        try {
            inputStream = blobAvatar.getInputStream();
            work = new HSSFWorkbook(inputStream);
            //analysis
            int numberOfSheets = work.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                HSSFSheet sheet = work.getSheetAt(i);
                int lastRowNum = sheet.getLastRowNum();
                if (0 == lastRowNum) {
                    return ResultUtils.error("Import file user information cannot be empty!");
                }
                for (int x = 1; x <= lastRowNum; x++) {
                    HSSFRow row = sheet.getRow(x);
                    HSSFCell cell0 = row.getCell(0);
                    if (cell0 == null) {
                        errorList.add(setMessage(x + 1, 1, "Email cannot be empty"));
                        continue;
                    }
                    HSSFCell cell1 = row.getCell(1);
                    if (cell1 == null) {
                        errorList.add(setMessage(x + 1, 2, "Name cannot be empty"));
                        continue;
                    }
                    HSSFCell cell2 = row.getCell(2);
                    if (cell2 == null) {
                        errorList.add(setMessage(x + 1, 3, "The initial password cannot be empty"));
                        continue;
                    }

                    String email = cell0.getStringCellValue();
                    if (!CommonUtils.isEmail(email)) {
                        errorList.add(setMessage(x + 1, 1, "Email format error"));
                        continue;
                    }
                    //Verify if the email exists
                    ConsumerDO userInfoByName = getUserInfoByName(email);
                    if (userInfoByName != null) {
                        errorList.add(setMessage(x + 1, 1, "The email has been registered"));
                        continue;
                    }
                    String password = cell2.getStringCellValue();
                    if (!CommonUtils.passVerify(password)) {
                        errorList.add(setMessage(x + 1, 3, "Password too weak(Password too weak，Password too weak，Password too weak，Password too weak Password too weak)"));
                        continue;
                    }
                    //Set cell type toString
                    cell1.setCellType(CellType.STRING);
                    String name = cell1.getStringCellValue();
                    ConsumerDO consumerDO = new ConsumerDO();
                    consumerDO.setName(name);
                    consumerDO.setEmailAccounts(email);
                    consumerDO.setPassword(RSAEncrypt.encrypt(password));
                    consumerDO.setState(3);
                    consumerDO.setCreateTime(LocalDateTime.now());
                    consumerDO.setAddWay("File Import");
                    consumerDO.setRoles(new ArrayList<String>() {{
                        add(Constant.GENERAL);
                    }});
                    mongoTemplate.save(consumerDO, COLLECTION_NAME);
                }
            }
        } catch (Exception e) {
            log.error("context", e);
            return ResultUtils.error("Failed to parse file!");
        } finally {
            try {
                inputStream.close();
                work.close();
            } catch (Exception e) {
                log.error("context", e);
            }
        }
        if (errorList.size() > 0) {
            return ResultUtils.error("fail" + errorList.size() + "fail ,fail: {} " + errorList.toString());
        }
        return ResultUtils.success();
    }

    @Override
    public Result addUserList(String token, ManualAddList manualAddList) {
        Token user = jwtTokenUtils.getToken(token);
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }

        if (StringUtils.isBlank(manualAddList.getOrg())) {
            return ResultUtils.error("UNIT_IS_NULL");
        }
        if (StringUtils.isBlank(manualAddList.getRole())) {
            return ResultUtils.error("ROLE_IS_NULL");
        }

        List<ManualAddList.Person> personList = manualAddList.getPerson();


        List<String> errorList = new ArrayList<>();

        for (ManualAddList.Person person : personList) {

            //Email verification
            if (!CommonUtils.isEmail(person.getEmail())) {
                errorList.add(setMessage(person.getEmail() + "  " + I18nUtil.get("USER_EMAIL_FORMAT")));
                continue;
            }

            //Name verification
            if (StringUtils.isBlank(person.getName())) {
                errorList.add(setMessage(person.getName() + "  " + I18nUtil.get("NAME_IS_NULL")));
                continue;
            }

            //Verify if the email exists
            ConsumerDO userInfoByName = getUserInfoByName(person.getEmail());
            if (null != userInfoByName) {
                errorList.add(setMessage(person.getEmail() + "  " + I18nUtil.get("USER_EMAIL_EXIST")));
                continue;
            }
            ConsumerDO consumerDO = new ConsumerDO();

            consumerDO.setEmailAccounts(person.getEmail());
            consumerDO.setState(1);
            consumerDO.setCreateTime(LocalDateTime.now());
            consumerDO.setAddWay("Batch Add");
            consumerDO.setName(person.getName());
            consumerDO.setRoles(new ArrayList<String>() {{
                add(manualAddList.getRole());
            }});

            mongoTemplate.save(consumerDO, COLLECTION_NAME);

            //Send email
            long stringTime = new Date().getTime();
            String code = SMS4.Encryption("pwdBack&" + person.getEmail() + "&" + stringTime);
            Map<String, Object> param = new HashMap<>(16);
            param.put("name", user.getName());
            param.put("email", user.getEmailAccounts());
            param.put("url", instdbUrl.getCallHost() + instdbUrl.getSetUpPsd() + code);
            ToEmail toEmail = new ToEmail();
            toEmail.setTos(new String[]{consumerDO.getEmailAccounts()});
            asyncDeal.send(toEmail, param, EmailModel.EMAIL_INVITE());
        }
        if (errorList.size() > 0) {
            return ResultUtils.errorOld(I18nUtil.get("FAILED") + errorList.size() + " ," + I18nUtil.get("ERROR_PROMPT") + " {} " + errorList.toString());
        }
        return ResultUtils.success();
    }

    @Override
    public Result deleteUserById(String token, String id) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        Query query = new Query();
        Criteria criteria = Criteria.where("_id").is(id);
        query.addCriteria(criteria);
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class, COLLECTION_NAME);
        if (null == one) {
            return ResultUtils.error("DATA_QUERY_EXCEPTION");
        }
        mongoTemplate.remove(query, ConsumerDO.class);
        return ResultUtils.success("DELETE_SUCCESS");
    }

    @Override
    public List<Map> adminUserList() {
        List<Map> list = new ArrayList<>();
        Query query = new Query();
        query.addCriteria(Criteria.where("roles").in(Constant.ADMIN, Constant.ROLE_APPROVE));
        List<ConsumerDO> userList = mongoTemplate.find(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null != userList && userList.size() > 0) {
            for (ConsumerDO user : userList) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", user.getName());
                map.put("email", user.getEmailAccounts());
                map.put("roles", user.getRoles());
                list.add(map);
            }
        }
        return list;
    }

    private Map<String, Object> setMessage(int sheet, int row, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 500);
        map.put("message", "Section" + sheet + "Section,Section" + row + "Section " + message);
        return map;
    }

    private String setMessage(String message) {
        StringBuffer sb = new StringBuffer();
        sb.append(message + "\n");
        return sb.toString();
    }

    @Override
    public Result adminUserUpdate(String token, ManualAdd manualAdd) {
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (roles == null) {
            return ResultUtils.error("LOGIN_EXCEPTION");
        }
        if (!roles.contains(Constant.ADMIN)) {
            return ResultUtils.error("PERMISSION_FORBIDDEN");
        }
        if (manualAdd == null || org.apache.commons.lang3.StringUtils.isEmpty(manualAdd.getUserId())) {
            return ResultUtils.error("PARAMETER_ERROR");
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(manualAdd.getUserId()));
        Update update = new Update();
        String email = "";
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(manualAdd.getEmailAccounts())) {
            email = RSAEncrypt.decrypt(manualAdd.getEmailAccounts());
            //Email verification
            if (!CommonUtils.isEmail(email)) {
                return ResultUtils.error("EMAIL_INCORRECT");
            }

            update.set("emailAccounts", email);
        }
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(manualAdd.getName())) {
            String name = RSAEncrypt.decrypt(manualAdd.getName());
            update.set("name", name);
        }
        if (StringUtils.isNotBlank(manualAdd.getOrgChineseName())) {
            update.set("orgChineseName", manualAdd.getOrgChineseName());
        }
        if (manualAdd.getRoles() != null && manualAdd.getRoles().size() > 0) {
            update.set("roles", manualAdd.getRoles());
            //Force Cleartoken
            tokenCache.invalidate(email);
        }

        mongoTemplate.upsert(query, update, UserServiceImpl.COLLECTION_NAME);
        return ResultUtils.success("UPDATE_SUCCESS");
    }

}
