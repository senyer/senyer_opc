package com.senyer.senyer_opc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Component
public class SwaggerConfig {
    private static final String WEB_VERSION = "1.0.0";
		private static final String SWAGGER_SCAN_WEB_PACKAGE = "com.senyer.opc_client.api.restful";

	@Bean
    public Docket webRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
        		//.globalOperationParameters(pars)
        		.groupName("Api接口文档（Restful版本）")
        		.apiInfo(apiWebInfo())
        		.useDefaultResponseMessages(false)
        		.enableUrlTemplating(false)
        		.forCodeGeneration(false)
        		.select()
        		.apis(RequestHandlerSelectors.basePackage(SWAGGER_SCAN_WEB_PACKAGE))//多个controller就用通配符
        		// 扫描所有有注解的api
	            //.apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
        		//.paths(PathSelectors.regex(".*/web/.*"))
        		
	            .build();
    }

    private ApiInfo apiWebInfo() {
        return new ApiInfoBuilder()
        		.title(">>>OPC客户端数据处理中心<<< 接口文档")
        		.description("Api接口文档（Restful版本）")
                .version(WEB_VERSION)
                .build();
    }

}
