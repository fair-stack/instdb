package cn.cnic.instdb.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
*
* Created  by wdd on 2019/11/1
*
**/
@Configuration
@EnableSwagger2
@ConditionalOnProperty(name = "swagger.enable",havingValue = "true")
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        //http://ipaddress:address/address/swagger-ui.html#/
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("instdb") //Website Title
                .description("instdbswagger RESTful APIs......") //Website Description
                .version("1.0") //version
                .contact(new Contact("Wang Dongdong","https://blog.csdn.net/Xiaodongge521","wangdongdong0224@163.com")) //Wang Dongdong
                .license("The Apache License") //protocol
                .licenseUrl("http://www.baidu.com") //protocolurl
                .build();

        return new Docket(DocumentationType.SWAGGER_2) //swaggerversion
                .pathMapping("/")
                .select()
                //Scan thosecontroller
                .apis(RequestHandlerSelectors.basePackage("cn.cnic.instdb.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo);
    }

}

