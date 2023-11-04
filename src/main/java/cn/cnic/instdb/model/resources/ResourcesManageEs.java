//package cn.cnic.instdb.model.resources;
//
//import lombok.Data;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.elasticsearch.annotations.Document;
//import org.springframework.data.elasticsearch.annotations.Field;
//import org.springframework.data.elasticsearch.annotations.FieldType;
//
//import java.util.List;
//
//@Data
//@Document(indexName = "resources_manage", type = "ResourcesManageEs")
//public class ResourcesManageEs {
//
//    @Id
//    private String id;
//    private String resourcesId;
//
//    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
//    private String titleZh;
//
//    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
//    private String titleEn;
//    //Resource Type
//    private String resourceType;
//
//    //version
//    private String version;
//
//    private String doi;
//    private String cstr;
//
//    //Publishing agencyid
//    private String organizationId;
//
//    private String organizationName;
//
//    //Name of publisher
//    private String publisherName;
//
//    //Publisher's email
//    private String publisherEmail;
//
//    //Privacy Policy
//    private ResourcesManage.PrivacyPolicy privacyPolicy;
//    //license agreement
//    private String licenseAgreement;
//
//    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
//    private String descZh;
//
//    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
//    private String descEn;
//
//    private List<String> keywordZh;
//    private List<String> keywordEn;
//
//    //subject
//    private List<String> subject;
//
//
//    //Author's thesis project
//    private List<ResourcesManage.Other> other;
//
//    //  projectproject paperproject authorproject contributorproject
//    private List<ResourcesManage.Value> author;
//    private List<ResourcesManage.Value> paper;
//    private List<ResourcesManage.Value> project;
//    private List<ResourcesManage.Value> contributor;
//
//    private String createTime;
//    //Release time
//    private String releaseTime;
//
//    //Visits
//    private int visitNum;
//    //Number of downloads
//    private int downloadNum;
//    //Number of followers
//    private int followNum;
//
//
//}
