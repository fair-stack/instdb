package cn.cnic.instdb.ftp.authen;


import cn.cnic.instdb.filehandle.FtpCustomization;
import cn.cnic.instdb.ftp.minimalftp.FTPConnection;
import cn.cnic.instdb.ftp.minimalftp.api.IFileSystem;
import cn.cnic.instdb.ftp.minimalftp.api.IUserAuthenticator;
import cn.cnic.instdb.ftp.minimalftp.impl.NativeFileSystem;
import cn.cnic.instdb.model.resources.FtpUser;
import cn.cnic.instdb.utils.InstdbUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.InetAddress;

/**
 * A simple user base which encodes passwords in MD5 (not really for security, it's just as an example)
 * @author chl
 */
@Service
@Slf4j
public class UserbaseAuthenticator implements IUserAuthenticator {


    private final MongoTemplate mongoTemplate;

    private final InstdbUrl instdbUrl;

    private final FtpCustomization control;

    public UserbaseAuthenticator(MongoTemplate mongoTemplate, InstdbUrl instdbUrl, FtpCustomization control) {
        this.mongoTemplate = mongoTemplate;
        this.instdbUrl = instdbUrl;
        this.control = control;
    }

    @Override
    public boolean needsUsername(FTPConnection con) {
        return true;
    }

    @Override
    public boolean needsPassword(FTPConnection con, String username, InetAddress address) {
        return true;
    }


    @Override
    public IFileSystem authenticate(FTPConnection con, InetAddress address, String username, String password) throws IUserAuthenticator.AuthException {
        // Check for a user with that username in the database
//        log.info(">>>>>>>>>>>>>>> ftp service user is verifying <<<<<<<<<<<<<");

        Query query = new Query();
        query.addCriteria(Criteria.where("username").is(username));
        query.addCriteria(Criteria.where("password").is(password));
        FtpUser user = mongoTemplate.findOne(query, FtpUser.class);
        if (null == user) {
            log.info(">>>>>>>>>>>>>>> ftp service user authentication failed <<<<<<<<<<<<<");
            log.info(">>>>>>>>>>>>>>> ftp username:" + username + " password:" + password + "<<<<<<<<<<<<<");
            throw new AuthException();
        }
        con.setRole(user.getAuth());
        con.setRealUsers(user.getRealUsers());
        con.setControl(control);
        con.setResourceId(user.getResourcesId());
        File path = new File(user.getHomedirectory());
//        log.info(">>>>>>>>>>>>>>> ftp service user authentication success <<<<<<<<<<<<<");
        return new NativeFileSystem(path);
    }



}
