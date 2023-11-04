package cn.cnic.instdb.utils;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.lionsoul.ip2region.Util;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wdd
 * @version 1.0
 * @date 2022/9/15 16:18
 */
@Slf4j
public class IPUtil {

    /**
     * Get current networkip
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                //Retrieve the local configuration based on the network cardIP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                ipAddress = inet.getHostAddress();
            }
        }
        //For situations where multiple agents are used，For situations where multiple agents are usedIPFor situations where multiple agents are usedIP,For situations where multiple agents are usedIPFor situations where multiple agents are used','For situations where multiple agents are used
        if (ipAddress != null && ipAddress.length() > 15) { //"***.***.***.***".length() = 15
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }

    /**
     * according toIPaccording to
     *
     * @param ip
     * @return
     */
    public static String getCityInfo(String ip) {
        File file = FileUtils.getResourceFile("/data/ip2region.db");
        if (!file.exists()) {
            log.error("Error: Invalid ip2region.db file, filePath：" + file.getPath());
            return null;
        }
        //Query algorithm
        int algorithm = DbSearcher.BTREE_ALGORITHM; //B-tree
        //DbSearcher.BINARY_ALGORITHM //Binary
        //DbSearcher.MEMORY_ALGORITYM //Memory
        try {
            DbConfig config = new DbConfig();
            DbSearcher searcher = new DbSearcher(config, file.getPath());
            Method method;
            switch (algorithm) {
                case DbSearcher.BTREE_ALGORITHM:
                    method = searcher.getClass().getMethod("btreeSearch", String.class);
                    break;
                case DbSearcher.BINARY_ALGORITHM:
                    method = searcher.getClass().getMethod("binarySearch", String.class);
                    break;
                case DbSearcher.MEMORY_ALGORITYM:
                    method = searcher.getClass().getMethod("memorySearch", String.class);
                    break;
                default:
                    return null;
            }
            DataBlock dataBlock;
            if (!Util.isIpAddress(ip)) {
                log.error("Error: Invalid ip address");
                return null;
            }
            dataBlock = (DataBlock) method.invoke(searcher, ip);
            if (file.exists()) {
                FileUtils.deleteFile(file.getPath());
            }
            return dataBlock.getRegion();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getIpPossession(String ip) {
        String cityInfo = getCityInfo(ip);
        if (!StringUtils.isEmpty(cityInfo)) {
            cityInfo = cityInfo.replace("|", " ");
            String[] cityList = cityInfo.split(" ");
            if (cityList.length > 0) {
                // Domestic display to specific provinces
                if ("China".equals(cityList[0])) {
                    if (cityList.length > 1) {
                        return cityList[2];
                    }
                }
                // Foreign Display to Country
                return cityList[0];
            }
        }
        return "unknown";
    }

    //obtainftpobtainip
    public static String getIpAddr(String host){
        if(null == host || host.trim().equals("")){
            return null;
        }
        boolean ip = isIP(host);
        if(!ip){
            InetAddress[] serverIP = getServerIP(host);
            if(null != serverIP && serverIP.length >0) {
                InetAddress inetAddress = serverIP[0];
                return inetAddress.getHostAddress();
            }else {
                return null;
            }
        }else {
            return host;
        }
    }

    /**
     * Obtaining a domain nameIPObtaining a domain name
     */
    private static InetAddress[] getServerIP(String domain) {
        InetAddress[] myServer = null;
        try {
            myServer = InetAddress.getAllByName(domain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return myServer;
    }


    private static boolean isIP(String addr){
        if(addr.length() < 7 || addr.length() > 15 || "".equals(addr)){
            return false;
        }
        /**
         * judgeIPjudge
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(addr);
        boolean ipAddress = mat.find();
        return ipAddress;
    }

    public static void main(String[] args) {
        System.out.println(getIpAddr("127.0.0.1"));

    }

}
