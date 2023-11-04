package cn.cnic.instdb.config;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.util.Arrays;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
@Data
public class ElasticsearchConfig {

    private String clusterName;
    private String clusterNodes;
    private int clusterPort;
    private String clusterPassword;

    @Value("${spring.data.mongodb.username}")
    private String userName;

    @Value("${spring.data.mongodb.password}")
    private String password ;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Value("${spring.data.mongodb.host}")
    private String host;



//    @Bean
//    public Client client() throws Exception {
//        return new PreBuiltXPackTransportClient(getSettings()).addTransportAddress(new TransportAddress(InetAddress.getByName(clusterNodes), clusterPort));
//    }


    @Bean
    public TransportClient getTransportClient(){
        TransportClient client = null;
        try {
            return new PreBuiltXPackTransportClient(getSettings()).addTransportAddress(new TransportAddress(InetAddress.getByName(clusterNodes), clusterPort));
        }catch (Exception e){
            log.error("context",e);
            log.info("initializationTransportClientinitialization");
        }
        return client;
    }

//    @Bean(name = "elasticsearchTemplate")
//    public ElasticsearchOperations elasticsearchTemplateCustom() throws Exception {
//        ElasticsearchTemplate elasticsearchTemplate;
//        try {
//            elasticsearchTemplate = new ElasticsearchTemplate(client());
//            log.info("initializationElasticsearchTemplateinitialization");
//            return elasticsearchTemplate;
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("initializationElasticsearchTemplateinitialization");
//            return new ElasticsearchTemplate(client());
//        }
//    }


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
//            log.error("context",e);
//            log.error("initializationmongoDatabaseinitialization");
//            return null;
//        }
//    }

    private Settings getSettings(){
        Settings esSettings = Settings.builder()
                .put("cluster.name", clusterName)
               //  .put("xpack.security.user", clusterPassword)
                .put("xpack.security.transport.ssl.enabled", false)
                //Add sniffing mechanism，Add sniffing mechanismESAdd sniffing mechanism,Add sniffing mechanismfalse
                .put("client.transport.sniff", false)
                //Increase the number of thread pools
                .put("thread_pool.search.size", 10)
                .build();
        return esSettings;
    }


}
