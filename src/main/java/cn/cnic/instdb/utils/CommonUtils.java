package cn.cnic.instdb.utils;


import cn.cnic.instdb.constant.Constant;
import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import sun.misc.BASE64Encoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.groups.Default;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * CommonUtils
 *
 * @author wangCc
 * @date 2018/11/2
 */

@Slf4j
public final class CommonUtils {

    private static final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    public static String FILE_SPLIT = "\\";
    public static final String STRONG = "strong";
    public static final String MIDDLE = "in";
    public static final String WEAK = "weak";

    /**
     * generate UUID
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * generate snow flake
     */
    public static String generateSnowflake() {
        return String.valueOf(IdUtil.getSnowflake(System.currentTimeMillis() % 32, 0L).nextId());
    }


    /**
     * picture type suffix
     */
    public static boolean isPic(String suffix) {
        return Arrays.asList("BMP", "JPG", "JPEG", "PNG", "GIF", "bmp", "jpg", "jpeg", "png", "gif").contains(suffix);
    }

    /**
     * matching“matching，matching，matching，matching”matching
     */
    public static boolean passVerify(String password) {

        String pattern = "^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_!@#$%^&*`~()-+=]+$)(?![a-z0-9]+$)(?![a-z\\W_!@#$%^&*`~()-+=]+$)(?![0-9\\W_!@#$%^&*`~()-+=]+$)[a-zA-Z0-9\\W_!@#$%^&*`~()-+=]{6,30}$";
        Pattern pA = Pattern.compile(pattern);
        Matcher matcher = pA.matcher(password);
        return matcher.matches();
    }

    /**
     * Verify if it is a Chinese character
     * @param password
     * @return
     */
    public static boolean chineseVerify(String password) {
        String pattern = "^[\u4e00-\u9fa5]{0,}$";
        Pattern pA = Pattern.compile(pattern);
        Matcher matcher = pA.matcher(password);
        return matcher.matches();
    }

    /**
     * urlcheck
     * @param url
     * @return
     */
    public static boolean urlVerify(String url) {
        String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" ;
        Pattern patt = Pattern. compile(regex );
        Matcher matcher = patt.matcher(url);
        return matcher.matches();
    }




    /**
     * RemovehtmlRemove
     * @param htmlStr
     * @return
     */
    public static String delHtmlTags(String htmlStr) {
        //definitionscriptdefinition，definitionjsdefinition
        String scriptRegex="<script[^>]*?>[\\s\\S]*?<\\/script>";
        //definitionstyledefinition，definitionstyledefinition，definitioncssdefinitioncssdefinition
        String styleRegex="<style[^>]*?>[\\s\\S]*?<\\/style>";
        //definitionHTMLdefinition，definition，definition
        String htmlRegex="<[^>]+>";
        //Define spaces,Define spaces,Define spaces,Define spaces
        String spaceRegex = "\\s*|\t|\r|\n";

        // filterscriptfilter
        htmlStr = htmlStr.replaceAll(scriptRegex, "");
        // filterstylefilter
        htmlStr = htmlStr.replaceAll(styleRegex, "");
        // filterhtmlfilter
        htmlStr = htmlStr.replaceAll(htmlRegex, "");
        // Filter spaces, etc
        htmlStr = htmlStr.replaceAll(spaceRegex, "");
        return htmlStr.trim(); // Returns a text string
    }

    /**
     * obtainHTMLobtain
     * @param htmlStr
     * @return
     */
    public static String getTextFromHtml(String htmlStr){
        //RemovehtmlRemove
        htmlStr = delHtmlTags(htmlStr);
        //Remove spaces" "
        htmlStr = htmlStr.replaceAll(" ","");
        return htmlStr;
    }


    public static String getCode(int n) {
        String string = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";//Save Numbers0-9 Save Numbers Save Numbers
        char[] ch = new char[n]; //Declare a character array objectch Declare a character array object Declare a character array object
        for (int i = 0; i < n; i++) {
            Random random = new Random();//Create a new random number generator
            int index = random.nextInt(string.length());//return[0,string.length)returnintreturn    return：return
            ch[i] = string.charAt(index);//charAt() : Returns the value at the specified index char Returns the value at the specified index   ==》Returns the value at the specified indexchReturns the value at the specified index
        }
        //takechartakeStringtakeresult
        //String result = new String(ch);//Method 1：Method 1      String(char[] value) ：Method 1 String，Method 1。
        String result = String.valueOf(ch);//Method 2： StringMethod 2   valueOf(char c) ：Method 2 char Method 2。
        return result;
    }

    /**
     * password strength
     *
     * @return Z = letter S = letter T = letter
     * one、one6-16，one：
     * weak：^[0-9A-Za-z]{6,16}$
     * in：^(?=.{6,16})[0-9A-Za-z]*[^0-9A-Za-z][0-9A-Za-z]*$
     * strong：^(?=.{6,16})([0-9A-Za-z]*[^0-9A-Za-z][0-9A-Za-z]*){2,}$
     * two、two6-16，twoASCIItwo：
     * weak：^[0-9A-Za-z]{6,16}$
     * in：^(?=.{6,16})[0-9A-Za-z]*[\x00-\x2f\x3A-\x40\x5B-\xFF][0-9A-Za-z]*$
     * strong：^(?=.{6,16})([0-9A-Za-z]*[\x00-\x2F\x3A-\x40\x5B-\xFF][0-9A-Za-z]*){2,}$
     */
    public static String checkPassword(String passwordStr) {
        String regexZ = "\\d*";
        String regexS = "[a-zA-Z]+";
        String regexT = "\\W+$";
        String regexZT = "\\D*";
        String regexST = "[\\d\\W]*";
        String regexZS = "\\w*";
        String regexZST = "[\\w\\W]*";

        if (passwordStr.matches(regexZ)) {
            return WEAK;
        }
        if (passwordStr.matches(regexS)) {
            return WEAK;
        }
        if (passwordStr.matches(regexT)) {
            return WEAK;
        }
        if (passwordStr.matches(regexZT)) {
            return MIDDLE;
        }
        if (passwordStr.matches(regexST)) {
            return MIDDLE;
        }
        if (passwordStr.matches(regexZS)) {
            return MIDDLE;
        }
        if (passwordStr.matches(regexZST)) {
            return STRONG;
        }
        return passwordStr;
    }

    public static boolean isEmail(String email) {
        if (null == email || "".equals(email)) {
            return false;
        }
        //String regEx1 = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        String regEx1 = "[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?";
        Pattern p = Pattern.compile(regEx1);
        Matcher m = p.matcher(email);
        if (m.matches()) {
            return true;
        } else {
            return false;
        }
    }



    public static boolean isPhone(String phone) {
        if (null == phone || "".equals(phone)) {
            return false;
        }
        String regEx1 = "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$";
        Pattern p = Pattern.compile(regEx1);
        Matcher m = p.matcher(phone);
        if (m.matches()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * License Agreement Matching
     * @param license
     * @return
     */
    public static String getLicense(String license) {
        if ("CC BY 4.0".equals(license)) {
            return "https://creativecommons.org/licenses/by/4.0/";
        } else if ("CC0".equals(license)) {
            return "https://creativecommons.org/publicdomain/zero/1.0/";
        } else if ("CC BY-SA 4.0".equals(license)) {
            return "https://creativecommons.org/licenses/by-sa/4.0/";
        } else if ("CC BY-NC 4.0".equals(license)) {
            return "https://creativecommons.org/licenses/by-nc/4.0/";
        } else if ("CC BY-NC-SA 4.0".equals(license)) {
            return "https://creativecommons.org/licenses/by-nc-sa/4.0/";
        } else if ("CC BY-ND 4.0".equals(license)) {
            return "https://creativecommons.org/licenses/by-nd/4.0/";
        } else if ("CC BY-NC-ND 4.0".equals(license)) {
            return "https://creativecommons.org/licenses/by-nc-nd/4.0/";
        } else if ("ODbL".equals(license)) {
            return "https://opendatacommons.org/licenses/odbl/1-0/";
        } else if ("Apache".equals(license)) {
            return "https://www.apache.org/licenses/LICENSE-2.0";
        } else if ("MIT".equals(license)) {
            return "https://opensource.org/license/mit/";
        } else if ("BSD".equals(license)) {
            return "https://metadata.ftp-master.debian.org/changelogs//main/a/arpwatch/arpwatch_2.1a15-7_copyright";
        } else if ("GPL".equals(license)) {
            return "https://www.gnu.org/licenses/gpl-3.0-standalone.html";
        } else if ("MPL".equals(license)) {
            return "https://www.mozilla.org/en-US/MPL/2.0/";
        } else if ("EPL".equals(license)) {
            return "https://www.eclipse.org/legal/epl-2.0/";
        } else if ("LGPL".equals(license)) {
            return "https://www.gnu.org/licenses/lgpl-3.0-standalone.html";
        } else if ("MulanPSL v2".equals(license)) {
            return "http://license.coscl.org.cn/MulanPSL2";
        } else if ("GGLY".equals(license)) {
            return "https://creativecommons.org/share-your-work/public-domain/pdm/";
        }else {
            return "";
        }
    }



    //jsoncharacter string character string
    public static String  stringToJSON(String strJson) {
        int tabNum = 0;
        StringBuffer jsonFormat = new StringBuffer();
        int length = strJson.length();
        for (int i = 0; i < length; i++) {
            char c = strJson.charAt(i);
            if (c == '{') {
                tabNum++;
                jsonFormat.append(c + "\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
            } else if (c == '}') {
                tabNum--;
                jsonFormat.append("\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
                jsonFormat.append(c);
            } else if (c == ',') {
                jsonFormat.append(c + "\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
            } else {
                jsonFormat.append(c);
            }
        }
        return jsonFormat.toString();
    }
    public static String getSpaceOrTab(int tabNum) {
        StringBuffer sbTab = new StringBuffer();
        for (int i = 0; i < tabNum; i++) {
            if (true) {
                sbTab.append('\t');
            } else {
                sbTab.append("    ");
            }
        }
        return sbTab.toString();
    }



    /**
     * Obtain attribute values based on attribute names
     * */
    public static Object getFieldValueByName(String fieldName, Object o) {
        try {
            String firstLetter = fieldName.substring(0, 1).toUpperCase();
            String getter = "get" + firstLetter + fieldName.substring(1);
            Method method = o.getClass().getMethod(getter, new Class[] {});
            Object value = method.invoke(o, new Object[] {});
            return value;
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return null;
        }
    }

    /**
     * Compare twolistCompare two Compare two
     * @param list1
     * @param list2
     * @return
     */
    public static boolean eqList(List<String> list1, List<String> list2){
        boolean bl = true;
        if(list1.size() == list2.size()){
            for(int i=0; i<list1.size(); i++){
                if((list1.get(i)).equals(list2.get(i))){
                    continue;
                } else {
                    bl = false;
                    break;
                }
            }
        } else {
            bl = false;
        }
        return bl;
    }


    /**
     * Escape Regular Special Characters （$()*+.[]?\^{},|）
     *
     * @param keyword
     * @return
     */
    public static String escapeExprSpecialWord(String keyword) {
        String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|" };
        for (String key : fbsArr) {
            if (keyword.contains(key)) {
                keyword = keyword.replace(key, "\\" + key);
            }
        }
        return keyword;
    }


    /**
     * JSR 303 validate
     *
     * @param obj Verify Object
     * @return List<String>
     */
    public static List<String> validation(Object obj) {
        Set<ConstraintViolation<Object>> validateSet =
                Validation.buildDefaultValidatorFactory().getValidator().validate(obj, Default.class);

        if (!CollectionUtils.isEmpty(validateSet)) {
            return validateSet.stream().map(ConstraintViolation::getMessage).collect(toList());
        }
        return Lists.newArrayList();
    }




    /**
     * cookieobtain
     *
     * @param request
     *            ServletRequest
     * @return String
     */

    public static String getCookie(HttpServletRequest request, String param){
        Cookie[] cookies = request.getCookies();
        if(cookies!=null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(param)) {
                    String value = cookie.getValue();
                    return value;
                }
            }
        }
        return null;
    }

    /*
     * obtainCookie
     * */
    public static Cookie setCookie(String name, String value, int time, int version){
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(time);//-1：No expiration date；0：No expiration date
        cookie.setVersion(version);
        //cookieThe valid path is the website root directory
        cookie.setPath("/");
        //   cookie.setDomain(Constants.Url.WEB_PATH);
        //set upHttpOnlyset up，set upCookieset up
        cookie.isHttpOnly();
        return cookie;
    }

    /**
     * base64 Encrypted String
     * @author wangzhiliang
     * */
    public static String baseEncoding(String str){

        byte[] bytes = null;
        String result = "";
        try {
            bytes = str.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if( bytes != null){
            result = new BASE64Encoder().encode(bytes);
        }
        return result;

    }

    /**
     * Picture tobase64Picture to
     *
     * @param imgFile Image Path
     * @return
     */
    public static String imageToBase64Str(String imgFile) {
        InputStream inputStream = null;
        byte[] data = null;
        try {
            inputStream = new FileInputStream(imgFile);
            data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
        } catch (IOException e) {
            log.error("context",e);
        }finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }
        // encryption
        String encode = Base64Encoder.encode(data);
        return  "data:image/png;base64,"+encode;
    }

    public static void main(String[] args) {


        String s = imageToBase64Str("C:\\Users\\wangdongdong\\Desktop\\banner2shallow.png");
        System.out.println(s);
//        s = s.replace("data:image/png;base64,", "");
//        base64ToFile("C:\\Users\\Administrator\\Desktop\\",s,"Hahaha.jpg");
    }


    /**
     * urlturnbase64
     * @param url
     * @return
     */
    public static String urlToBase64(String url) {
        if (StringUtils.isNotBlank(url)) {
            try {
                byte[] bytes = IOUtils.toByteArray(new URL(url));
                return "data:image/png;base64,"+ Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }


    //BASE64Decode intoFileDecode into
    public static void base64ToFile(String destPath,String base64, String fileName) {
        base64 = base64.replace("data:image/png;base64,", "");
        base64 = base64.replace("data:image/jpg;base64,", "");
        base64 = base64.replace("data:image/jpeg;base64,", "");
        base64 = base64.replace("data:image/bmp;base64,", "");
        File file = null;
        //Create file directory
        File dir = new File(destPath);
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdirs();
        }
        BufferedOutputStream bos = null;
        java.io.FileOutputStream fos = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            file = new File(destPath + fileName+Constant.PNG);
            fos = new java.io.FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Custom return valuecodeCustom return valuemsg
     * @param response
     * @param param
     */
    public static void errorMsg(HttpServletResponse response,Map param) {

        OutputStream out = null;
        try {
          //  response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.setCharacterEncoding("utf-8");
          //  response.setContentType("text/json");
            out = response.getOutputStream();
            out.write(JSON.toJSONString(param).getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            log.error("context",e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("context",e);
                }
            }
        }
        return;
    }

    public static String getApiMsgByCode(int code) {
        switch (code) {
            case 501:
                return "Missing token parameter";
            case 502:
                return "Token expiration";
            case 503:
                return "Illegal token";
            case 504:
                return "Unsupported interface version";
            case 505:
                return "Unauthorized token";
            case 506:
                return "Expired authorized token";
            case 507:
                return "APIOffline";
            default:
                return "Internal server error";
        }
    }

    /**
     * listturn,turn
     *
     * @param list
     * @return
     */
    public static String listToStr(List<String> list,String sign) {
        String keywords = "";
        if (null != list && list.size() > 0) {
            for (String keyword : list) {
                keywords += keyword + sign;
            }
            keywords = keywords.substring(0, keywords.length() - 1);
        }
        return keywords;
    }

    /**
     * String tolist
     *
     * @param sign
     * @return
     */
    public static List<String> strToList(String sign,String str) {
        List<String> list = new ArrayList<>();
        if (StringUtils.isNotBlank(sign) && sign.contains(",")) {
            String[] split = sign.split(str);
            list = Arrays.asList(split);
        } else {
            list.add(sign);
        }
        return list;
    }

//    public static String getResourceType(String resourceType) {
//        for (String type : Constant.RESOURCE_TYPES) {
//            String[] split = type.split("&");
//            if (split[0].equals(resourceType)) {
//                String lang = StringUtils.isBlank(tokenCache.getIfPresent("lang")) ? Constant.Language.chinese : tokenCache.getIfPresent("lang");
//                if(Constant.Language.chinese.equals(lang)){
//                    return split[1];
//                }else {
//                    return split[2];
//                }
//
//            }
//        }
//        return resourceType;
//    }

    /**
     * Obtain Chinese and English constants
     * @param value
     * @param type
     * @return
     */
    public static String getValueByType(String value,String type) {
        String datas[] = null;
         if (Constant.LanguageStatus.PRIVACYPOLICY.equals(type)) {
            datas = Constant.PrivacyPolicy_TYPES;
        } else if (Constant.LanguageStatus.ROLE.equals(type)) {
            datas = Constant.ROLE_TYPES;
        }else if (Constant.LanguageStatus.RESOURCE_TYPES.equals(type)) {
             datas = Constant.RESOURCE_TYPES;
         }

        for (String data : datas) {
            String[] split = data.split("&");
            if (split[0].equals(value)) {
                String lang = StringUtils.isBlank(tokenCache.getIfPresent("lang")) ? Constant.Language.chinese : tokenCache.getIfPresent("lang");
                if(Constant.Language.chinese.equals(lang)){
                    return split[1];
                }else {
                    return split[2];
                }

            }
        }
        return value;
    }


    /**
     * special symbol check
     * true: include
     * false: not include
     */
    boolean specialSymbolCheck(String str) {
        return Constant.PATTERN.matcher(str).find();
    }


    /**
     * Specialized processing of resource types  Specialized processing of resource types0Specialized processing of resource types
     * @param map
     */
    public static void addResourceType(Map<String, String> map,String type) {
        String datas[] = null;
        if (Constant.LanguageStatus.STATUS.equals(type)) {
            datas = Constant.STATUS_TYPES;
        } else if (Constant.LanguageStatus.RESOURCE_TYPES.equals(type)) {
            datas = Constant.RESOURCE_TYPES;
        }
        Map<String, String> newMap = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            for (String resourceType :datas) {
                String[] split = resourceType.split("&");
                if (entry.getKey().contains("-") && split[0].equals(entry.getKey().split("-")[0])) {
                    String lang = StringUtils.isBlank(tokenCache.getIfPresent("lang")) ? Constant.Language.chinese : tokenCache.getIfPresent("lang");
                    if(Constant.Language.chinese.equals(lang)){
                        entry.setValue(split[1]);
                    }else {
                        entry.setValue(split[2]);
                    }
                }
            }
        }

        if (map.size() > 0) {
            for (String resourceType : datas) {
                String[] split = resourceType.split("&");
                Set keySet = map.keySet();  //obtainkeyobtain
                for(Object keyName:keySet){
                    if (!keyName.toString().contains(split[0]+"-")) {
                        String lang = StringUtils.isBlank(tokenCache.getIfPresent("lang")) ? Constant.Language.chinese : tokenCache.getIfPresent("lang");
                        if(Constant.Language.chinese.equals(lang)){
                            newMap.put(split[0] + "-" + 0, split[1]);
                        }else {
                            newMap.put(split[0] + "-" + 0, split[2]);
                        }
                    }
                }
            }
            Map<String, String> result = new HashMap<>();
            result.putAll(newMap);

            for (Map.Entry<String, String> entry : newMap.entrySet()) {
                for (Map.Entry<String, String> maps : map.entrySet()) {
                    if(entry.getValue().equals(maps.getValue())){
                        result.remove(entry.getKey());
                    }
                }
            }
            map.putAll(result);
        }
    }



    public static void setLangToReq(HttpServletRequest request) {
        String lang = request.getHeader("lang");
        if(org.apache.commons.lang3.StringUtils.isNotBlank(lang)){
            tokenCache.put("lang",lang);
        }else {
            tokenCache.put("lang",Constant.Language.chinese);
        }
    }

    /**
     * map sort
     * @param map
     * @return
     */
    public static Map<String, Integer> sortMap(Map<String, Integer> map) {
        //utilizeMaputilizeentrySetutilize，utilizelistutilize
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
        //utilizeCollectionsutilizesortutilizelistutilize
        Collections.sort(entryList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                //Positive arrangement，Positive arrangement
                return o2.getValue() - o1.getValue();
            }
        });
        //Traverse sortedlist，Traverse sortedLinkedHashMap，Traverse sortedLinkedHashMapTraverse sorted
        LinkedHashMap<String, Integer> linkedHashMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String,Integer> e : entryList
        ) {
            linkedHashMap.put(e.getKey(),e.getValue());
        }
        return linkedHashMap;
    }

}
