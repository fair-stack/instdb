package cn.cnic.instdb.ftp.listener;

import cn.cnic.instdb.ftp.minimalftp.api.SpaceListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SpaceFileListener implements SpaceListener {
    @Override
    public String saveSpaceId(String username,String spaceId) {
        String space = "error";
        if(spaceId.equals("/")){
            return space;
        }
//        Map<String, String> authoritiesCache = CaffeineUtil.getAuthoritiesCache(username);
//        String[] split = spaceId.split("/");
//        for (String s : split) {
//            if(!s.equals("")){
//                if(authoritiesCache.containsKey(s)){
//                   space = "/"+authoritiesCache.get(s)+"/"+s;
//                  // CaffeineUtil.setSpace(username,path);
//                   break;
//                }
//            }
//        }
        return space;
    }

    @Override
    public String auth(String username, String path) {
       // String space = CaffeineUtil.getSPACE(username);
        //return path+space;
         return "";
    }

    @Override
    public void renameFile(String username, String sourceFileName, String targetFileName) {

    }

    @Override
    public void mkdirFile(String username, String fileName) {

    }

    @Override
    public void deleteFile(String username, String fileName, String type) {

    }

    @Override
    public void uploadFile(String username, String fileName) {

    }

    @Override
    public void downloadFile(String username, String fileName) {

    }
}
