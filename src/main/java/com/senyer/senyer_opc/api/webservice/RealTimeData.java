package com.senyer.senyer_opc.api.webservice;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import java.io.IOException;

/**
 * 获取实时数据接口webservice
 */
@WebService(targetNamespace="http://webservice.senyer.com/")
public interface RealTimeData {


  /**
   * 获取所有实时数据
   */
  @WebMethod(action ="http://webservice.senyer.com/realtime" )
  String realtime(
          @WebParam(name = "tableName",
                  targetNamespace = "http://webservice.senyer.com/")
                  String tableName,
          @WebParam(name = "type",
                  targetNamespace = "http://webservice.senyer.com/")
                  String type) throws IOException;


}
