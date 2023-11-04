package cn.cnic.instdb.constant;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Constant {

    public static final String RESOURCE_TYPES[] = {"11&data set&Datasets", "13&data set&Reports", "14&data set&Papers", "15&data set&Treatises", "16&data set&Patents", "17&data set&Standards", "19&data set&Software","43&data set&Softness","99&data set&Others"};
    public static final String STATUS_TYPES[] = {"wait&Pending approval&pending approval", "adopt&Pending approval&Passed", "no&Pending approval&Rejected","offline&Pending approval&Voided"};
    public static final String PrivacyPolicy_TYPES[] = {"notOpen&Not open&Closed", "open&Not open&Open", "protect&Not open&Embargoed", "condition&Not open&Restricted"};
    public static final String ROLE_TYPES[] = {"role_admin&administrators&Admin", "role_approve&administrators&Auditor", "role_general&administrators&General User"};
    public static final String CHINESE_PROVINCES[] = {"Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing","Beijing"};
    public static final String CHINESE_PROVINCES_EN[] = {"Beijing", "Tianjin", "Shanghai", "Chongqing", "Hebei", "Henan", "Yunnan", "Liaoning", "Heilongjiang", "Hunan", "Anhui", "Shandong", "Xinjiang", "Jiangsu", "Zhejiang", "Jiangxi", "Hubei", "Guangxi", "Gansu", "Shanxi", "Inner Mongolia", "Shaanxi", "Jilin", "Fujian", "Guizhou", "Guangdong", "Qinghai", "Tibet", "Sichuan", "Ningxia", "Hainan", "Taiwan", "Hong Kong", "Macao"};
    public static final String ABROAD[] = {"Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola(Angola)","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola","Angola"};
    public static final String ABROAD_EN[] = {"Angola","Afghanistan","Albania","Algeria","Andorra","Anguilla","Antigua and Barbuda","Argentina","Armenia","Ascension","Australia","Austria","Azerbaijan","Bahamas","Bahrain","Bangladesh","Barbados","Belarus","Belgium","Belize","Benin","Bermuda Is.","Bolivia","Botswana","Brazil","Brunei","Bulgaria","Burkina-faso","Burma","Burundi","Cameroon","Canada","Cayman Is.","Central African Republic","Chad","Chile","China","Colombia","Congo","Cook Is.","Costa Rica","Cuba","Cyprus","Czech Republic","Denmark","Djibouti","Dominica Rep.","Ecuador","Egypt","EI Salvador","Estonia","Ethiopia","Fiji","Finland","France","French Guiana","Gabon","Gambia","Georgia","Germany","Ghana","Gibraltar","Greece","Grenada","Guam","Guatemala","Guinea","Guyana","Haiti","Honduras","Hongkong","Hungary","Iceland","India","Indonesia","Iran","Iraq","Ireland","Israel","Italy","Ivory Coast","Jamaica","Japan","Jordan","Kampuchea (Cambodia )","Kazakstan","Kenya","Korea","Kuwait","Kyrgyzstan","Laos","Latvia","Lebanon","Lesotho","Liberia","Libya","Liechtenstein","Lithuania","Luxembourg","Macao","Madagascar","Malawi","Malaysia","Maldives","Mali","Malta","Mariana Is","Martinique","Mauritius","Mexico","Moldova, Republic of","Monaco","Mongolia","Montserrat Is","Morocco","Mozambique","Namibia","Nauru","Nepal","Netheriands Antilles","Netherlands","New Zealand","Nicaragua","Niger","Nigeria","North Korea","Norway","Oman","Pakistan","Panama","Papua New Cuinea","Paraguay","Peru","Philippines","Poland","French Polynesia","Portugal","Puerto Rico","Qatar","Reunion","Romania","Russia","Saint Lueia","Saint Vincent","Samoa Eastern","Samoa Western","San Marino","Sao Tome and Principe","Saudi Arabia","Senegal","Seychelles","Sierra Leone","Singapore","Slovakia","Slovenia","Solomon Is","Somali","South Africa","Spain","Sri Lanka","St.Lucia","St.Vincent","Sudan","Suriname","Swaziland","Sweden","Switzerland","Syria","Taiwan","Tajikstan","Tanzania","Thailand","Togo","Tonga","Trinidad and Tobago","Tunisia","Turkey","Turkmenistan","Uganda","Ukraine","United Arab Emirates","United Kiongdom","United States of America","Uruguay","Uzbekistan","Venezuela","Vietnam","Yemen","Yugoslavia","Zimbabwe","Zaire","Zambia","Mauritania","Palestine","Serbia","Croatia"};

    public static final String DEFAULT_CODE_KEY = "random_code_";
    public static final String REVIEW = "_review";


    public static final class LanguageStatus {
        public static final String PRIVACYPOLICY = "privacyPolicy";
        public static final String ROLE = "role";
        public static final String STATUS = "status";
        public static final String RESOURCE_TYPES = "resource";
    }

    public static final class ScidbUrl {
        public static final String METRICS = "";
        public static final String HARVEST = "";
        public static final String FTPBYDOI = "";
    }

    public static List<String> addInterceptors(){
        List<String> list = new ArrayList<>();
        list.add("/approve/**");
        list.add("/commentNotice/**");
        list.add("/resources/**");
        list.add("/special/**");
        list.add("/user/**");
        list.add("/subject/**");
        list.add("/setting/**");
        list.add("/fair/**");
        list.add("/es/**");
        list.add("/review/**");
        list.add("/mysetting/**");
        list.add("/system/**");
        list.add("/open/**");
        list.add("/access/**");
        list.add("/community/**");
        list.add("/constant/**");
        list.add("/apply/access/**");
        list.add("/search/config/**");
        list.add("/fairman/**");
//        list.add("/doc.html");
//        list.add("/swagger-ui.html");
        return list;
    }




    /**
     * Path that does not require login
     * @return
     */
    public static List<String> getExcludePathPatternsList(){
        List<String> list = new ArrayList<>();
        list.add("/resources/getResourcesDetails");//Resource Details
        list.add("/commentNotice/findAllComment");//Comment List
        list.add("/special/findAllSpecial");//Topic List
        list.add("/special/getSpecialById");//Topic Details
        list.add("/special/getResourcesBySpecialId");//Resources under the topic
        list.add("/resources/getSpecialByResourcesId");//Special topics under resources
        list.add("/mysetting/check");//Password verification strength
        list.add("/mysetting/verification");//Password verification strength
        list.add("/mysetting/pwdEmail");//Retrieve password
        list.add("/mysetting/updatePwd");//Password Reset 
        list.add("/setting/downloadDataTemplate"); //File Download
        list.add("/resources/structuredDownloadFile"); //Structured Download
        list.add("/resources/resourcesDownloadFile"); //Download resource files
        list.add("/resources/resourcesFtpDownloadFile");//obtainftpobtain);
        list.add("/resources/getCitationDetail");//Reference citation detailed information query
        list.add("/resources/getStatisticsResourcesDay");//Dataset Column Chart);
        list.add("/resources/getStatisticsResourcesMonth");//Dataset Column Chart);
        list.add("/resources/getResourcesMap");//Dataset Map Display);
        list.add("/api/getDataTemplate"); //Provide external metadata template interface
        list.add("/api/getlicenseAgreement"); //Provide external license agreements
        list.add("/resources/resourcesDownloadJsonLd");//resourcejson-ldresource);
        list.add("/setting/set/host/port"); //Automatic mappingipAutomatic mapping
        list.add("/version/push"); //Version notification update
        list.add("/review/resourcesReview"); //expert review 
        list.add("/system/getBasicConfig"); //Global Configuration Query
        list.add("/system/getAboutConfig"); //About configuring queries
        list.add("/system/getIndexConfig"); //Homepage Configuration Query
        list.add("/resources/getResourceRecommend"); //Dataset recommendation
        list.add("/resources/getResourceFileTree"); //Data File Query
        list.add("/resources/exportResourceData"); //Data resource export
        list.add("/resources/getResourceGroupList"); //Data File Query
        list.add("/review"); //Data File Query
        list.add("/resources/getStructured"); //Structured Data Query
        list.add("/resources/getStructuredData");//Structured Data Content Query
        list.add("/resources/getJsonld");//jsonldStructured return  Structured return
        list.add("/constant/getDataByType");//Dictionary Query
        list.add("/access/get.data");//Exchange directory data query
        list.add("/approve/downloadRejectFile");//Approval rejection attachment download
        list.add("/approve/exportApprovalData");//Export approval records
        list.add("/apply/access/downloadAccessTemplate");//Data resource application access attachment download
        list.add("/open/downloadApiFile");//Open interface attachment download
        list.add("/fairman/previewData");//Component Preview
        list.add("/fairman/getComponent");//Component Query
        return list;
    }


    public static final class Api {
        //Paths that can be accessed without logging in(Paths that can be accessed without logging in:Paths that can be accessed without logging in)
        public static final String[] includeUrls = new String[]{
                "/fair/dataset/publish", "/fair/dataset/cancel", "/fair/uploadCompleted",
                "/fair/dataset/list", "/fair/dataset/details", "/fair/dataset/info", "/fair/dataset/detailsV1",
                "/fair/entry", "/fair/getTemplates",
                //open api
                "/open/approveSubmit", "/open/dataset/details", "/open/findAllApproval", "/open/claim", "/open/getToken",
                "/open/getResourceFileTree", "/open/resourcesDownloadFile", "/open/resourcesFtpDownloadFile", "/open/dataset/details/relatedDataset",
                "/open/setFilestorageInfo"};
        //"/open/dataset/details1",
        public static final String version = "1.0";
    }

    public static final Pattern PATTERN = Pattern.compile("[`~!@#$%^&*()+=|{}':;\\[\\]<>/?！￥…【】‘；：”“’。，、？]");

    public static boolean useList(String[] arr, String targetValue) {
        return Arrays.asList(arr).contains(targetValue);
    }

    public static final String LOGO_BASE64 = "data:image/png;base64,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";

    public static final String HTTP = "http://";
    public static final String PNG = ".png";
    public static final String CASDC = "casdc";
    public static final String DATACITE = "datacite";
    public static final String CHINA_DOI = "chinadoi";


    public static final String CASDC_EMAIL = "";
    public static final String CASDC_EMAIL_PASSWORD = "";
    public static final String USERNAME = "";
    public static final String PASSWORD = "";
    //Manual
    public static final String MANUAL = "manual";
    //batch
    public static final String BATCH = "batch";



    /**
     * Data Resource Table Name  monggoData Resource Table Name esData Resource Table Name
     */
    public static final String RESOURCE_COLLECTION_NAME = "resources_manage";
    //Structured Table Name
    public static final String TABLE_NAME = "structured";
    //Data resource release time(es)
    public static final String RESOURCE_CREATE_TIME = "createTime";
    //long-term
    public static final String LONG_TERM = "long-term";
    //short-term
    public static final String SHORT_TERM = "short-term";
    //statistics
    public static final String STATISTICS = "statistics";
    //retrieval
    public static final String SEARCH = "search";

    /**
     *   Markup for the latest version of resources Markup for the latest version of resourcestrue
     */
    public static final String VERSION_FLAG = "true";


    /**
     * Application identification  Application identificationcstr doiApplication identification
     */
    public static final String APPLY = "apply";

    //Perform metadata analysisCSTRPerform metadata analysisurl
    public static final String REGISTER_CSTR = "http://127.0.0.1:8082/api/v1/registerCSTR";

    public static final class Language {
        public static final String chinese = "zh_CN";
        public static final String english = "en_US";
    }


    public static final class PrivacyPolicy {
        //Privacy Policy ： Privacy Policy：notOpen ,Privacy Policy：open ,Privacy Policy：protect Privacy Policy：condition
        public static final String NOTOPEN = "notOpen";
        public static final String OPEN = "open";
        public static final String PROTECT = "protect";
        public static final String CONDITION = "condition";
    }

    /**
     * Return status code
     *
     * @date 2019-10-8 11:06:57
     */
    public static final class StatusCode {
        public static final int SUCCESS = 200;
        public static final int NO_LOGIN = 401;
        public static final int FORBIDDEN = 403;
        public static final int ERROR = 500;
    }

    /**
     * Return status prompt
     *
     * @date 2019-10-8 11:06:57
     */
    public static final class StatusMsg {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILED = "FAILED";
    }


    /**
     * Several statuses of approval
     */
    public static final class Approval {
        public static final String PENDING_APPROVAL = "wait";
        public static final String ADOPT = "adopt";
        public static final String NO = "no";
        public static final String REVOKE = "revoke";
        public static final String YES = "yes";
        //Offline status
        public static final String OFFLINE = "offline";
    }

    /**
     * My message type
     */
    public static final class Comment {
        public static final String RESOURCE_PUBLISHING = "Resource Publishing";
        public static final String APPROVAL_REMINDER = "Approval Reminder";
        public static final String ACCESS_APPROVAL_REMINDER = "Apply for access";
        public static final String APPROVAL_ADOPT = "Approved";
        public static final String APPROVAL_NO = "Approval rejection";
        public static final String APPROVAL_REVOKE = "Approval revocation";
        public static final String VERSION = "Version upgrade";
    }

    //role
    public static final String ADMIN = "role_admin";   //administrators
    public static final String ROLE_APPROVE = "role_approve";   //Approval Role
    public static final String GENERAL = "role_general";   //General users


    public static final class LoginWay{

        public static final String UMP = "ump";  //Technology Cloud Login

        public static final String SYS = "sys";  //System login

        public static final String CAS = "cas";  //casLogin

        public static final String WECHAT = "wechat";   //WeChat login

        public static final String ESCIENCE = "escience";  //Shared Network

    }



    /**
     * Highlight Field Color
     *
     * @date 2019-11-29 14:05
     */
    public static final class TitleColor {

        //red
        public static final String COLOR_RED = "<span style=\"color:red;font-weight:bold;\">";
        // green
        public static final String COLOR_Green = "<span style=\"color:green;font-weight:bold;font-size:23px;\">";
        //End Label
        public static final String POST_TAGS="</span>";

    }

    /**
     * Parameter Prefix Type
     */
    public static final class QueryType {

        //Accurate matching
        public static final String TQ = "tq_";
        // Match a field with a participle
        public static final String MPQ = "mpq_";
        // Fuzzy matching
        public static final String LK = "lk_";
        // Full text search
        public static final String MMQ = "mmq_";
        // Accurate matching of multiple
        public static final String TSQ = "tsq_";
        // Condition matching Condition matching
        public static final String RQG = "rqg_";
        // Condition matching Condition matching
        public static final String RQL = "rql_";

    }

    /**
     * sort
     */
    public static final class Order {
        public static final String DIRECTION = "direction";
        public static final String SORT_FIELD = "sortField";
        public static final String DESC = "desc";
        public static final String ASC = "asc";
    }
}
