package cn.cnic.instdb.ftp;


import cn.cnic.instdb.filehandle.FtpCustomization;
import cn.cnic.instdb.ftp.authen.CustomServer;
import cn.cnic.instdb.ftp.authen.UserbaseAuthenticator;
import cn.cnic.instdb.ftp.listener.SpaceFileListener;
import cn.cnic.instdb.ftp.minimalftp.FTPServer;
import cn.cnic.instdb.utils.InstdbUrl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(value = 1)
public class FtpStartupRunner implements CommandLineRunner {

    @Autowired
    private InstdbUrl instdbUrl;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private FtpCustomization control;

    @Override
    public void run(String... args) throws Exception {
        log.info(">>>>>>>>>>>>>>> ftp service is starting <<<<<<<<<<<<<");
        runnerFTP(mongoTemplate,instdbUrl,control);
        log.info(">>>>>>>>>>>>>>> ftp service is success <<<<<<<<<<<<<");
    }

    public static void runnerFTP(MongoTemplate mongoTemplate, InstdbUrl instdbUrl,FtpCustomization control) {
        new Thread(){
            @SneakyThrows
            @Override
            public void run() {
                FTPServer server = new FTPServer();

                UserbaseAuthenticator auth = new UserbaseAuthenticator(mongoTemplate,instdbUrl,control);
                server.setAuthenticator(auth);
                server.setSpaceListener(new SpaceFileListener());

                // Register an instance of this class as a listener
                server.addListener(new CustomServer());

                // Changes the timeout to 10 minutes  100 * 60 * 100
                server.setTimeout(Integer.valueOf(instdbUrl.getTimeOut())); // 10 minutes

                // Changes the buffer size  1024 * 5
                server.setBufferSize(Integer.valueOf(instdbUrl.getBufferSize())); // 5 kilobytes

                String accHost = instdbUrl.getAccHost();
                if (accHost.contains("https://")) {
                    accHost = accHost.replaceAll("https://", "");
                } else if (accHost.contains("http://")) {
                    accHost = accHost.replaceAll("http://", "");
                }

                server.setIP_HOST(accHost);

              //  InetAddress byName = InetAddress.getByName(instdbUrl.getAccHost());

                server.listenSync(Integer.valueOf(instdbUrl.getPort()));
            }
        }.start();;
    }
}
