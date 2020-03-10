package com.senyer.senyer_opc.config;

import com.senyer.senyer_opc.api.webservice.RealTimeData;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.Resource;
import javax.xml.ws.Endpoint;

@Configuration
public class WebServiceConfig {
 
    @Resource
    private RealTimeData realTimeData;
    /**
     * 注入servlet  bean name不能dispatcherServlet 否则会覆盖dispatcherServlet
     */
    @Bean(name = "cxfServlet")
    public ServletRegistrationBean cxfServlet() {
        return new ServletRegistrationBean(new CXFServlet(),"/services/*");
    }

    @Bean(name = Bus.DEFAULT_BUS_ID)
    public SpringBus springBus() {
        return new SpringBus();
    }
 
    @Bean(name = "RealTimeData")
    public Endpoint realTimeEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(springBus(), realTimeData);
        endpoint.publish("/realtime");
        return endpoint;
    }
}
