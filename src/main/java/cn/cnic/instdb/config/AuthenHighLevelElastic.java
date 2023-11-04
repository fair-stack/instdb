//package cn.cnic.instdb.config;
//
//import com.mongodb.MongoClient;
//import com.mongodb.MongoCredential;
//import com.mongodb.ServerAddress;
//import com.mongodb.client.MongoDatabase;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.elasticsearch.client.Client;
//import org.elasticsearch.client.transport.TransportClient;
//import org.elasticsearch.common.settings.Settings;
//import org.elasticsearch.common.transport.TransportAddress;
//import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.FileNotFoundException;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.util.Arrays;
//
///**
// * @Auther: wdd
// * @Date: 2021/06/17/12:47
// * @Description:
// */
//@Configuration
//@Slf4j
//@Data
//@ConfigurationProperties(prefix = "elasticsearch")
//public class AuthenHighLevelElastic {
//
//    private String clusterName;
//
//    private String clusterNodes;
//
//    private String clusterPassword;
//
//    private String certPath;
//
//    private boolean sslEnabled;
//
//    @Value("${spring.data.mongodb.username}")
//    private String userName;
//
//    @Value("${spring.data.mongodb.password}")
//    private String password ;
//
//    @Value("${spring.data.mongodb.database}")
//    private String database;
//
//    @Value("${spring.data.mongodb.host}")
//    private String host;
//
//
//        /**
//     * elasticsearchClient injection（Client injection）
//     *
//     * @return
//     * @throws FileNotFoundException
//     */
//    @Bean
//    public Client client() {
//        try {
//            PreBuiltXPackTransportClient packTransportClient = new PreBuiltXPackTransportClient(settings());
//            String[] split = clusterNodes.split(",");
//            for (String s : split) {
//                String[] split1 = s.split(":");
//                int port = Integer.parseInt(split1[1]);
//                packTransportClient.addTransportAddress(new TransportAddress(InetAddress.getByName(split1[0]), port));
//            }
//            return packTransportClient;
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    /**
//     * elasticsearchClient injection（Client injection）
//     *
//     * @return
//     * @throws FileNotFoundException
//     */
//    @Bean
//    public TransportClient transportClient() {
//        try {
//            PreBuiltXPackTransportClient packTransportClient = new PreBuiltXPackTransportClient(settings());
//            String[] split = clusterNodes.split(",");
//            for (String s : split) {
//                String[] split1 = s.split(":");
//                int port = Integer.parseInt(split1[1]);
//                packTransportClient.addTransportAddress(new TransportAddress(InetAddress.getByName(split1[0]), port));
//            }
//            return packTransportClient;
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    @Bean(name = "mongoDatabase")
//    public MongoDatabase getMongoDatabase(){
//        MongoDatabase mongoDatabase;
//        try {
//            MongoCredential credential = MongoCredential.createCredential(userName, database, password.toCharArray());
//            MongoClient mongoClient = new MongoClient(new ServerAddress(host, 27017), Arrays.asList(credential));
//            // Connect to database
//            mongoDatabase = mongoClient.getDatabase(database);
//            log.info("initializationmongoDatabaseinitialization");
//            return mongoDatabase;
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("initializationmongoDatabaseinitialization");
//            return null;
//        }
//    }
//
//
//
//    private Settings settings() {
//        if (sslEnabled) {
//            Settings.Builder builder = Settings.builder();
//            builder.put("cluster.name", clusterName);
//            builder.put("xpack.security.user", clusterPassword);
//            builder.put("xpack.security.enabled", sslEnabled);
//            builder.put("xpack.security.transport.ssl.keystore.path", certPath);
//            builder.put("xpack.security.transport.ssl.keystore.password", "instdb");
//            //    builder.put("xpack.security.transport.ssl.truststore.path", certPath);
//            builder.put("xpack.security.transport.ssl.verification_mode", "certificate");
//            builder.put("xpack.security.transport.ssl.enabled", sslEnabled);
//            builder.put("client.transport.sniff", true);
//            builder.put("thread_pool.search.size", 10);
//            return builder.build();
//        } else {
//            Settings.Builder builder = Settings.builder();
//            return builder.build();
//        }
//    }
//
//
//
//}
