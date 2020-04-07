package com.senyer.senyer_opc;

import com.senyer.senyer_opc.datacenter.DataCenter;
import com.senyer.senyer_opc.opc.OpcAsyncHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import javax.annotation.Resource;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.senyer.senyer_opc.persistence")
public class OpcClientApplication implements ApplicationRunner  {

  public static void main(String[] args) {
    SpringApplication.run(OpcClientApplication.class, args);
  }

  @Resource
  private DataCenter dataCenter;

  @Resource
  private OpcAsyncHandler opcAsyncHandler;

//默认启动完毕执行一次
  @Override
  public void run(ApplicationArguments args) throws Exception {
    dataCenter.read();
    //opcAsyncHandler.asyncHandle();
  }
}

